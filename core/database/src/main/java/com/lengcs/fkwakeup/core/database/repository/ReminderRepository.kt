package com.lengcs.fkwakeup.core.database.repository

import com.lengcs.fkwakeup.core.database.dao.ReminderDao
import com.lengcs.fkwakeup.core.database.mapper.toDomain
import com.lengcs.fkwakeup.core.database.mapper.toEntity
import com.lengcs.fkwakeup.core.model.CourseReminderRule
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(private val dao: ReminderDao) {
    fun observeRule(courseId: Long): Flow<CourseReminderRule?> = dao.observeRule(courseId).map { it?.toDomain() }
    fun observeOverrides(courseId: Long): Flow<List<ReminderOccurrenceOverride>> =
        dao.observeOverrides(courseId).map { rows -> rows.map { it.toDomain() } }
    suspend fun getAllRules() = dao.getAllRules().map { it.toDomain() }
    suspend fun getAllOverrides() = dao.getAllOverrides().map { it.toDomain() }
    suspend fun saveRule(rule: CourseReminderRule) = dao.upsertRule(rule.toEntity())
    suspend fun deleteRule(courseId: Long) = dao.deleteRule(courseId)
    suspend fun saveOverride(override: ReminderOccurrenceOverride) = dao.upsertOverride(override.toEntity())
    suspend fun deleteOverride(key: String) = dao.deleteOverride(key)
}
