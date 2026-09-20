package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 课块时间状态的判定（#29）。
 *
 * 规则：**只按时间** —— 结束时间早于此刻就是「已上完」，与它属于第几周无关。
 */
class BlockPhaseTest {

    private val monday = LocalDate.of(2026, 9, 7)
    private val wednesday = LocalDate.of(2026, 9, 9)

    // 第 3 节：10:00 – 10:45
    private val startMin = 10 * 60
    private val endMin = 10 * 60 + 45

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 9, 9, hour, minute)

    @Test
    fun `缺开始时间时判 Upcoming_宁可不灰也不要误灰`() {
        assertEquals(BlockPhase.Upcoming, BlockPhaseCalculator.of(wednesday, null, endMin, at(23, 0)))
    }

    @Test
    fun `缺结束时间时判 Upcoming`() {
        assertEquals(BlockPhase.Upcoming, BlockPhaseCalculator.of(wednesday, startMin, null, at(23, 0)))
    }

    @Test
    fun `早于开始时刻是 Upcoming`() {
        assertEquals(BlockPhase.Upcoming, BlockPhaseCalculator.of(wednesday, startMin, endMin, at(9, 59)))
    }

    @Test
    fun `正好在开始时刻算正在上`() {
        assertEquals(BlockPhase.Ongoing, BlockPhaseCalculator.of(wednesday, startMin, endMin, at(10, 0)))
    }

    @Test
    fun `处于起止之间算正在上`() {
        assertEquals(BlockPhase.Ongoing, BlockPhaseCalculator.of(wednesday, startMin, endMin, at(10, 30)))
    }

    @Test
    fun `正好在结束时刻算已上完`() {
        assertEquals(BlockPhase.Past, BlockPhaseCalculator.of(wednesday, startMin, endMin, at(10, 45)))
    }

    @Test
    fun `晚于结束时刻是 Past`() {
        assertEquals(BlockPhase.Past, BlockPhaseCalculator.of(wednesday, startMin, endMin, at(10, 46)))
    }

    @Test
    fun `前一天的课在第二天早上一定是 Past`() {
        val tuesday = LocalDate.of(2026, 9, 8)
        val wedMorning = LocalDateTime.of(2026, 9, 9, 8, 0)
        assertEquals(BlockPhase.Past, BlockPhaseCalculator.of(tuesday, startMin, endMin, wedMorning))
    }

    @Test
    fun `同一天更晚的课是 Upcoming`() {
        // 第 9 节：19:00 – 19:45
        assertEquals(BlockPhase.Upcoming, BlockPhaseCalculator.of(wednesday, 19 * 60, 19 * 60 + 45, at(8, 0)))
    }

    @Test
    fun `开始与结束相同（异常数据）不会崩且按时间落在哪边算`() {
        assertEquals(BlockPhase.Upcoming, BlockPhaseCalculator.of(wednesday, startMin, startMin, at(9, 0)))
        assertEquals(BlockPhase.Past, BlockPhaseCalculator.of(wednesday, startMin, startMin, at(11, 0)))
    }

    // ---- 显示周的周一 ----

    @Test
    fun `第一周的周一就是学期起始日`() {
        assertEquals(monday, BlockPhaseCalculator.weekMonday(monday, 1))
    }

    @Test
    fun `第三周的周一往后推两周`() {
        assertEquals(LocalDate.of(2026, 9, 21), BlockPhaseCalculator.weekMonday(monday, 3))
    }

    @Test
    fun `周次小于 1 时按第一周处理`() {
        assertEquals(monday, BlockPhaseCalculator.weekMonday(monday, 0))
        assertEquals(monday, BlockPhaseCalculator.weekMonday(monday, -3))
    }

    /**
     * 这条记录了「上周的课会整周变灰」这个**有意为之**的行为：
     * 判定只看时间，所以上一周的每个课块在当下都已经是 Past。
     */
    @Test
    fun `上一周的课在当下全部是 Past`() {
        val lastWeekMonday = BlockPhaseCalculator.weekMonday(monday, 1)   // 9-07 那周
        val now = LocalDateTime.of(2026, 9, 15, 9, 0)                     // 下一周的周二
        val phases = (0..6).map { offset ->
            BlockPhaseCalculator.of(lastWeekMonday.plusDays(offset.toLong()), startMin, endMin, now)
        }
        assertEquals(List(7) { BlockPhase.Past }, phases)
    }
}
