package com.lengcs.fkwakeup.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "section_templates",
    primaryKeys = ["term_id", "section_index"],
    foreignKeys = [
        ForeignKey(
            entity = TermEntity::class,
            parentColumns = ["id"],
            childColumns = ["term_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SectionTemplateEntity(
    @ColumnInfo(name = "term_id") val termId: Long,
    @ColumnInfo(name = "section_index") val index: Int,
    @ColumnInfo(name = "start_minutes") val startMinutes: Int,
    @ColumnInfo(name = "end_minutes") val endMinutes: Int,
)
