package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import org.junit.Test

class ScheduleLayoutTest {

    private val totalWeeks = 18

    private fun course(name: String, sessions: List<CourseSession>) =
        CourseWithSessions(
            course = Course(id = 1L, termId = 1L, name = name),
            sessions = sessions,
        )

    private fun session(
        day: Int,
        start: Int,
        end: Int,
        weeks: String = "1-16",
        courseId: Long = 1L,
    ) = CourseSession(
        id = 0L,
        courseId = courseId,
        dayOfWeek = day,
        startSection = start,
        endSection = end,
        weekSpec = weeks,
    )

    // ---- 基本定位 ----

    @Test
    fun `周一第 1-2 节映射到 dayIndex 0 startRow 0 rowSpan 2`() {
        val blocks = ScheduleLayout.build(
            listOf(course("高等数学", listOf(session(day = 1, start = 1, end = 2)))),
            week = 1,
            totalWeeks,
        )
        val b = blocks.single()
        assertThat(b.dayIndex).isEqualTo(0)
        assertThat(b.startRow).isEqualTo(0)
        assertThat(b.rowSpan).isEqualTo(2)
        assertThat(b.columnCount).isEqualTo(1)
    }

    @Test
    fun `跨 3 节的课程 rowSpan 为 3`() {
        val blocks = ScheduleLayout.build(
            listOf(course("数据结构", listOf(session(day = 2, start = 5, end = 7)))),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks.single().rowSpan).isEqualTo(3)
        assertThat(blocks.single().startRow).isEqualTo(4)
    }

    // ---- 周次过滤 ----

    @Test
    fun `不在本周上课的课程被过滤`() {
        val blocks = ScheduleLayout.build(
            listOf(course("英语", listOf(session(day = 1, start = 1, end = 2, weeks = "1-9")))),
            week = 12,
            totalWeeks,
        )
        assertThat(blocks).isEmpty()
    }

    @Test
    fun `单周课程只在奇数周出现`() {
        val courses = listOf(course("英语", listOf(session(day = 1, start = 1, end = 2, weeks = "1-16单"))))
        assertThat(ScheduleLayout.build(courses, week = 3, totalWeeks)).hasSize(1)
        assertThat(ScheduleLayout.build(courses, week = 4, totalWeeks)).isEmpty()
    }

    @Test
    fun `双周课程只在偶数周出现`() {
        val courses = listOf(course("实验", listOf(session(day = 5, start = 5, end = 8, weeks = "2-16双"))))
        assertThat(ScheduleLayout.build(courses, week = 2, totalWeeks)).hasSize(1)
        assertThat(ScheduleLayout.build(courses, week = 3, totalWeeks)).isEmpty()
    }

    @Test
    fun `跳周表达式中间那周不上课`() {
        val courses = listOf(course("数据结构", listOf(session(day = 2, start = 5, end = 7, weeks = "1-9,11-18"))))
        assertThat(ScheduleLayout.build(courses, week = 9, totalWeeks)).hasSize(1)
        assertThat(ScheduleLayout.build(courses, week = 10, totalWeeks)).isEmpty()
        assertThat(ScheduleLayout.build(courses, week = 11, totalWeeks)).hasSize(1)
    }

    // ---- 冲突并排 ----

    @Test
    fun `同一节次两门课并排各占一半`() {
        val blocks = ScheduleLayout.build(
            listOf(
                course("A", listOf(session(day = 1, start = 1, end = 2, courseId = 1L))),
                course("B", listOf(session(day = 1, start = 1, end = 2, courseId = 2L))),
            ),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks).hasSize(2)
        assertThat(blocks.map { it.columnCount }).containsExactly(2, 2)
        assertThat(blocks.map { it.columnIndex }).containsExactly(0, 1)
    }

    @Test
    fun `跨节次课程与它内部某节的短课程并排`() {
        // A 占 1-3 节，B 只占第 2 节 —— 两者有交集，应并排
        val blocks = ScheduleLayout.build(
            listOf(
                course("A", listOf(session(day = 3, start = 1, end = 3, courseId = 1L))),
                course("B", listOf(session(day = 3, start = 2, end = 2, courseId = 2L))),
            ),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks).hasSize(2)
        assertThat(blocks.map { it.columnCount }).containsExactly(2, 2)
        // 跨节次的 A 仍然保持 rowSpan 3
        assertThat(blocks.first { it.course.name == "A" }.rowSpan).isEqualTo(3)
        assertThat(blocks.first { it.course.name == "B" }.rowSpan).isEqualTo(1)
    }

    @Test
    fun `时间不重叠的课程不并排`() {
        val blocks = ScheduleLayout.build(
            listOf(
                course("A", listOf(session(day = 1, start = 1, end = 2, courseId = 1L))),
                course("B", listOf(session(day = 1, start = 3, end = 4, courseId = 2L))),
            ),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks.map { it.columnCount }).containsExactly(1, 1)
    }

    @Test
    fun `不同天的课程互不影响`() {
        val blocks = ScheduleLayout.build(
            listOf(
                course("A", listOf(session(day = 1, start = 1, end = 2, courseId = 1L))),
                course("B", listOf(session(day = 5, start = 1, end = 2, courseId = 2L))),
            ),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks.map { it.columnCount }).containsExactly(1, 1)
        assertThat(blocks.map { it.dayIndex }).containsExactly(0, 4)
    }

    @Test
    fun `非法周次表达式被跳过而不炸整页`() {
        val blocks = ScheduleLayout.build(
            listOf(
                course("坏数据", listOf(session(day = 1, start = 1, end = 2, weeks = "1-16周（单）"))),
                course("正常课", listOf(session(day = 1, start = 3, end = 4, weeks = "1-16"))),
            ),
            week = 1,
            totalWeeks,
        )
        assertThat(blocks).hasSize(1)
        assertThat(blocks.single().course.name).isEqualTo("正常课")
    }

    // ---- 当前节次 ----

    @Test
    fun `当前节次按时间落在哪一节计算`() {
        val sections = listOf(
            SectionTemplate(1L, 1, 480, 525),   // 08:00-08:45
            SectionTemplate(1L, 2, 535, 580),   // 08:55-09:40
        )
        assertThat(ScheduleLayout.currentSectionIndex(sections, 500)).isEqualTo(1)
        assertThat(ScheduleLayout.currentSectionIndex(sections, 550)).isEqualTo(2)
        // 课间
        assertThat(ScheduleLayout.currentSectionIndex(sections, 530)).isNull()
    }
}
