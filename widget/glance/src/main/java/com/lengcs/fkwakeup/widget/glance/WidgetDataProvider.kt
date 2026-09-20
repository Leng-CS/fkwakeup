package com.lengcs.fkwakeup.widget.glance

import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.common.CurrentTermPick
import com.lengcs.fkwakeup.core.common.CurrentWeekCalculator
import com.lengcs.fkwakeup.core.common.LessonWindow
import com.lengcs.fkwakeup.core.common.ScheduleLayout
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** 小组件「最近课程」列表里的一条 */
data class WidgetLesson(
    val name: String,
    val location: String?,
    /** 「第 5-7 节」 */
    val sectionText: String,
    /** 「14:00-14:45」 */
    val timeText: String,
    val startMinutes: Int,
    val endMinutes: Int,
    /** 该课程最终使用的颜色（自定义色优先，否则按课名哈希），用于左侧色条 */
    val colorArgb: Int,
    /** 此刻正在上这节课 */
    val isOngoing: Boolean,
)

/** 小组件数据快照 */
data class WidgetData(
    val termName: String?,
    /** 「第 2 周」 */
    val weekText: String,
    /** 「9月20日 周日」 */
    val dateText: String,
    /**
     * 今天尚未结束的课程（含正在上），按开始时间排序。
     * 已结束的不在其中 —— 空列表就是「今天没有更多课」。
     */
    val lessons: List<WidgetLesson>,
    /** 下一个需要刷新的课程边界时刻；null 表示今天没有剩余边界 */
    val nextBoundary: LocalDateTime?,
)

class WidgetDataProvider(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun load(now: LocalDateTime = LocalDateTime.now()): WidgetData? {
        val settings = settingsRepository.settings.first()
        // 与 CurrentTermProvider 共用同一套「当前学期」选择规则（CHANGELOG #26）：
        // 配置值优先，无效才退回「开学日期最晚的未归档学期」。
        // 小组件这边没走 CurrentTermProvider，是因为 Glance 侧是手动构造（见 WidgetDependencies）。
        val termId = CurrentTermPick.pick(
            configuredTermId = settings.currentTermId,
            fallbackIds = termRepository.observeTerms().first().map { it.id },
        ) ?: return null

        val term = termRepository.getTerm(termId) ?: return null
        val sections = termRepository.getSections(termId)
        val courses = courseRepository.observeCourses(termId).first()

        val today = now.toLocalDate()
        val week = CurrentWeekCalculator.status(term, today).week
        val dayOfWeek = today.dayOfWeek.value
        val nowMinutes = now.hour * 60 + now.minute

        val all = todayLessons(courses, sections, week, dayOfWeek, term.totalWeeks)
        // #30：列表 = 今天尚未结束的课（含正在上），最多取多少条由各形态自己决定
        val indexes = LessonWindow.upcomingIndexes(
            starts = all.map { it.startMinutes },
            ends = all.map { it.endMinutes },
            nowMinutes = nowMinutes,
            max = Int.MAX_VALUE,
        )
        val lessons = indexes.map { i ->
            val l = all[i]
            l.copy(isOngoing = l.startMinutes <= nowMinutes)
        }

        val boundaryMinutes = LessonWindow.nextBoundaryMinutes(
            starts = all.map { it.startMinutes },
            ends = all.map { it.endMinutes },
            nowMinutes = nowMinutes,
        )

        return WidgetData(
            termName = term.name,
            weekText = "第 ${week}周",
            dateText = formatDate(today),
            lessons = lessons,
            nextBoundary = boundaryMinutes?.let { today.atStartOfDay().plusMinutes(it.toLong()) },
        )
    }

    /** 今天（周 [dayOfWeek]）该周次命中的课程，按开始时间排序，颜色已解析 */
    private suspend fun todayLessons(
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        week: Int,
        dayOfWeek: Int,
        totalWeeks: Int,
    ): List<WidgetLesson> {
        val sectionTimes = sections.associateBy { it.index }
        val result = mutableListOf<WidgetLesson>()

        for (entry in courses) {
            for (session in entry.sessions) {
                if (session.dayOfWeek != dayOfWeek) continue
                val weeks = ScheduleLayout.build(
                    listOf(CourseWithSessions(entry.course, listOf(session))),
                    week,
                    totalWeeks,
                )
                if (weeks.isEmpty()) continue

                val start = sectionTimes[session.startSection]
                val end = sectionTimes[session.endSection]
                if (start == null || end == null) continue

                val sectionText = if (session.startSection == session.endSection) {
                    "第${session.startSection}节"
                } else {
                    "第${session.startSection}-${session.endSection}节"
                }

                result += WidgetLesson(
                    name = entry.course.name,
                    location = session.location,
                    sectionText = sectionText,
                    timeText = "${formatClock(start.startMinutes)}-${formatClock(end.endMinutes)}",
                    startMinutes = start.startMinutes,
                    endMinutes = end.endMinutes,
                    colorArgb = CourseColorPalette.resolve(entry.course.colorArgb, entry.course.name),
                    isOngoing = false,
                )
            }
        }
        return result.sortedBy { it.startMinutes }
    }

    /** 「9月20日 周日」 */
    private fun formatDate(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.CHINA)
        return "${date.monthValue}月${date.dayOfMonth}日 $weekday"
    }

    private fun formatClock(minutes: Int): String =
        "%02d:%02d".format(minutes / 60, minutes % 60)
}
