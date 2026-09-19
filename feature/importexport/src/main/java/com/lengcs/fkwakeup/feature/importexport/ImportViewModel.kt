package com.lengcs.fkwakeup.feature.importexport

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    ),
                )
                course.sessions.forEach { draft ->
                    val startSection = draft.startSection ?: return@forEach
                    val endSection = draft.endSection ?: startSection
                    val dayOfWeek = draft.dayOfWeek ?: return@forEach
                    courseRepository.addSession(
                        CourseSession(
                            courseId = courseId,
                            dayOfWeek = dayOfWeek,
                            startSection = startSection,
                            endSection = endSection,
                            weekSpec = draft.weeks ?: "1-$totalWeeks",
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

    /** 节次时间表在 createTerm 里用默认值写入，这里暴露给 UI 展示条数 */
    fun defaultSectionCount(): Int = DefaultSections.forTerm(0L).size
}
