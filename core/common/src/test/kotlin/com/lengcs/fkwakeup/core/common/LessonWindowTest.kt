package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 「最近课程」窗口与刷新边界的判定（#30/#31）。
 *
 * 第 1-2 节 08:00-08:55；第 3 节 10:00-10:45；第 5 节 14:00-14:45。
 */
class LessonWindowTest {

    private val starts = listOf(8 * 60, 8 * 60 + 55, 10 * 60, 14 * 60)
    private val ends = listOf(8 * 60 + 55, 9 * 60 + 40, 10 * 60 + 45, 14 * 60 + 45)

    // ---- upcomingIndexes ----

    @Test
    fun `早晨未上课时全部按顺序入选`() {
        val idx = LessonWindow.upcomingIndexes(starts, ends, 7 * 60, 4)
        assertEquals(listOf(0, 1, 2, 3), idx)
    }

    @Test
    fun `正在上的课包含在内且排第一`() {
        // 10:20 正在上第 3 节
        val idx = LessonWindow.upcomingIndexes(starts, ends, 10 * 60 + 20, 4)
        assertEquals(listOf(2, 3), idx)
    }

    @Test
    fun `正好在结束时刻算已结束`() {
        // 09:40:00 —— 第 2 节刚结束
        val idx = LessonWindow.upcomingIndexes(starts, ends, 9 * 60 + 40, 4)
        assertEquals(listOf(2, 3), idx)
    }

    @Test
    fun `正好在开始时刻算未结束`() {
        val idx = LessonWindow.upcomingIndexes(starts, ends, 10 * 60, 4)
        assertEquals(listOf(2, 3), idx)
    }

    @Test
    fun `max 截断条数`() {
        val idx = LessonWindow.upcomingIndexes(starts, ends, 7 * 60, 2)
        assertEquals(listOf(0, 1), idx)
    }

    @Test
    fun `全部结束时为空`() {
        val idx = LessonWindow.upcomingIndexes(starts, ends, 15 * 60, 4)
        assertEquals(emptyList<Int>(), idx)
    }

    @Test
    fun `max 为 0 或负数时为空`() {
        assertEquals(emptyList<Int>(), LessonWindow.upcomingIndexes(starts, ends, 7 * 60, 0))
        assertEquals(emptyList<Int>(), LessonWindow.upcomingIndexes(starts, ends, 7 * 60, -1))
    }

    @Test
    fun `空列表时为空且不崩`() {
        assertEquals(emptyList<Int>(), LessonWindow.upcomingIndexes(emptyList(), emptyList(), 600, 2))
    }

    @Test
    fun `starts 比 ends 长时不会越界`() {
        val idx = LessonWindow.upcomingIndexes(listOf(100, 200, 300), listOf(150, 250), 0, 4)
        assertEquals(listOf(0, 1), idx)
    }

    // ---- nextBoundaryMinutes ----

    @Test
    fun `没上课时下一个边界是第一节的开始`() {
        assertEquals(8 * 60, LessonWindow.nextBoundaryMinutes(starts, ends, 7 * 60))
    }

    @Test
    fun `正在上时下一个边界是它的结束时刻`() {
        assertEquals(10 * 60 + 45, LessonWindow.nextBoundaryMinutes(starts, ends, 10 * 60 + 20))
    }

    @Test
    fun `课间时下一个边界是下一节的开始`() {
        assertEquals(10 * 60, LessonWindow.nextBoundaryMinutes(starts, ends, 9 * 60 + 50))
    }

    @Test
    fun `全上完后无边界`() {
        assertNull(LessonWindow.nextBoundaryMinutes(starts, ends, 15 * 60))
    }

    @Test
    fun `最后一节的结束也是边界`() {
        assertEquals(14 * 60 + 45, LessonWindow.nextBoundaryMinutes(starts, ends, 14 * 60))
    }

    @Test
    fun `空列表时无边界`() {
        assertNull(LessonWindow.nextBoundaryMinutes(emptyList(), emptyList(), 600))
    }
}
