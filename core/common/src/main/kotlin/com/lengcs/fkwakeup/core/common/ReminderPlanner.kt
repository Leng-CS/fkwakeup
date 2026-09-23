package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.CourseReminderRule
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.RecurringReminderMode
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceKind
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceOverride
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PlannedReminder(
    val alarmKey: String,
    val courseId: Long,
    val courseName: String,
    val kind: ReminderOccurrenceKind,
    val sourceId: Long,
    val occurrenceDate: LocalDate,
    val triggerAt: LocalDateTime,
    val startAt: LocalDateTime?,
    val place: String?,
)

object ReminderPlanner {
    fun plan(
        term: Term,
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        rules: List<CourseReminderRule>,
        overrides: List<ReminderOccurrenceOverride>,
    ): List<PlannedReminder> {
        val rulesByCourse = rules.associateBy { it.courseId }
        val overridesByKey = overrides.associateBy { it.occurrenceKey }
        val sectionTimes = sections.associateBy { it.index }
        return buildList {
            courses.forEach { entry ->
                val rule = rulesByCourse[entry.course.id] ?: return@forEach
                entry.sessions.forEach { session ->
                    val weeks = WeekSpecParser.parseOrNull(session.weekSpec, term.totalWeeks) ?: emptySet()
                    weeks.forEach { week ->
                        val key = ReminderOccurrenceOverride.sessionKey(session.id, week)
                        val override = overridesByKey[key]
                        val enabled = override?.enabled ?: (rule.recurringMode == RecurringReminderMode.ALL_FUTURE)
                        if (!enabled) return@forEach
                        val start = sectionTimes[session.startSection] ?: return@forEach
                        val date = term.startMonday.plusWeeks((week - 1).toLong())
                            .plusDays((session.dayOfWeek - 1).toLong())
                        val startAt = LocalDateTime.of(date, minutesToTime(start.startMinutes))
                        offsets(rule, override).forEach { before ->
                            add(
                                PlannedReminder(
                                    alarmKey = "$key:before:$before",
                                    courseId = entry.course.id,
                                    courseName = entry.course.name,
                                    kind = ReminderOccurrenceKind.SESSION,
                                    sourceId = session.id,
                                    occurrenceDate = date,
                                    triggerAt = startAt.minusMinutes(before.toLong()),
                                    startAt = startAt,
                                    place = session.onlinePlatform ?: session.location,
                                ),
                            )
                        }
                    }
                }
                entry.onlineWindows.forEach { window ->
                    val openKey = ReminderOccurrenceOverride.asyncOpenKey(window.id)
                    val openOverride = overridesByKey[openKey]
                    if (openOverride?.enabled ?: rule.asyncOpenEnabled) {
                        add(
                            PlannedReminder(
                                openKey, entry.course.id, entry.course.name,
                                ReminderOccurrenceKind.ASYNC_OPEN, window.id, window.startDate,
                                LocalDateTime.of(window.startDate, minutesToTime(rule.asyncOpenMinutesOfDay)),
                                null, window.platform,
                            ),
                        )
                    }
                    val deadlineKey = ReminderOccurrenceOverride.asyncDeadlineKey(window.id)
                    val deadlineOverride = overridesByKey[deadlineKey]
                    if (deadlineOverride?.enabled ?: rule.asyncDeadlineEnabled) {
                        val date = window.endDate.minusDays(rule.asyncDeadlineDaysBefore.toLong())
                        add(
                            PlannedReminder(
                                deadlineKey, entry.course.id, entry.course.name,
                                ReminderOccurrenceKind.ASYNC_DEADLINE, window.id, date,
                                LocalDateTime.of(date, minutesToTime(rule.asyncDeadlineMinutesOfDay)),
                                null, window.platform,
                            ),
                        )
                    }
                }
            }
        }.distinctBy { it.alarmKey }.sortedBy { it.triggerAt }
    }

    private fun offsets(
        rule: CourseReminderRule,
        override: ReminderOccurrenceOverride?,
    ): List<Int> = listOfNotNull(
        override?.primaryMinutesBefore ?: rule.primaryMinutesBefore,
        if (override != null && override.primaryMinutesBefore != null) {
            override.secondaryMinutesBefore
        } else {
            override?.secondaryMinutesBefore ?: rule.secondaryMinutesBefore
        },
    ).distinct()

    private fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
}
