package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lengcs.fkwakeup.core.database.entity.TermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {

    @Insert
    suspend fun insert(term: TermEntity): Long

    @Update
    suspend fun update(term: TermEntity)

    @Delete
    suspend fun delete(term: TermEntity)

    @Query("SELECT * FROM terms WHERE is_archived = 0 ORDER BY start_monday_epoch_day DESC")
    fun observeActive(): Flow<List<TermEntity>>

    @Query("SELECT * FROM terms ORDER BY start_monday_epoch_day DESC")
    fun observeAll(): Flow<List<TermEntity>>

    @Query("SELECT * FROM terms WHERE id = :id")
    fun observeById(id: Long): Flow<TermEntity?>

    @Query("SELECT * FROM terms WHERE id = :id")
    suspend fun getById(id: Long): TermEntity?

    @Query("UPDATE terms SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM terms")
    suspend fun count(): Int
}
