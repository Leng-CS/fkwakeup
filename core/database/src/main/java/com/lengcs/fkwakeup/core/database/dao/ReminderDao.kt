package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lengcs.fkwakeup.core.database.entity.CourseReminderRuleEntity
import com.lengcs.fkwakeup.core.database.entity.ReminderOccurrenceOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM course_reminder_rules WHERE course_id = :courseId")
    fun observeRule(courseId: Long): Flow<CourseReminderRuleEntity?>

    @Query("SELECT * FROM course_reminder_rules WHERE course_id = :courseId")
    suspend fun getRule(courseId: Long): CourseReminderRuleEntity?

    @Query("SELECT * FROM course_reminder_rules")
    suspend fun getAllRules(): List<CourseReminderRuleEntity>

    @Upsert
    suspend fun upsertRule(rule: CourseReminderRuleEntity)

    @Query("DELETE FROM course_reminder_rules WHERE course_id = :courseId")
    suspend fun deleteRule(courseId: Long)

    @Query("SELECT * FROM reminder_occurrence_overrides WHERE course_id = :courseId")
    fun observeOverrides(courseId: Long): Flow<List<ReminderOccurrenceOverrideEntity>>

    @Query("SELECT * FROM reminder_occurrence_overrides")
    suspend fun getAllOverrides(): List<ReminderOccurrenceOverrideEntity>

    @Upsert
    suspend fun upsertOverride(override: ReminderOccurrenceOverrideEntity)

    @Query("DELETE FROM reminder_occurrence_overrides WHERE occurrence_key = :key")
    suspend fun deleteOverride(key: String)
}
