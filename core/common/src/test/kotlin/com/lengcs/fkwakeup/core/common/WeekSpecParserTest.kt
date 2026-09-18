package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class WeekSpecParserTest {

    private val totalWeeks = 18

    // ---- 主开发文档 4.3 表格：合法表达式 ----

    @Test
    fun `连续周 1-16`() {
        assertThat(WeekSpecParser.parse("1-16", totalWeeks)).isEqualTo((1..16).toSet())
    }

    @Test
    fun `单周 1-16单`() {
        assertThat(WeekSpecParser.parse("1-16单", totalWeeks))
            .isEqualTo(setOf(1, 3, 5, 7, 9, 11, 13, 15))
    }

    @Test
    fun `双周 2-16双`() {
        assertThat(WeekSpecParser.parse("2-16双", totalWeeks))
            .isEqualTo(setOf(2, 4, 6, 8, 10, 12, 14, 16))
    }

    @Test
    fun `跳过某周 1-9 和 11-18`() {
        assertThat(WeekSpecParser.parse("1-9,11-18", totalWeeks))
            .isEqualTo(((1..9) + (11..18)).toSet())
    }

    @Test
    fun `离散周 1,3,5,7`() {
        assertThat(WeekSpecParser.parse("1,3,5,7", totalWeeks)).isEqualTo(setOf(1, 3, 5, 7))
    }

    @Test
    fun `混合表达式 1-8 与 10-16双`() {
        assertThat(WeekSpecParser.parse("1-8,10-16双", totalWeeks))
            .isEqualTo((1..8).toSet() + setOf(10, 12, 14, 16))
    }

    @Test
    fun `单节写法 3 等价于 3-3`() {
        assertThat(WeekSpecParser.parse("3", totalWeeks)).isEqualTo(setOf(3))
        assertThat(WeekSpecParser.parse("3", totalWeeks))
            .isEqualTo(WeekSpecParser.parse("3-3", totalWeeks))
    }

    @Test
    fun `英文 odd 与 even 等价`() {
        assertThat(WeekSpecParser.parse("1-9odd", totalWeeks))
            .isEqualTo(WeekSpecParser.parse("1-9单", totalWeeks))
        assertThat(WeekSpecParser.parse("2-10even", totalWeeks))
            .isEqualTo(WeekSpecParser.parse("2-10双", totalWeeks))
    }

    @Test
    fun `含空白字符可容错`() {
        assertThat(WeekSpecParser.parse(" 1 - 9 , 11 - 16 ", totalWeeks))
            .isEqualTo(((1..9) + (11..16)).toSet())
    }

    // ---- 越界钳制 ----

    @Test
    fun `超出学期总周数时钳制到 totalWeeks`() {
        val result = WeekSpecParser.parse("1-30", totalWeeks)
        assertThat(result).isEqualTo((1..18).toSet())
        assertThat(result).doesNotContain(19)
    }

    // ---- 非法输入 ----

    @Test
    fun `非数字 abc 抛异常`() {
        val e = assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("abc", totalWeeks)
        }
        assertThat(e.segment).isEqualTo("abc")
    }

    @Test
    fun `结束周小于起始周 5-2 抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("5-2", totalWeeks)
        }
    }

    @Test
    fun `空字符串抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("", totalWeeks)
        }
    }

    @Test
    fun `半截范围 1- 抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("1-", totalWeeks)
        }
    }

    @Test
    fun `起始周超出学期总周数抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("20-22", totalWeeks)
        }
    }

    @Test
    fun `第 0 周抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("0-4", totalWeeks)
        }
    }

    @Test
    fun `中文周字 1-16周 抛异常`() {
        assertThrows(WeekSpecParseException::class.java) {
            WeekSpecParser.parse("1-16周", totalWeeks)
        }
    }

    // ---- 宽松解析 ----

    @Test
    fun `parseOrNull 非法时返回 null`() {
        assertThat(WeekSpecParser.parseOrNull("1-16周（单）", totalWeeks)).isNull()
        assertThat(WeekSpecParser.parseOrNull("1-16", totalWeeks)).isNotNull()
    }
}
