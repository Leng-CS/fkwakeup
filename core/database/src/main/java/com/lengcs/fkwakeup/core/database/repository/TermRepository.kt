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

    /**
     * 所有学期，**按开学日期由晚到早**（DAO 的 `ORDER BY start_monday_epoch_day DESC`）。
     *
     * ⚠️ 因此 `first()` **不是**「当前学期」，而是「开学日期最晚的学期」。
     * 曾经有代码拿它当当前学期用，导致课程被写进 / 搬到别的学期（CHANGELOG #26）。
     * 要拿当前学期，请用 [CurrentTermProvider.observeCurrentTerm] /
     * [CurrentTermProvider.resolveCurrentTermId]；要表达"选哪个学期"的规则，
     * 请复用 [com.lengcs.fkwakeup.core.common.CurrentTermPick]。
     */
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
        require(termId > 0) { "学期无效" }
        require(templates.isNotEmpty()) { "至少需要一节课" }
        val ordered = templates.sortedBy(SectionTemplate::index)
        require(ordered.map { it.index } == (1..ordered.size).toList()) { "节次必须从 1 开始连续编号" }
        require(ordered.zipWithNext().all { (previous, next) -> previous.endMinutes <= next.startMinutes }) {
            "相邻节次时间重叠"
        }
        // 编辑草稿不持有学期 ID，以本次操作的目标学期为准。
        sectionTemplateDao.replaceByTerm(termId, ordered.map { it.copy(termId = termId).toEntity() })
    }
}
