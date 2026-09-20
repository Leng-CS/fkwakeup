package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 守住「当前学期」的选择规则。
 *
 * 背景（CHANGELOG #26）：`CourseEditViewModel` 曾经直接拿 `observeTerms().first()`
 * 当作当前学期，而那是「开学日期最晚的学期」。多学期时，新建课程会被写进另一个学期
 * （管理页看不到），编辑已有课程更是会把课程的 `term_id` 改掉、使课程从当前学期消失。
 *
 * 这些用例把「配置优先、兜底靠后、不要拿兜底结果冒充当前学期」钉住。
 */
class CurrentTermPickTest {

    @Test
    fun `配置有效时用配置的学期，忽略兜底列表`() {
        assertEquals(7L, CurrentTermPick.pick(configuredTermId = 7L, fallbackIds = listOf(2L, 5L, 1L)))
    }

    @Test
    fun `配置有效时即使它不在兜底列表里也照用`() {
        // 归档学期不会出现在 observeActive() 里，但它仍可能是当前学期
        assertEquals(9L, CurrentTermPick.pick(configuredTermId = 9L, fallbackIds = listOf(2L, 5L)))
    }

    @Test
    fun `配置为 0 时退回兜底列表第一个`() {
        assertEquals(2L, CurrentTermPick.pick(configuredTermId = 0L, fallbackIds = listOf(2L, 5L, 1L)))
    }

    @Test
    fun `配置为 -1（无当前学期）时退回兜底列表第一个`() {
        assertEquals(2L, CurrentTermPick.pick(configuredTermId = -1L, fallbackIds = listOf(2L, 5L, 1L)))
    }

    @Test
    fun `配置与兜底都为空时返回 null`() {
        assertNull(CurrentTermPick.pick(configuredTermId = 0L, fallbackIds = emptyList()))
    }

    @Test
    fun `负数配置且无兜底时返回 null`() {
        assertNull(CurrentTermPick.pick(configuredTermId = -1L, fallbackIds = emptyList()))
    }

    /**
     * 这条用例记录了「兜底结果 ≠ 当前学期」这个事实：
     * 兜底取的是列表第一个（开学日期最晚），它可能和用户配置的学期完全不同。
     */
    @Test
    fun `兜底结果与配置学期可能不同_所以不能拿兜底冒充当前学期`() {
        val configured = 5L          // 用户当前学期（开学 09-07）
        val activeByStartDesc = listOf(2L, 4L, 1L, 5L, 3L)  // 兜底列表：2 号开学最晚

        assertEquals(configured, CurrentTermPick.pick(configured, activeByStartDesc))
        assertEquals(2L, CurrentTermPick.pick(0L, activeByStartDesc))
        // 两者不同 —— 这正是 #26 的成因
        assertEquals(
            false,
            CurrentTermPick.pick(0L, activeByStartDesc) == configured,
        )
    }
}
