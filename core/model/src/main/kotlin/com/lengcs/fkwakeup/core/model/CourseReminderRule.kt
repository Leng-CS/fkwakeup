package com.lengcs.fkwakeup.core.model

enum class RecurringReminderMode { NONE, ALL_FUTURE }

data class CourseReminderRule(
    val courseId: Long,
    val recurringMode: RecurringReminderMode = RecurringReminderMode.NONE,
    val primaryMinutesBefore: Int = 10,
    val secondaryMinutesBefore: Int? = null,
    val asyncOpenEnabled: Boolean = false,
    val asyncOpenMinutesOfDay: Int = 9 * 60,
    val asyncDeadlineEnabled: Boolean = false,
    val asyncDeadlineDaysBefore: Int = 1,
    val asyncDeadlineMinutesOfDay: Int = 20 * 60,
) {
    init {
        require(primaryMinutesBefore >= 0)
        require(secondaryMinutesBefore == null || secondaryMinutesBefore >= 0)
        require(asyncOpenMinutesOfDay in 0 until 24 * 60)
        require(asyncDeadlineDaysBefore >= 0)
        require(asyncDeadlineMinutesOfDay in 0 until 24 * 60)
    }
}

enum class ReminderOccurrenceKind { SESSION, ASYNC_OPEN, ASYNC_DEADLINE }

data class ReminderOccurrenceOverride(
    val occurrenceKey: String,
    val courseId: Long,
    val kind: ReminderOccurrenceKind,
    val sourceId: Long,
    val weekNumber: Int? = null,
    val enabled: Boolean,
    val primaryMinutesBefore: Int? = null,
    val secondaryMinutesBefore: Int? = null,
) {
    init {
        require(occurrenceKey.isNotBlank())
        require(primaryMinutesBefore == null || primaryMinutesBefore >= 0)
        require(secondaryMinutesBefore == null || secondaryMinutesBefore >= 0)
        if (kind == ReminderOccurrenceKind.SESSION) require(weekNumber != null && weekNumber >= 1)
    }

    companion object {
        fun sessionKey(sessionId: Long, weekNumber: Int) = "session:$sessionId:week:$weekNumber"
        fun asyncOpenKey(windowId: Long) = "window:$windowId:open"
        fun asyncDeadlineKey(windowId: Long) = "window:$windowId:deadline"
    }
}
