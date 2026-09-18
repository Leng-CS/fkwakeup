package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseSessionDao {

    @Insert
    suspend fun insert(session: CourseSessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<CourseSessionEntity>): List<Long>

    @Update
    suspend fun update(session: CourseSessionEntity)

    @Delete
    suspend fun delete(session: CourseSessionEntity)

    @Query("SELECT * FROM course_sessions WHERE course_id = :courseId ORDER BY day_of_week, start_section")
    fun observeByCourse(courseId: Long): Flow<List<CourseSessionEntity>>

    @Query("SELECT * FROM course_sessions WHERE course_id = :courseId ORDER BY day_of_week, start_section")
    suspend fun getByCourse(courseId: Long): List<CourseSessionEntity>

    @Query(
        """
        SELECT course_sessions.* FROM course_sessions
        INNER JOIN courses ON courses.id = course_sessions.course_id
        WHERE courses.term_id = :termId
        ORDER BY course_sessions.day_of_week, course_sessions.start_section
        """,
    )
    fun observeByTerm(termId: Long): Flow<List<CourseSessionEntity>>

    @Query("DELETE FROM course_sessions WHERE course_id = :courseId")
    suspend fun deleteByCourse(courseId: Long)
}
