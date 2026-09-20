package com.lengcs.fkwakeup.core.common

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 课块相对「现在」的时间状态（#29）。
 *
 * 周视图据此决定配色：已上完的变灰、正在上的保持全彩并突出。
 */
enum class BlockPhase {
    /** 还没开始 */
    Upcoming,

    /** 正在上 —— 此刻正好处于这个课块的起止时间之内 */
    Ongoing,

    /** 已上完 */
    Past,
}

/**
 * 判定课块的时间状态。纯逻辑，便于单测。
 *
 * 规则是**只按时间**：结束时间早于此刻就算「已上完」，不关心它属于第几周。
 * 所以切到上一周会整周变灰，切到下一周则是全彩。
 */
object BlockPhaseCalculator {

    /**
     * 某一周的周一日期。
     *
     * 学期第一周的周一就是 `Term.startMonday`，第 N 周往后推 N-1 周。
     */
    fun weekMonday(termStartMonday: LocalDate, displayWeek: Int): LocalDate =
        termStartMonday.plusWeeks((displayWeek - 1).coerceAtLeast(0).toLong())

    /**
     * 判定一个课块此刻处于哪个阶段。
     *
     * @param date 这个课块所属的日期（显示周的周一 + 星期偏移）
     * @param startMinutes 该节次的开始时间，单位为「当天第几分钟」（来自节次时间表）
     * @param endMinutes 该节次的结束时间，同上
     *
     * **缺时间信息时一律判 [BlockPhase.Upcoming]** —— 宁可不灰，也不要误灰。
     * 节次表没配全时 `ScheduleLayout` 给不出起止时间，此时把课涂灰会让人以为课已经上完了。
     */
    fun of(
        date: LocalDate,
        startMinutes: Int?,
        endMinutes: Int?,
        now: LocalDateTime,
    ): BlockPhase {
        if (startMinutes == null || endMinutes == null) return BlockPhase.Upcoming

        val midnight = date.atStartOfDay()
        val start = midnight.plusMinutes(startMinutes.toLong())
        val end = midnight.plusMinutes(endMinutes.toLong())

        return when {
            // 正好在开始时刻 → 已经算「正在上」
            now.isBefore(start) -> BlockPhase.Upcoming
            // 正好在结束时刻 → 已经算「上完了」
            now.isBefore(end) -> BlockPhase.Ongoing
            else -> BlockPhase.Past
        }
    }
}
