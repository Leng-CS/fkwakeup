package com.lengcs.fkwakeup.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_occurrence_overrides",
    indices = [Index("course_id"), Index("source_id")],
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["course_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ReminderOccurrenceOverrideEntity(
    @PrimaryKey @ColumnInfo(name = "occurrence_key") val occurrenceKey: String,
    @ColumnInfo(name = "course_id") val courseId: Long,
    val kind: String,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "week_number") val weekNumber: Int?,
    val enabled: Boolean,
    @ColumnInfo(name = "primary_minutes_before") val primaryMinutesBefore: Int?,
    @ColumnInfo(name = "secondary_minutes_before") val secondaryMinutesBefore: Int?,
)
