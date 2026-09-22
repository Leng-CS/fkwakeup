package com.lengcs.fkwakeup.core.database

import android.test.AndroidTestCase
import androidx.room.Room
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.model.SectionTemplate
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/** 使用平台测试框架和真实 SQLite，验证外键与事务，不用模拟 DAO 代替数据库。 */
@Suppress("DEPRECATION")
class SectionPersistenceTest : AndroidTestCase() {
    private lateinit var db: AppDatabase
    private lateinit var repository: TermRepository
    private var termId = 0L

    override fun setUp() {
        super.setUp()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TermRepository(db.termDao(), db.sectionTemplateDao())
        termId = runBlocking { repository.createTerm("测试学期", LocalDate.of(2026, 9, 7), 18) }
    }

    override fun tearDown() {
        db.close()
        super.tearDown()
    }

    fun testEditorDraftWithZeroTermIdSavesToSelectedTerm() = runBlocking {
        val otherId = repository.createTerm("另一个学期", LocalDate.of(2027, 2, 1), 18)
        val otherBefore = repository.getSections(otherId)
        repository.replaceSections(termId, listOf(SectionTemplate(0, 1, 480, 527)))
        assertEquals(listOf(SectionTemplate(termId, 1, 480, 527)), repository.getSections(termId))
        assertEquals(otherBefore, repository.getSections(otherId))
    }

    fun testInsertFailureRollsBackDeletionAndPartialInsert() = runBlocking {
        val before = repository.getSections(termId)
        // 第二条插入失败，确保第一条新数据及删除旧数据都回滚。
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_section_insert BEFORE INSERT ON section_templates
            WHEN NEW.section_index = 2 BEGIN SELECT RAISE(ABORT, 'forced failure'); END
        """.trimIndent())
        try {
            repository.replaceSections(termId, listOf(
                SectionTemplate(termId, 1, 480, 520),
                SectionTemplate(termId, 2, 530, 570),
            ))
            fail("应触发插入失败")
        } catch (expected: android.database.sqlite.SQLiteException) {
            assertEquals(before, repository.getSections(termId))
        }
    }

    fun testInvalidTableNeverReplacesExistingRows() = runBlocking {
        val before = repository.getSections(termId)
        val invalidTables = listOf(
            emptyList(),
            listOf(SectionTemplate(0, 2, 480, 520)),
            listOf(SectionTemplate(0, 1, 480, 540), SectionTemplate(0, 2, 530, 570)),
        )
        invalidTables.forEach { rows ->
            try {
                repository.replaceSections(termId, rows)
                fail("无效时间表应被拦截：$rows")
            } catch (expected: IllegalArgumentException) {
                assertEquals(before, repository.getSections(termId))
            }
        }
    }
}
