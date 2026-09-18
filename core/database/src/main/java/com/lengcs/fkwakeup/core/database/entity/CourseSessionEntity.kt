package com.lengcs.fkwakeup.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_sessions",
    indices = [Index("course_id"), Index("day_of_week")],
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CourseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "course_id") val courseId: Long,
    @ColumnInfo(name = "day_of_week") val dayOfWeek: Int,
    @ColumnInfo(name = "start_section") val startSection: Int,
    @ColumnInfo(name = "end_section") val endSection: Int,
    @ColumnInfo(name = "week_spec") val weekSpec: String,
    val location: String?,
    val note: String?,
)
