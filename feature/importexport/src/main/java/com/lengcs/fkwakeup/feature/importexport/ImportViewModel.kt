package com.lengcs.fkwakeup.feature.importexport

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.common.WeekSpecFallback
import com.lengcs.fkwakeup.core.common.WeekSpecFormatter
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.importer.TimetableImporter
import com.lengcs.fkwakeup.core.importer.model.ImportError
import com.lengcs.fkwakeup.core.importer.model.ImportResult
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

enum class ImportStage { Input, Preview, Failure, Done }

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    var stage: ImportStage by mutableStateOf(ImportStage.Input)
        private set

    var inputText: String by mutableStateOf("")
        private set

    var termName: String by mutableStateOf("")
        private set

    var totalWeeks: Int by mutableStateOf(18)
        private set

    var startDate: LocalDate? by mutableStateOf(null)
        private set

    var courses: List<MergedCourse> by mutableStateOf(emptyList())
        private set

    var errors: List<ImportError> by mutableStateOf(emptyList())
        private set

    var sessionCount: Int by mutableStateOf(0)
        private set

    var isImporting: Boolean by mutableStateOf(false)
        private set

    var importedTermId: Long? by mutableStateOf(null)
        private set

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    // ---- 输入 ----

    fun onTextChanged(text: String) {
        inputText = text
    }

    fun pasteFromClipboard() {
        val text = readClipboardText(appContext)
        if (text.isNullOrBlank()) {
            _messages.trySend("剪贴板里没有内容")
        } else {
            inputText = text
            _messages.trySend("已粘贴")
        }
    }

    fun clearInput() {
        inputText = ""
    }

    fun copyPrompt() {
        copyPlainText(appContext, "课表识别提示词", ImportTexts.prompt(appContext))
        _messages.trySend(appContext.getString(R.string.import_copied))
    }

    fun copyErrorReport() {
        val report = ImportTexts.buildRepairReport(appContext, errors, inputText)
        copyPlainText(appContext, "课表导入错误报告", report)
        _messages.trySend(appContext.getString(R.string.import_report_copied))
    }

    // ---- 解析 ----

    fun parse() {
        val result: ImportResult = TimetableImporter.import(inputText)
        sessionCount = result.sessionCount

        if (result.errors.isEmpty()) {
            errors = emptyList()
            termName = result.term?.name.orEmpty()
            totalWeeks = result.term?.totalWeeks ?: 18
            startDate = result.term?.startMonday
            courses = result.courses
            stage = ImportStage.Preview
        } else {
            errors = result.errors
            stage = ImportStage.Failure
        }
    }

    fun backToInput() {
        stage = ImportStage.Input
    }

    fun reset() {
        stage = ImportStage.Input
        inputText = ""
        courses = emptyList()
        errors = emptyList()
        sessionCount = 0
        isImporting = false
    }

    // ---- 预览纠偏 ----

    fun onTermNameChanged(value: String) {
        termName = value
    }

    fun onTotalWeeksChanged(value: Int) {
        totalWeeks = value.coerceIn(1, 30)
    }

    fun renameCourse(index: Int, name: String, teacher: String?) {
        courses = courses.mapIndexed { i, course ->
            if (i == index) course.copy(name = name, teacher = teacher) else course
        }
    }

    fun deleteCourse(index: Int) {
        courses = courses.filterIndexed { i, _ -> i != index }
    }

    /** 把某条时间段从原课程里拆出来，单独成为一门新课 */
    fun splitSession(courseIndex: Int, sessionIndex: Int) {
        val target = courses.getOrNull(courseIndex) ?: return
        val session = target.sessions.getOrNull(sessionIndex) ?: return

        val remaining = target.copy(sessions = target.sessions.filterIndexed { i, _ -> i != sessionIndex })
        val updated = courses.toMutableList()
        updated[courseIndex] = remaining
        updated.add(
            MergedCourse(
                name = target.name,
                teacher = target.teacher,
                sessions = listOf(session),
            ),
        )
        courses = updated
    }

    /** 把 fromIndex 课程的全部时间段并入 toIndex 课程 */
    fun mergeCourses(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val from = courses.getOrNull(fromIndex) ?: return
        val to = courses.getOrNull(toIndex) ?: return

        val merged = to.copy(sessions = (to.sessions + from.sessions)
            .sortedWith(compareBy({ it.dayOfWeek ?: 0 }, { it.startSection ?: 0 })))

        val updated = courses.toMutableList()
        updated[toIndex] = merged
        updated.removeAt(fromIndex)
        courses = updated
    }

    /**
     * 修改某条时间段的字段。**只影响这一条**，不动同课程的其它时间段。
     *
     * 周次传的是点选出来的集合，这里用 [WeekSpecFormatter] 转回存储用的表达式 ——
     * 存储格式仍是字符串，未改导入契约、不需要数据库迁移。
     */
    fun updateSession(
        courseIndex: Int,
        sessionIndex: Int,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        weeks: Set<Int>,
        location: String?,
        note: String?,
    ) {
        val course = courses.getOrNull(courseIndex) ?: return
        val draft = course.sessions.getOrNull(sessionIndex) ?: return

        // 起止节写反了自动纠正，比报错省事（与周视图抽屉一致）
        val from = minOf(startSection, endSection).coerceAtLeast(1)
        val to = maxOf(startSection, endSection).coerceAtLeast(from)

        val updatedSessions = course.sessions.toMutableList()
        updatedSessions[sessionIndex] = draft.copy(
            dayOfWeek = dayOfWeek.coerceIn(1, 7),
            startSection = from,
            endSection = to,
            weeks = WeekSpecFormatter.format(weeks, totalWeeks),
            location = location?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null },
        )
        courses = courses.toMutableList().also {
            it[courseIndex] = course.copy(sessions = updatedSessions)
        }
    }

    /** 设置课块颜色。null 表示「自动」（按课名哈希），与 App 内其它地方语义一致。 */
    fun updateCourseColor(courseIndex: Int, colorArgb: Int?) {
        courses = courses.mapIndexed { i, course ->
            if (i == courseIndex) course.copy(colorArgb = colorArgb) else course
        }
    }

    /**
     * 修改学期起始日。
     *
     * 必须**对齐到周一** —— `startMonday` 的语义是「第一周的周一」，
     * `CurrentWeekCalculator` 依赖它算周次；存一个周中日期会让「今天第几周」出现半周边界错误。
     */
    fun onStartDateChanged(date: LocalDate) {
        startDate = date.with(DayOfWeek.MONDAY)
    }

    // ---- 落库 ----

    fun confirmImport() {
        if (isImporting) return
        viewModelScope.launch {
            isImporting = true
            val start = startDate ?: LocalDate.now()
            val termId = termRepository.createTerm(
                name = termName.ifBlank { "未命名学期" },
                startMonday = start,
                totalWeeks = totalWeeks,
            )

            courses.forEach { course ->
                val courseId = courseRepository.addCourse(
                    Course(
                        termId = termId,
                        name = course.name,
                        teacher = course.teacher,
                        colorArgb = course.colorArgb,
                    ),
                )
                course.sessions.forEach { draft ->
                    // 周几/节次缺失就跳过：用户没补齐的字段宁可少写一条，也不写坏数据
                    val startSection = draft.startSection ?: return@forEach
                    val endSection = draft.endSection ?: startSection
                    val dayOfWeek = draft.dayOfWeek ?: return@forEach
                    courseRepository.addSession(
                        CourseSession(
                            courseId = courseId,
                            dayOfWeek = dayOfWeek,
                            startSection = startSection,
                            endSection = endSection,
                            // #23：空白周次必须回退成整学期。写空串会让周视图解析失败并把
                            // 这条时间段静默跳过，整门课永久不显示且没有报错。
                            weekSpec = WeekSpecFallback.orFullTerm(draft.weeks, totalWeeks),
                            location = draft.location,
                            note = draft.note,
                        ),
                    )
                }
            }

            settingsRepository.setCurrentTerm(termId)
            importedTermId = termId
            isImporting = false
            stage = ImportStage.Done

            // 写库后主动刷新小组件，否则要等 15 分钟兜底
            WidgetRefreshScheduler.refreshNow(appContext)
        }
    }

    /**
     * 预览页编辑抽屉里「节次」滚轮的上限。
     *
     * 取默认节次表长度与**草稿里已用到的最大节次**的较大者：
     * 若 AI 给的 JSON 带了更长的节次表、或某门课用到第 13 节，
     * 直接用默认的 12 会把它夹掉，用户一保存就静默改坏了数据。
     */
    fun defaultSectionCount(): Int {
        val used = courses.flatMap { it.sessions }.mapNotNull { it.endSection }.maxOrNull() ?: 0
        return maxOf(DefaultSections.forTerm(0L).size, used)
    }
}
