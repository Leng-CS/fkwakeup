package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lengcs.fkwakeup.core.database.entity.OnlineCourseWindowEntity

@Dao
interface OnlineCourseWindowDao {
    @Insert
    suspend fun insert(window: OnlineCourseWindowEntity): Long

    @Insert
    suspend fun insertAll(windows: List<OnlineCourseWindowEntity>): List<Long>

    @Update
    suspend fun update(window: OnlineCourseWindowEntity)

    @Delete
    suspend fun delete(window: OnlineCourseWindowEntity)

    @Query("SELECT * FROM online_course_windows WHERE course_id = :courseId ORDER BY start_date_epoch_day, end_date_epoch_day")
    suspend fun getByCourse(courseId: Long): List<OnlineCourseWindowEntity>

    @Query("DELETE FROM online_course_windows WHERE course_id = :courseId")
    suspend fun deleteByCourse(courseId: Long)
}
