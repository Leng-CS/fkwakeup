package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 验证 #14/#15/#16 依赖的两个核心转换在边界上的行为。
 *
 * 这些不是新增功能测试，而是把「滚轮 + 点选」改造后必须仍然成立的存储契约
 * 再钉一遍：UI 换了输入方式，落库的 weekSpec 必须还是合法表达式。
 */
class WeekSpecFormatterEdgeTest {

    private val totalWeeks = 18

    @Test
    fun `空集合返回空串且可被解析为 null`() {
        val spec = WeekSpecFormatter.format(emptySet(), totalWeeks)
        assertEquals("", spec)
        // 空串不是合法表达式 —— 保存逻辑必须先拦住「未选周次」的情况
        assertTrue(WeekSpecParser.parseOrNull(spec, totalWeeks) == null)
    }

    @Test
    fun `全选生成 1-18`() {
        assertEquals("1-18", WeekSpecFormatter.format((1..18).toSet(), totalWeeks))
    }

    @Test
    fun `单周集合往返后周集合等价`() {
        // {1,3..15} 会被规范化为 1-15单，字符串不同但周集合必须等价
        val odd = (1..15).filter { it % 2 == 1 }.toSet()
        val spec = WeekSpecFormatter.format(odd, totalWeeks)
        val back = WeekSpecParser.parseOrNull(spec, totalWeeks)
        assertEquals(odd, back)
    }

    @Test
    fun `采集到的真实用例全覆盖`() {
        val cases = listOf(
            setOf(1) to 18,
            (1..16).toSet() to 18,
            (1..16).filter { it % 2 == 0 }.toSet() to 18,
            ((1..9) + (11..18)).toSet() to 18,
            ((1..9) + (11..18)).filter { it % 2 == 1 }.toSet() to 18,
        )
        for ((weeks, total) in cases) {
            val spec = WeekSpecFormatter.format(weeks, total)
            val back = WeekSpecParser.parseOrNull(spec, total)
            assertEquals("周集合往返不等价: weeks=$weeks spec=$spec", weeks, back)
        }
    }

    @Test
    fun `所有周集合的唯一元素都在范围内`() {
        // 防止把 0 或 totalWeeks+1 这类越界值写进表达式
        val weeks = setOf(1, 18)
        val spec = WeekSpecFormatter.format(weeks, totalWeeks)
        assertEquals("1,18", spec)
        assertEquals(weeks, WeekSpecParser.parseOrNull(spec, totalWeeks))
    }
}
