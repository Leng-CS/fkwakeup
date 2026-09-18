package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.Term
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CurrentWeekCalculatorTest {

    /** 用一个确定是周一的日期做基准，避免依赖具体日历 */
    private val startMonday: LocalDate =
        CurrentWeekCalculator.alignToMonday(LocalDate.of(2026, 9, 10))

    private val term = Term(
        id = 1L,
        name = "2026-2027 秋季学期",
        startMonday = startMonday,
        totalWeeks = 18,
        isArchived = false,
        createdAt = java.time.Instant.EPOCH,
    )

    @Test
    fun `起始周一是第 1 周`() {
        assertThat(CurrentWeekCalculator.status(term, startMonday).week).isEqualTo(1)
    }

    @Test
    fun `同一周内的周日仍算第 1 周`() {
        val sunday = startMonday.plusDays(6)
        assertThat(CurrentWeekCalculator.status(term, sunday).week).isEqualTo(1)
    }

    @Test
    fun `第二周的周一是第 2 周`() {
        assertThat(CurrentWeekCalculator.status(term, startMonday.plusWeeks(1)).week).isEqualTo(2)
    }

    @Test
    fun `学期最后一周仍在学期内`() {
        val lastWeekMonday = startMonday.plusWeeks((term.totalWeeks - 1).toLong())
        val status = CurrentWeekCalculator.status(term, lastWeekMonday)
        assertThat(status.week).isEqualTo(18)
        assertThat(status.phase).isEqualTo(TermPhase.IN_TERM)
    }

    @Test
    fun `学期开始前返回 BEFORE 且展示第 1 周`() {
        val before = startMonday.minusWeeks(1)
        val status = CurrentWeekCalculator.status(term, before)
        assertThat(status.phase).isEqualTo(TermPhase.BEFORE)
        assertThat(status.week).isEqualTo(1)
        assertThat(status.isInTerm).isFalse()
    }

    @Test
    fun `学期结束后返回 AFTER 且展示最后一周`() {
        val after = startMonday.plusWeeks(term.totalWeeks.toLong())
        val status = CurrentWeekCalculator.status(term, after)
        assertThat(status.phase).isEqualTo(TermPhase.AFTER)
        assertThat(status.week).isEqualTo(term.totalWeeks)
    }

    @Test
    fun `跨年计算正确`() {
        // 让学期末跨越 2026 -> 2027
        val lateStart = CurrentWeekCalculator.alignToMonday(LocalDate.of(2026, 12, 20))
        val crossYearTerm = term.copy(startMonday = lateStart)
        val inJanuary = lateStart.plusWeeks(3)

        assertThat(inJanuary.year).isEqualTo(2027)
        assertThat(CurrentWeekCalculator.status(crossYearTerm, inJanuary).week).isEqualTo(4)
        assertThat(ChronoUnit.WEEKS.between(lateStart, inJanuary)).isEqualTo(3)
    }

    @Test
    fun `学期起始日非周一时会被向前对齐`() {
        val wednesday = startMonday.plusDays(2)
        val nonMondayTerm = term.copy(startMonday = wednesday)
        assertThat(CurrentWeekCalculator.rawWeek(nonMondayTerm, wednesday)).isEqualTo(1)
        assertThat(CurrentWeekCalculator.alignToMonday(wednesday)).isEqualTo(startMonday)
    }

    @Test
    fun `isActiveInWeek 判断课程是否在某周上课`() {
        val weeks = WeekSpecParser.parse("2-16双", term.totalWeeks)
        assertThat(CurrentWeekCalculator.isActiveInWeek(weeks, 2)).isTrue()
        assertThat(CurrentWeekCalculator.isActiveInWeek(weeks, 3)).isFalse()
    }
}
