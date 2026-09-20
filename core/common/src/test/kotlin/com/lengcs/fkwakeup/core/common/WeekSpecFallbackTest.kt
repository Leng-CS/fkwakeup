package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * `WeekSpecFallback` 的单测（对应 Issue #23）。
 *
 * 核心要守住的：**空白周次必须回退成合法表达式**，不能把空串写进库。
 * 空串会让 `WeekSpecParser.parseOrNull` 返回 null，周视图据此 `continue` 跳过该时间段 ——
 * 整门课永久不显示，且没有任何报错，用户完全无从排查。
 */
class WeekSpecFallbackTest {

    private val totalWeeks = 18

    @Test
    fun `null 回退到整学期`() {
        assertEquals("1-18", WeekSpecFallback.orFullTerm(null, totalWeeks))
    }

    @Test
    fun `空串回退到整学期 —— 这是本 issue 的核心`() {
        // 周次点选器「清空」后给到的就是空串，而不是 null
        assertEquals("1-18", WeekSpecFallback.orFullTerm("", totalWeeks))
    }

    @Test
    fun `全空白也回退到整学期`() {
        assertEquals("1-18", WeekSpecFallback.orFullTerm("   ", totalWeeks))
        assertEquals("1-18", WeekSpecFallback.orFullTerm("\t", totalWeeks))
    }

    @Test
    fun `合法表达式原样返回`() {
        assertEquals("1-16", WeekSpecFallback.orFullTerm("1-16", totalWeeks))
        assertEquals("1-16单", WeekSpecFallback.orFullTerm("1-16单", totalWeeks))
        assertEquals("1-9,11-18", WeekSpecFallback.orFullTerm("1-9,11-18", totalWeeks))
    }

    @Test
    fun `首尾空白会被去掉`() {
        assertEquals("1-16", WeekSpecFallback.orFullTerm("  1-16  ", totalWeeks))
    }

    @Test
    fun `返回值一定可以被解析 —— 这是兜底存在的意义`() {
        val inputs = listOf(null, "", "   ", "1-16", "1-16单", "2-16双", "1-9,11-18")
        for (input in inputs) {
            val spec = WeekSpecFallback.orFullTerm(input, totalWeeks)
            assertNotNull("回退结果 $spec（来自 $input）应当可解析", WeekSpecParser.parseOrNull(spec, totalWeeks))
        }
    }

    @Test
    fun `totalWeeks 为 0 或负数时不会生成非法区间`() {
        // 防御性：不能生成 "1-0"，那是无法解析的
        assertEquals("1-1", WeekSpecFallback.orFullTerm(null, 0))
        assertEquals("1-1", WeekSpecFallback.orFullTerm(null, -5))
        assertNotNull(WeekSpecParser.parseOrNull(WeekSpecFallback.orFullTerm(null, 0), 1))
    }

    @Test
    fun `fullTerm 的默认边界`() {
        assertEquals("1-1", WeekSpecFallback.fullTerm(1))
        assertEquals("1-30", WeekSpecFallback.fullTerm(30))
    }
}
