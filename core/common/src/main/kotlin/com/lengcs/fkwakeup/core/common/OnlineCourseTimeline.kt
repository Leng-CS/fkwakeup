package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.Term
import java.time.LocalDate

enum class OnlineCoursePhase { ACTIVE, UPCOMING, ENDED }

data class OnlineCourseItem(
    val course: Course,
    val window: OnlineCourseWindow,
    val phase: OnlineCoursePhase,
)

object OnlineCourseTimeline {
    fun termEnd(term: Term): LocalDate =
        term.startMonday.plusDays(term.totalWeeks.toLong() * 7L - 1L)

    fun overlapsTerm(window: OnlineCourseWindow, term: Term): Boolean =
        !window.endDate.isBefore(term.startMonday) && !window.startDate.isAfter(termEnd(term))

    fun phase(window: OnlineCourseWindow, today: LocalDate): OnlineCoursePhase = when {
        today.isBefore(window.startDate) -> OnlineCoursePhase.UPCOMING
        today.isAfter(window.endDate) -> OnlineCoursePhase.ENDED
        else -> OnlineCoursePhase.ACTIVE
    }

    /** 进行中 → 未开始 → 已结束；组内优先显示最近边界。 */
    fun items(courses: List<CourseWithSessions>, today: LocalDate): List<OnlineCourseItem> =
        courses.flatMap { entry ->
            entry.onlineWindows.map { window ->
                OnlineCourseItem(entry.course, window, phase(window, today))
            }
        }.sortedWith(
            compareBy<OnlineCourseItem> { phaseOrder(it.phase) }
                .thenBy { sortDate(it) }
                .thenBy { it.course.name },
        )

    private fun phaseOrder(phase: OnlineCoursePhase): Int = when (phase) {
        OnlineCoursePhase.ACTIVE -> 0
        OnlineCoursePhase.UPCOMING -> 1
        OnlineCoursePhase.ENDED -> 2
    }

    private fun sortDate(item: OnlineCourseItem): Long = when (item.phase) {
        OnlineCoursePhase.ACTIVE -> item.window.endDate.toEpochDay()
        OnlineCoursePhase.UPCOMING -> item.window.startDate.toEpochDay()
        OnlineCoursePhase.ENDED -> -item.window.endDate.toEpochDay()
    }
}
