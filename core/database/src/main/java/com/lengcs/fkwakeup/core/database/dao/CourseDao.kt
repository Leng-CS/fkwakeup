package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.lengcs.fkwakeup.core.database.entity.CourseEntity
import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import kotlinx.coroutines.flow.Flow

data class CourseWithSessions(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "course_id")
    val sessions: List<CourseSessionEntity>,
)

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Transaction
    @Query("SELECT * FROM courses WHERE term_id = :termId ORDER BY name ASC")
    fun observeCoursesWithSessions(termId: Long): Flow<List<CourseWithSessions>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE term_id = :termId")
    suspend fun getByTerm(termId: Long): List<CourseEntity>

    @Query("SELECT COUNT(*) FROM courses WHERE term_id = :termId")
    suspend fun countByTerm(termId: Long): Int
}
