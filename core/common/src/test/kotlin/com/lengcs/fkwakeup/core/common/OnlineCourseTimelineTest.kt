package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.Term
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class OnlineCourseTimelineTest {
    private val term = Term(
        id = 1L,
        name = "秋季学期",
        startMonday = LocalDate.of(2026, 9, 7),
        totalWeeks = 18,
        createdAt = Instant.EPOCH,
    )

    private fun window(start: String, end: String, id: Long = 1L) = OnlineCourseWindow(
        id = id,
        courseId = 1L,
        startDate = LocalDate.parse(start),
        endDate = LocalDate.parse(end),
    )

    @Test
    fun `开始日和结束日当天都属于进行中`() {
        val target = window("2026-09-10", "2026-09-20")

        assertThat(OnlineCourseTimeline.phase(target, LocalDate.parse("2026-09-10")))
            .isEqualTo(OnlineCoursePhase.ACTIVE)
        assertThat(OnlineCourseTimeline.phase(target, LocalDate.parse("2026-09-20")))
            .isEqualTo(OnlineCoursePhase.ACTIVE)
    }

    @Test
    fun `学期外开放期不相交而跨边界开放期相交`() {
        assertThat(OnlineCourseTimeline.overlapsTerm(window("2026-08-01", "2026-08-31"), term)).isFalse()
        assertThat(OnlineCourseTimeline.overlapsTerm(window("2026-09-01", "2026-09-07"), term)).isTrue()
        assertThat(OnlineCourseTimeline.overlapsTerm(window("2027-01-10", "2027-02-01"), term)).isTrue()
        assertThat(OnlineCourseTimeline.overlapsTerm(window("2027-02-01", "2027-03-01"), term)).isFalse()
    }

    @Test
    fun `网课按进行中未开始已结束排序`() {
        val course = Course(id = 1L, termId = 1L, name = "大学英语")
        val entry = CourseWithSessions(
            course = course,
            sessions = emptyList(),
            onlineWindows = listOf(
                window("2026-08-01", "2026-08-31", 1L),
                window("2026-10-01", "2026-11-01", 2L),
                window("2026-09-01", "2026-09-30", 3L),
            ),
        )

        val result = OnlineCourseTimeline.items(listOf(entry), LocalDate.parse("2026-09-15"))

        assertThat(result.map { it.window.id }).containsExactly(3L, 2L, 1L).inOrder()
        assertThat(result.map { it.phase }).containsExactly(
            OnlineCoursePhase.ACTIVE,
            OnlineCoursePhase.UPCOMING,
            OnlineCoursePhase.ENDED,
        ).inOrder()
    }
}
