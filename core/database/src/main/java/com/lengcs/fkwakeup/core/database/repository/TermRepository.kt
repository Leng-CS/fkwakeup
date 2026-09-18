package com.lengcs.fkwakeup.core.database.repository

import com.lengcs.fkwakeup.core.database.dao.SectionTemplateDao
import com.lengcs.fkwakeup.core.database.dao.TermDao
import com.lengcs.fkwakeup.core.database.mapper.toDomain
import com.lengcs.fkwakeup.core.database.mapper.toEntity
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermRepository @Inject constructor(
    private val termDao: TermDao,
    private val sectionTemplateDao: SectionTemplateDao,
) {

    fun observeTerms(includeArchived: Boolean = false): Flow<List<Term>> =
        if (includeArchived) {
            termDao.observeAll().map { it.map { e -> e.toDomain() } }
        } else {
            termDao.observeActive().map { it.map { e -> e.toDomain() } }
        }

    suspend fun getTerm(id: Long): Term? = termDao.getById(id)?.toDomain()

    /**
     * 新建学期，并写入默认节次时间表。
     */
    suspend fun createTerm(
        name: String,
        startMonday: LocalDate,
        totalWeeks: Int,
    ): Long {
        val termId = termDao.insert(
            Term(
                name = name,
                startMonday = startMonday,
                totalWeeks = totalWeeks,
                createdAt = Instant.now(),
            ).toEntity(),
        )
        replaceSections(termId, DefaultSections.forTerm(termId))
        return termId
    }

    suspend fun updateTerm(term: Term) {
        termDao.update(term.toEntity())
    }

    suspend fun setArchived(termId: Long, archived: Boolean) {
        termDao.setArchived(termId, archived)
    }

    suspend fun deleteTerm(term: Term) {
        termDao.delete(term.toEntity())
    }

    fun observeSections(termId: Long): Flow<List<SectionTemplate>> =
        sectionTemplateDao.observeByTerm(termId).map { it.map { e -> e.toDomain() } }

    suspend fun getSections(termId: Long): List<SectionTemplate> =
        sectionTemplateDao.getByTerm(termId).map { it.toDomain() }

    suspend fun replaceSections(termId: Long, templates: List<SectionTemplate>) {
        sectionTemplateDao.deleteByTerm(termId)
        sectionTemplateDao.insertAll(templates.map { it.toEntity() })
    }
}
