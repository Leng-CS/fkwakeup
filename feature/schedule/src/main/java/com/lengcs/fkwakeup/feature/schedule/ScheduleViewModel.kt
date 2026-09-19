package com.lengcs.fkwakeup.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.common.CurrentWeekCalculator
import com.lengcs.fkwakeup.core.common.ScheduleBlock
import com.lengcs.fkwakeup.core.common.ScheduleLayout
import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.common.TermPhase
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class ScheduleUiState(
    val term: Term? = null,
    val sections: List<SectionTemplate> = emptyList(),
    val blocks: List<ScheduleBlock> = emptyList(),
    /** 当前实际周次（今天） */
    val currentWeek: Int = 1,
    /** 正在查看的周次 */
    val displayWeek: Int = 1,
    /** 今天星期几（1-7），用于高亮今日列 */
    val todayDayOfWeek: Int? = null,
    /** 当前处于第几节，课间为 null */
    val currentSection: Int? = null,
    val isEmpty: Boolean = true,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val currentTermProvider: CurrentTermProvider,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val weekOffset = MutableStateFlow(0)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            // 当前学期本身是流：切学期后会自动重新订阅该学期的课程与节次表
            combine(
                currentTermProvider.observeCurrentTerm(),
                weekOffset,
            ) { term, offset -> term to offset }
                .flatMapLatest { (term, offset) ->
                    if (term == null) {
                        flowOf(ScheduleUiState())
                    } else {
                        val today = LocalDate.now()
                        combine(
                            courseRepository.observeCourses(term.id),
                            termRepository.observeSections(term.id),
                        ) { courses, sections ->
                            buildState(term, courses, sections, today, offset)
                        }
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    private fun buildState(
        term: Term,
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        today: LocalDate,
        offset: Int,
    ): ScheduleUiState {
        val status = CurrentWeekCalculator.status(term, today)
        val rawWeek = status.week + offset
        val displayWeek = rawWeek.coerceIn(1, term.totalWeeks)

        // 传入节次时间表，让每个课程块带上具体起止时间（周视图里要显示开始时间）
        val blocks = ScheduleLayout.build(courses, displayWeek, term.totalWeeks, sections)

        // 只在「正在看本周」时高亮今日列与当前节次
        val isThisWeek = displayWeek == status.week && status.phase == TermPhase.IN_TERM
        val now = LocalTime.now()
        val currentSection = if (isThisWeek) {
            ScheduleLayout.currentSectionIndex(sections, now.hour * 60 + now.minute)
        } else {
            null
        }

        return ScheduleUiState(
            term = term,
            sections = sections,
            blocks = blocks,
            currentWeek = status.week,
            displayWeek = displayWeek,
            todayDayOfWeek = if (isThisWeek) today.dayOfWeek.value else null,
            currentSection = currentSection,
            isEmpty = courses.isEmpty(),
        )
    }

    /**
     * 保存从周视图抽屉里编辑的内容。
     *
     * 会同时更新课程（名称/教师/颜色）与本时间段（周几/节次/周次/地点）。
     * 校验不通过时只提示、不改数据。
     */
    fun saveBlockEdits(
        block: ScheduleBlock,
        name: String,
        teacher: String,
        colorArgb: Int?,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        weekSpec: String,
        location: String?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                _messages.trySend("课程名称不能为空")
                return@launch
            }

            val totalWeeks = _uiState.value.term?.totalWeeks ?: 18
            val trimmedSpec = weekSpec.trim()
            if (WeekSpecParser.parseOrNull(trimmedSpec, totalWeeks) == null) {
                _messages.trySend("周次「$trimmedSpec」无法识别")
                return@launch
            }

            // 起止节次写反了就自动纠正，比报错更省事
            val from = minOf(startSection, endSection).coerceAtLeast(1)
            val to = maxOf(startSection, endSection).coerceAtLeast(from)

            courseRepository.updateCourse(
                block.course.copy(
                    name = trimmedName,
                    teacher = teacher.trim().ifBlank { null },
                    colorArgb = colorArgb,
                ),
            )
            courseRepository.updateSession(
                block.session.copy(
                    dayOfWeek = dayOfWeek.coerceIn(1, 7),
                    startSection = from,
                    endSection = to,
                    weekSpec = trimmedSpec,
                    location = location?.trim()?.ifBlank { null },
                ),
            )

            WidgetRefreshScheduler.refreshNow(appContext)
            _messages.trySend("已保存")
            onDone()
        }
    }

    /** 删除这一节课（只是删时间段，不是删整门课） */
    fun deleteBlock(block: ScheduleBlock, onDone: () -> Unit) {
        viewModelScope.launch {
            courseRepository.deleteSession(block.session)
            WidgetRefreshScheduler.refreshNow(appContext)
            _messages.trySend("已删除该时间段")
            onDone()
        }
    }

    fun previousWeek() {
        weekOffset.value -= 1
    }

    fun nextWeek() {
        weekOffset.value += 1
    }

    fun backToCurrentWeek() {
        weekOffset.value = 0
    }
}
