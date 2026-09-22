package com.lengcs.fkwakeup.core.database.repository

import com.lengcs.fkwakeup.core.database.dao.SectionTemplateDao
import com.lengcs.fkwakeup.core.database.dao.TermDao
import com.lengcs.fkwakeup.core.database.entity.SectionTemplateEntity
import com.lengcs.fkwakeup.core.model.SectionTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class SectionReplacementTest {
    private val dao = RecordingSections()
    private val unusedTerms = Proxy.newProxyInstance(TermDao::class.java.classLoader,
        arrayOf(TermDao::class.java)) { _, _, _ -> error("节次替换不需要读取学期 DAO") } as TermDao
    private val repository = TermRepository(unusedTerms, dao)

    @Test
    fun `draft IDs are replaced by target term and rows ordered`() = runBlocking {
        repository.replaceSections(7, listOf(SectionTemplate(99, 2, 540, 580), SectionTemplate(0, 1, 480, 520)))
        assertEquals(7L, dao.target)
        assertEquals(listOf(SectionTemplateEntity(7, 1, 480, 520), SectionTemplateEntity(7, 2, 540, 580)), dao.rows)
    }

    @Test
    fun `invalid inputs never reach database replacement`() = runBlocking {
        val invalid = listOf(
            emptyList(),
            listOf(SectionTemplate(0, 2, 480, 520)),
            listOf(SectionTemplate(0, 1, 480, 520), SectionTemplate(0, 1, 540, 580)),
            listOf(SectionTemplate(0, 1, 480, 550), SectionTemplate(0, 2, 540, 580)),
        )
        invalid.forEach { rows ->
            try {
                repository.replaceSections(7, rows)
                fail("应拒绝非法时间表")
            } catch (expected: IllegalArgumentException) {
                assertNull(dao.target)
            }
        }
    }

    @Test
    fun `zero target term is rejected before database write`() = runBlocking {
        try {
            repository.replaceSections(0, listOf(SectionTemplate(0, 1, 480, 520)))
            fail("应拒绝无效学期")
        } catch (expected: IllegalArgumentException) {
            assertNull(dao.target)
        }
    }

    private class RecordingSections : SectionTemplateDao {
        var target: Long? = null
        var rows = emptyList<SectionTemplateEntity>()
        override suspend fun replaceByTerm(termId: Long, templates: List<SectionTemplateEntity>) {
            target = termId
            rows = templates
        }
        override suspend fun insertAll(templates: List<SectionTemplateEntity>) { error("必须走原子替换") }
        override suspend fun deleteByTerm(termId: Long) { error("禁止单独删除") }
        override fun observeByTerm(termId: Long): Flow<List<SectionTemplateEntity>> = flowOf(rows)
        override suspend fun getByTerm(termId: Long) = rows
        override suspend fun countByTerm(termId: Long) = rows.size
    }
}
