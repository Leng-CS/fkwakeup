package com.lengcs.fkwakeup.feature.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.ReminderRepository
import com.lengcs.fkwakeup.core.model.CourseReminderRule
import com.lengcs.fkwakeup.core.model.RecurringReminderMode
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceKind
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceOverride
import com.lengcs.fkwakeup.core.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ReminderScope { OFF, ALL_FUTURE, CUSTOM }

data class ReminderChoice(
    val key: String,
    val kind: ReminderOccurrenceKind,
    val sourceId: Long,
    val weekNumber: Int?,
    val date: LocalDate,
    val label: String,
)

data class CourseReminderUiState(
    val loading: Boolean = true,
    val courseName: String = "",
    val scope: ReminderScope = ReminderScope.OFF,
    val primaryMinutes: Int = 10,
    val secondaryMinutes: Int? = null,
    val asyncOpen: Boolean = false,
    val asyncOpenMinutes: Int = 9 * 60,
    val asyncDeadline: Boolean = false,
    val asyncDeadlineDays: Int = 1,
    val asyncDeadlineMinutes: Int = 20 * 60,
    val choices: List<ReminderChoice> = emptyList(),
    val selectedKeys: Set<String> = emptySet(),
    val saved: Boolean = false,
)

internal data class ReminderSavePlan(
    val rule: CourseReminderRule,
    val overrides: List<ReminderOccurrenceOverride>,
)

internal fun buildReminderSavePlan(courseId: Long, value: CourseReminderUiState): ReminderSavePlan {
    val allFuture = value.scope == ReminderScope.ALL_FUTURE
    val rule = CourseReminderRule(
        courseId = courseId,
        recurringMode = if (allFuture) RecurringReminderMode.ALL_FUTURE else RecurringReminderMode.NONE,
        primaryMinutesBefore = value.primaryMinutes,
        secondaryMinutesBefore = value.secondaryMinutes,
        asyncOpenEnabled = value.asyncOpen,
        asyncOpenMinutesOfDay = value.asyncOpenMinutes,
        asyncDeadlineEnabled = value.asyncDeadline,
        asyncDeadlineDaysBefore = value.asyncDeadlineDays,
        asyncDeadlineMinutesOfDay = value.asyncDeadlineMinutes,
    )
    val overrides = value.choices.mapNotNull { choice ->
        val selected = choice.key in value.selectedKeys && value.scope != ReminderScope.OFF
        if (selected == allFuture) null else ReminderOccurrenceOverride(
            choice.key, courseId, choice.kind, choice.sourceId, choice.weekNumber, selected,
        )
    }
    return ReminderSavePlan(rule, overrides)
}

@HiltViewModel
class CourseReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val currentTermProvider: CurrentTermProvider,
    private val reminderRepository: ReminderRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val courseId = savedStateHandle.get<String>("courseId")?.toLongOrNull() ?: -1L
    private val requestedKey = savedStateHandle.get<String>("occurrenceKey")
    private val _state = MutableStateFlow(CourseReminderUiState())
    val state: StateFlow<CourseReminderUiState> = _state
    private var existingOverrides: List<ReminderOccurrenceOverride> = emptyList()

    init { load() }

    private fun load() = viewModelScope.launch {
        val term = currentTermProvider.currentTerm() ?: return@launch finishLoading()
        val entry = courseRepository.getCourseWithSessions(courseId) ?: return@launch finishLoading()
        val rule = reminderRepository.observeRule(courseId).first() ?: CourseReminderRule(courseId)
        existingOverrides = reminderRepository.observeOverrides(courseId).first()
        val today = LocalDate.now()
        val choices = buildList {
            entry.sessions.forEach { session ->
                WeekSpecParser.parseOrNull(session.weekSpec, term.totalWeeks).orEmpty().forEach { week ->
                    val date = term.startMonday.plusWeeks((week - 1).toLong()).plusDays((session.dayOfWeek - 1).toLong())
                    if (!date.isBefore(today)) add(
                        ReminderChoice(
                            ReminderOccurrenceOverride.sessionKey(session.id, week),
                            ReminderOccurrenceKind.SESSION,
                            session.id,
                            week,
                            date,
                            "第${week}周 · ${date.monthValue}月${date.dayOfMonth}日 · ${session.startSection}–${session.endSection}节",
                        ),
                    )
                }
            }
        }.sortedBy { it.date }
        val explicit = existingOverrides.filter { it.enabled }.mapTo(mutableSetOf()) { it.occurrenceKey }
        val scope = when {
            requestedKey != null -> ReminderScope.CUSTOM
            rule.recurringMode == RecurringReminderMode.ALL_FUTURE -> ReminderScope.ALL_FUTURE
            explicit.isNotEmpty() -> ReminderScope.CUSTOM
            else -> ReminderScope.OFF
        }
        val selected = when (scope) {
            ReminderScope.ALL_FUTURE -> choices.mapTo(mutableSetOf()) { it.key }.apply {
                removeAll(existingOverrides.filter { !it.enabled }.map { it.occurrenceKey }.toSet())
            }
            ReminderScope.CUSTOM -> explicit.apply { requestedKey?.let(::add) }
            ReminderScope.OFF -> emptySet()
        }
        _state.value = CourseReminderUiState(
            loading = false,
            courseName = entry.course.name,
            scope = scope,
            primaryMinutes = rule.primaryMinutesBefore,
            secondaryMinutes = rule.secondaryMinutesBefore,
            asyncOpen = rule.asyncOpenEnabled,
            asyncOpenMinutes = rule.asyncOpenMinutesOfDay,
            asyncDeadline = rule.asyncDeadlineEnabled,
            asyncDeadlineDays = rule.asyncDeadlineDaysBefore,
            asyncDeadlineMinutes = rule.asyncDeadlineMinutesOfDay,
            choices = choices,
            selectedKeys = selected,
        )
    }

    private fun finishLoading() { _state.value = _state.value.copy(loading = false) }
    fun setScope(value: ReminderScope) = update { it.copy(scope = value, selectedKeys = if (value == ReminderScope.ALL_FUTURE) it.choices.mapTo(mutableSetOf()) { c -> c.key } else it.selectedKeys) }
    fun setPrimary(value: Int) = update { it.copy(primaryMinutes = value) }
    fun setSecondary(value: Int?) = update { it.copy(secondaryMinutes = value) }
    fun setAsyncOpen(value: Boolean) = update { it.copy(asyncOpen = value) }
    fun setAsyncOpenMinutes(value: Int) = update { it.copy(asyncOpenMinutes = value) }
    fun setAsyncDeadline(value: Boolean) = update { it.copy(asyncDeadline = value) }
    fun setAsyncDeadlineDays(value: Int) = update { it.copy(asyncDeadlineDays = value) }
    fun setAsyncDeadlineMinutes(value: Int) = update { it.copy(asyncDeadlineMinutes = value) }
    fun toggle(key: String) = update { state -> state.copy(selectedKeys = state.selectedKeys.toMutableSet().apply { if (!add(key)) remove(key) }) }
    private fun update(block: (CourseReminderUiState) -> CourseReminderUiState) { _state.value = block(_state.value).copy(saved = false) }

    fun save() = viewModelScope.launch {
        val value = _state.value
        val plan = buildReminderSavePlan(courseId, value)
        reminderRepository.saveRule(plan.rule)
        existingOverrides.forEach { reminderRepository.deleteOverride(it.occurrenceKey) }
        plan.overrides.forEach { reminderRepository.saveOverride(it) }
        ReminderScheduler.requestRebuild(context)
        _state.value = value.copy(saved = true)
    }
}
