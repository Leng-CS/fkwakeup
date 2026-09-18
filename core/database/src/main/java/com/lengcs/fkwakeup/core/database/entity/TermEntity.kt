package com.lengcs.fkwakeup.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terms",
    indices = [Index("name")],
)
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** LocalDate.toEpochDay() */
    @ColumnInfo(name = "start_monday_epoch_day") val startMondayEpochDay: Long,
    @ColumnInfo(name = "total_weeks") val totalWeeks: Int,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
)
