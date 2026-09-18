package com.lengcs.fkwakeup.core.importer

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import org.junit.Test

class CourseMergerTest {

    private fun draft(
        index: Int,
        name: String,
        teacher: String? = "张伟",
        day: Int = 1,
        start: Int = 1,
    ) = SessionDraft(
        index = index,
        name = name,
        teacher = teacher,
        location = null,
        dayOfWeek = day,
        startSection = start,
        endSection = start,
        weeks = "1-16",
        note = null,
    )

    @Test
    fun `同名同教师归为一门课`() {
        val merged = CourseMerger.merge(
            listOf(draft(1, "高等数学A", day = 1), draft(2, "高等数学A", day = 3)),
        )
        assertThat(merged).hasSize(1)
        assertThat(merged.single().sessions).hasSize(2)
    }

    @Test
    fun `同一门课的多个时间段按周几和节次排序`() {
        val merged = CourseMerger.merge(
            listOf(
                draft(1, "A", day = 5, start = 3),
                draft(2, "A", day = 1, start = 1),
                draft(3, "A", day = 1, start = 5),
            ),
        )
        val days = merged.single().sessions.map { it.dayOfWeek }
        assertThat(days).containsExactly(1, 1, 5).inOrder()
    }

    @Test
    fun `教师不同则视为两门课`() {
        val merged = CourseMerger.merge(
            listOf(draft(1, "英语", teacher = "李娜"), draft(2, "英语", teacher = "王强")),
        )
        assertThat(merged).hasSize(2)
    }

    @Test
    fun `大小写与空格不敏感`() {
        val merged = CourseMerger.merge(
            listOf(
                draft(1, "Math", teacher = "Smith"),
                draft(2, "  math ", teacher = "SMITH"),
            ),
        )
        assertThat(merged).hasSize(1)
    }

    @Test
    fun `匿名记录不参与归并`() {
        val merged = CourseMerger.merge(listOf(draft(1, ""), draft(2, "   ")))
        assertThat(merged).isEmpty()
    }

    @Test
    fun `归并键由名称和教师组成`() {
        assertThat(CourseMerger.mergeKey("数学", "张伟"))
            .isEqualTo(CourseMerger.mergeKey(" 数学 ", "张伟"))
        assertThat(CourseMerger.mergeKey("数学", "张伟"))
            .isNotEqualTo(CourseMerger.mergeKey("数学", "李娜"))
    }
}
