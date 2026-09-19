package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeekSpecFormatterTest {

    private val totalWeeks = 18

    @Test
    fun `全学期连续周`() {
        assertThat(WeekSpecFormatter.format((1..16).toSet(), totalWeeks)).isEqualTo("1-16")
    }

    @Test
    fun `纯单周生成单标记`() {
        val odds = (1..16).filter { it % 2 == 1 }.toSet()
        assertThat(WeekSpecFormatter.format(odds, totalWeeks)).isEqualTo("1-15单")
    }

    @Test
    fun `纯双周生成双标记`() {
        val evens = (2..16).filter { it % 2 == 0 }.toSet()
        assertThat(WeekSpecFormatter.format(evens, totalWeeks)).isEqualTo("2-16双")
    }

    @Test
    fun `跳周生成多段`() {
        val weeks = ((1..9) + (11..18)).toSet()
        assertThat(WeekSpecFormatter.format(weeks, totalWeeks)).isEqualTo("1-9,11-18")
    }

    @Test
    fun `离散周`() {
        assertThat(WeekSpecFormatter.format(setOf(1, 3, 5, 7), totalWeeks)).isEqualTo("1-7单")
        assertThat(WeekSpecFormatter.format(setOf(1, 4, 9), totalWeeks)).isEqualTo("1,4,9")
    }

    @Test
    fun `单周就一个`() {
        assertThat(WeekSpecFormatter.format(setOf(5), totalWeeks)).isEqualTo("5")
    }

    @Test
    fun `空集合返回空串`() {
        assertThat(WeekSpecFormatter.format(emptySet(), totalWeeks)).isEmpty()
    }

    @Test
    fun `丢弃越界周`() {
        assertThat(WeekSpecFormatter.format(setOf(1, 2, 99), totalWeeks)).isEqualTo("1-2")
    }

    /**
     * 注意：格式化产出的是**规范化表达式**，不保证与原字符串逐字相同。
     * 例如 `1-16单` 会被规范化成 `1-15单`（周集合完全一样，只是上界收紧）。
     * 所以这里断言的是**周集合等价**，而不是字符串相等。
     */
    @Test
    fun `往返一致：解析后再格式化，周集合保持不变`() {
        listOf("1-16", "1-16单", "2-16双", "1-9,11-18", "1,3,5,7", "3", "1-18")
            .forEach { spec ->
                val weeks = WeekSpecParser.parse(spec, totalWeeks)
                val regenerated = WeekSpecFormatter.format(weeks, totalWeeks)
                assertThat(WeekSpecParser.parse(regenerated, totalWeeks)).isEqualTo(weeks)
            }
    }

    @Test
    fun `往返一致：格式化后再解析回到原集合`() {
        listOf(
            (1..16).toSet(),
            (1..16).filter { it % 2 == 1 }.toSet(),
            ((1..9) + (11..18)).toSet(),
            setOf(1, 4, 9),
            setOf(7),
        ).forEach { weeks ->
            val spec = WeekSpecFormatter.format(weeks, totalWeeks)
            assertThat(WeekSpecParser.parse(spec, totalWeeks)).isEqualTo(weeks)
        }
    }

    @Test
    fun `段数统计`() {
        assertThat(WeekSpecFormatter.segmentCount((1..16).toSet(), totalWeeks)).isEqualTo(1)
        assertThat(WeekSpecFormatter.segmentCount(((1..9) + (11..18)).toSet(), totalWeeks)).isEqualTo(2)
        assertThat(WeekSpecFormatter.segmentCount(emptySet(), totalWeeks)).isEqualTo(0)
    }
}
