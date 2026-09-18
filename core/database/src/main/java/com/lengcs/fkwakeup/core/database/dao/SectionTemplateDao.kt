package com.lengcs.fkwakeup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lengcs.fkwakeup.core.database.entity.SectionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<SectionTemplateEntity>)

    @Query("SELECT * FROM section_templates WHERE term_id = :termId ORDER BY section_index ASC")
    fun observeByTerm(termId: Long): Flow<List<SectionTemplateEntity>>

    @Query("SELECT * FROM section_templates WHERE term_id = :termId ORDER BY section_index ASC")
    suspend fun getByTerm(termId: Long): List<SectionTemplateEntity>

    @Query("DELETE FROM section_templates WHERE term_id = :termId")
    suspend fun deleteByTerm(termId: Long)

    @Query("SELECT COUNT(*) FROM section_templates WHERE term_id = :termId")
    suspend fun countByTerm(termId: Long): Int
}
