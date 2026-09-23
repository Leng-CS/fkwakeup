package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlannerTest {
    private val term = Term(1, "秋季", LocalDate.of(2026, 9, 7), 18, createdAt = Instant.EPOCH)
    private val sections = listOf(SectionTemplate(1, 1, 8 * 60, 8 * 60 + 45))
    private val session = CourseSession(11, 2, 1, 1, 1, "1-2")
    private val course = CourseWithSessions(Course(2, 1, "高数"), listOf(session))

    @Test
    fun `all future rule creates reminders for every occurrence`() {
        val result = ReminderPlanner.plan(
            term, listOf(course), sections,
            listOf(CourseReminderRule(2, RecurringReminderMode.ALL_FUTURE)), emptyList(),
        )
        assertThat(result.map { it.triggerAt }).containsExactly(
            LocalDateTime.of(2026, 9, 7, 7, 50),
            LocalDateTime.of(2026, 9, 14, 7, 50),
        ).inOrder()
    }

    @Test
    fun `single override enables only selected occurrence and custom time`() {
        val result = ReminderPlanner.plan(
            term, listOf(course), sections,
            listOf(CourseReminderRule(2)),
            listOf(
                ReminderOccurrenceOverride(
                    ReminderOccurrenceOverride.sessionKey(11, 2), 2,
                    ReminderOccurrenceKind.SESSION, 11, 2, true, 30,
                ),
            ),
        )
        assertThat(result).hasSize(1)
        assertThat(result.single().triggerAt).isEqualTo(LocalDateTime.of(2026, 9, 14, 7, 30))
    }

    @Test
    fun `disabled override excludes one occurrence from course rule`() {
        val result = ReminderPlanner.plan(
            term, listOf(course), sections,
            listOf(CourseReminderRule(2, RecurringReminderMode.ALL_FUTURE)),
            listOf(
                ReminderOccurrenceOverride(
                    ReminderOccurrenceOverride.sessionKey(11, 1), 2,
                    ReminderOccurrenceKind.SESSION, 11, 1, false,
                ),
            ),
        )
        assertThat(result.map { it.occurrenceDate }).containsExactly(LocalDate.of(2026, 9, 14))
    }

    @Test
    fun `async reminders use open and deadline clock defaults`() {
        val online = course.copy(
            sessions = emptyList(),
            onlineWindows = listOf(OnlineCourseWindow(21, 2, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20))),
        )
        val result = ReminderPlanner.plan(
            term, listOf(online), sections,
            listOf(CourseReminderRule(2, asyncOpenEnabled = true, asyncDeadlineEnabled = true)), emptyList(),
        )
        assertThat(result.map { it.triggerAt }).containsExactly(
            LocalDateTime.of(2026, 9, 10, 9, 0),
            LocalDateTime.of(2026, 9, 19, 20, 0),
        ).inOrder()
    }
}
