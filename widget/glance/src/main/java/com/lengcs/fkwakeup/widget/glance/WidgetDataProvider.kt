package com.lengcs.fkwakeup.widget.glance

import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.common.CurrentTermPick
import com.lengcs.fkwakeup.core.common.CurrentWeekCalculator
import com.lengcs.fkwakeup.core.common.LessonWindow
import com.lengcs.fkwakeup.core.common.ScheduleLayout
import com.lengcs.fkwakeup.core.common.WidgetLessonRange
import com.lengcs.fkwakeup.core.common.WidgetLessonRangePicker
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** 小组件中一条独立的课程时间段。 */
data class WidgetLesson(
    val stableId: String,
    val date: LocalDate,
    val dateText: String,
    val name: String,
    val teacher: String?,
    val location: String?,
    val startSection: Int,
    val endSection: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val startTime: String,
    val endTime: String,
    val colorArgb: Int,
    val dateHeader: String? = null,
)

data class WidgetData(
    val termName: String,
    val weekText: String,
    val lessons: List<WidgetLesson>,
    val nextLesson: WidgetLesson?,
    val nextBoundary: LocalDateTime?,
)

class WidgetDataProvider(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun load(
        range: WidgetLessonRange = WidgetLessonRange.TODAY_REMAINING,
        now: LocalDateTime = LocalDateTime.now(),
    ): WidgetData? {
        val settings = settingsRepository.settings.first()
        val termId = CurrentTermPick.pick(
            configuredTermId = settings.currentTermId,
            fallbackIds = termRepository.observeTerms().first().map { it.id },
        ) ?: return null
        val term = termRepository.getTerm(termId) ?: return null
        val sections = termRepository.getSections(termId)
        val courses = courseRepository.observeCourses(termId).first()
        val today = now.toLocalDate()
        val nowMinutes = now.hour * 60 + now.minute

        val todayLessons = lessonsForDate(term, courses, sections, today)
        val selected = WidgetLessonRangePicker.dates(range, today)
            .flatMap { date -> lessonsForDate(term, courses, sections, date) }
            .filter { lesson ->
                WidgetLessonRangePicker.shouldKeep(
                    range = range,
                    date = lesson.date,
                    endMinutes = lesson.endMinutes,
                    today = today,
                    nowMinutes = nowMinutes,
                )
            }
            .sortedWith(compareBy<WidgetLesson>({ it.date }, { it.startMinutes }, { it.name }))
            .withDateHeaders(range, today)

        val boundaryMinutes = LessonWindow.nextBoundaryMinutes(
            starts = todayLessons.map { it.startMinutes },
            ends = todayLessons.map { it.endMinutes },
            nowMinutes = nowMinutes,
        )
        val next = findNextLesson(term, courses, sections, today, nowMinutes)
        val week = CurrentWeekCalculator.status(term, today).week
        return WidgetData(
            termName = term.name,
            weekText = "第 ${week} 周",
            lessons = selected,
            nextLesson = next,
            nextBoundary = boundaryMinutes?.let { today.atStartOfDay().plusMinutes(it.toLong()) },
        )
    }

    private fun lessonsForDate(
        term: com.lengcs.fkwakeup.core.model.Term,
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        date: LocalDate,
    ): List<WidgetLesson> {
        val sectionTimes = sections.associateBy(SectionTemplate::index)
        val week = CurrentWeekCalculator.status(term, date).week
        val result = mutableListOf<WidgetLesson>()
        for (course in courses) {
            for (session in course.sessions) {
                if (session.dayOfWeek != date.dayOfWeek.value) continue
                if (
                    ScheduleLayout.build(
                        listOf(CourseWithSessions(course.course, listOf(session))),
                        week,
                        term.totalWeeks,
                    ).isEmpty()
                ) continue
                val start = sectionTimes[session.startSection] ?: continue
                val end = sectionTimes[session.endSection] ?: continue
                result += WidgetLesson(
                    stableId = "${date.toEpochDay()}-${session.id}",
                    date = date,
                    dateText = formatDate(date),
                    name = course.course.name,
                    teacher = course.course.teacher,
                    location = session.location,
                    startSection = session.startSection,
                    endSection = session.endSection,
                    startMinutes = start.startMinutes,
                    endMinutes = end.endMinutes,
                    startTime = formatClock(start.startMinutes),
                    endTime = formatClock(end.endMinutes),
                    colorArgb = CourseColorPalette.resolve(course.course.colorArgb, course.course.name),
                )
            }
        }
        return result.sortedWith(compareBy({ it.startMinutes }, { it.name }))
    }

    /** 找到当前时刻之后的第一条课，用于当前范围为空的兜底入口。 */
    private fun findNextLesson(
        term: com.lengcs.fkwakeup.core.model.Term,
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        today: LocalDate,
        nowMinutes: Int,
    ): WidgetLesson? {
        val lastDay = term.startMonday.plusWeeks(term.totalWeeks.toLong()).minusDays(1)
        var date = today
        while (!date.isAfter(lastDay)) {
            val candidate = lessonsForDate(term, courses, sections, date)
                .firstOrNull { date != today || it.endMinutes > nowMinutes }
            if (candidate != null) return candidate
            date = date.plusDays(1)
        }
        return null
    }

    private fun List<WidgetLesson>.withDateHeaders(
        range: WidgetLessonRange,
        today: LocalDate,
    ): List<WidgetLesson> {
        val showHeaders = range == WidgetLessonRange.TODAY_AND_TOMORROW || range == WidgetLessonRange.THIS_WEEK
        return mapIndexed { index, lesson ->
            val startsDate = index == 0 || lesson.date != this[index - 1].date
            lesson.copy(dateHeader = if (showHeaders && startsDate) formatDateHeader(lesson.date, today) else null)
        }
    }

    private fun formatDateHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
    }

    private fun formatDate(date: LocalDate): String =
        "${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"

    private fun formatClock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
}
