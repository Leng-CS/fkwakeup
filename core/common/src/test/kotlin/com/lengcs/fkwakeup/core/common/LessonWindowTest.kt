package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LessonWindowTest {

    private val starts = listOf(8 * 60, 10 * 60, 14 * 60)
    private val ends = listOf(8 * 60 + 45, 10 * 60 + 45, 14 * 60 + 45)

    @Test
    fun `今日未结束课程包含正在上的和之后的课程`() {
        assertThat(LessonWindow.remainingIndexes(starts, ends, 10 * 60 + 20))
            .containsExactly(1, 2)
            .inOrder()
    }

    @Test
    fun `结束时刻等于当前时刻的课程不再显示`() {
        assertThat(LessonWindow.remainingIndexes(starts, ends, 10 * 60 + 45))
            .containsExactly(2)
    }

    @Test
    fun `下一个边界在课间取下一节开始时刻`() {
        assertThat(LessonWindow.nextBoundaryMinutes(starts, ends, 9 * 60))
            .isEqualTo(10 * 60)
    }

    @Test
    fun `下一个边界在上课中取本节结束时刻`() {
        assertThat(LessonWindow.nextBoundaryMinutes(starts, ends, 10 * 60 + 20))
            .isEqualTo(10 * 60 + 45)
    }

    @Test
    fun `当天没有剩余课程时无边界`() {
        assertThat(LessonWindow.nextBoundaryMinutes(starts, ends, 15 * 60)).isNull()
    }

    @Test
    fun `起止列表长度不同时忽略缺失结束时刻`() {
        assertThat(LessonWindow.remainingIndexes(listOf(100, 200), listOf(150), 0))
            .containsExactly(0)
    }
}
