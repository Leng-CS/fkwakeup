package com.lengcs.fkwakeup.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "online_course_windows",
    indices = [
        Index("course_id"),
        Index("start_date_epoch_day"),
        Index("end_date_epoch_day"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OnlineCourseWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "course_id") val courseId: Long,
    @ColumnInfo(name = "start_date_epoch_day") val startDateEpochDay: Long,
    @ColumnInfo(name = "end_date_epoch_day") val endDateEpochDay: Long,
    val platform: String?,
    val url: String?,
    val note: String?,
)
