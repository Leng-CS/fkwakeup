package com.lengcs.fkwakeup.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_reminder_rules",
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["course_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class CourseReminderRuleEntity(
    @PrimaryKey @ColumnInfo(name = "course_id") val courseId: Long,
    @ColumnInfo(name = "recurring_mode") val recurringMode: String,
    @ColumnInfo(name = "primary_minutes_before") val primaryMinutesBefore: Int,
    @ColumnInfo(name = "secondary_minutes_before") val secondaryMinutesBefore: Int?,
    @ColumnInfo(name = "async_open_enabled") val asyncOpenEnabled: Boolean,
    @ColumnInfo(name = "async_open_minutes_of_day") val asyncOpenMinutesOfDay: Int,
    @ColumnInfo(name = "async_deadline_enabled") val asyncDeadlineEnabled: Boolean,
    @ColumnInfo(name = "async_deadline_days_before") val asyncDeadlineDaysBefore: Int,
    @ColumnInfo(name = "async_deadline_minutes_of_day") val asyncDeadlineMinutesOfDay: Int,
)
