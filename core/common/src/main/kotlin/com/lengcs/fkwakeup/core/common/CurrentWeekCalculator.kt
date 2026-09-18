package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.Term
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** 今天相对学期所处的位置 */
enum class TermPhase {
    /** 学期尚未开始 */
    BEFORE,

    /** 学期进行中 */
    IN_TERM,

    /** 学期已结束 */
    AFTER,
}

/**
 * 当前周计算结果。
 *
 * [week] 是 UI 应展示的周次：学期前恒为 1，学期后恒为总周数。
 */
data class WeekStatus(
    val week: Int,
    val phase: TermPhase,
) {
    val isInTerm: Boolean get() = phase == TermPhase.IN_TERM
}

/**
 * 当前周计算（见主开发文档 7.2）。
 */
object CurrentWeekCalculator {

    /** 把任意日期向前对齐到本周周一 */
    fun alignToMonday(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * 未经钳制的周次：学期第一周为 1，之前为 0 或负数，之后大于 [Term.totalWeeks]。
     */
    fun rawWeek(term: Term, today: LocalDate = LocalDate.now()): Int {
        val startMonday = alignToMonday(term.startMonday)
        val todayMonday = alignToMonday(today)
        return ChronoUnit.WEEKS.between(startMonday, todayMonday).toInt() + 1
    }

    /**
     * 带状态的周次结果。
     */
    fun status(term: Term, today: LocalDate = LocalDate.now()): WeekStatus {
        val raw = rawWeek(term, today)
        return when {
            raw < 1 -> WeekStatus(week = 1, phase = TermPhase.BEFORE)
            raw > term.totalWeeks -> WeekStatus(week = term.totalWeeks, phase = TermPhase.AFTER)
            else -> WeekStatus(week = raw, phase = TermPhase.IN_TERM)
        }
    }

    /**
     * 某节课在指定周是否上课。
     */
    fun isActiveInWeek(sessionWeeks: Set<Int>, week: Int): Boolean = week in sessionWeeks
}
