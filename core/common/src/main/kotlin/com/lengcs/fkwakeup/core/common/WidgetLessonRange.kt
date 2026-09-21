package com.lengcs.fkwakeup.core.common

import java.time.DayOfWeek
import java.time.LocalDate

/** 每个小组件实例可选择的课程显示范围。 */
enum class WidgetLessonRange {
    TODAY_REMAINING,
    TODAY_ALL,
    TODAY_AND_TOMORROW,
    THIS_WEEK,
}

/** 小组件左侧信息条的展示方式。 */
enum class WidgetLessonBarMode {
    COURSE_COLOR,
    SECTION,
    COLOR_AND_SECTION,
}

/**
 * 组件课程范围的纯计算规则。
 *
 * 日期窗口与“今天已结束课程”的过滤分开，避免“本周”意外丢掉已经过去的日期；
 * 本周范围始终是当前周的周一至周日。
 */
object WidgetLessonRangePicker {

    fun dates(range: WidgetLessonRange, today: LocalDate): List<LocalDate> = when (range) {
        WidgetLessonRange.TODAY_REMAINING,
        WidgetLessonRange.TODAY_ALL,
        -> listOf(today)

        WidgetLessonRange.TODAY_AND_TOMORROW -> listOf(today, today.plusDays(1))
        WidgetLessonRange.THIS_WEEK -> {
            val monday = today.with(DayOfWeek.MONDAY)
            (0L..6L).map(monday::plusDays)
        }
    }

    fun shouldKeep(
        range: WidgetLessonRange,
        date: LocalDate,
        endMinutes: Int,
        today: LocalDate,
        nowMinutes: Int,
    ): Boolean = date in dates(range, today) && (
        range != WidgetLessonRange.TODAY_REMAINING || date != today || endMinutes > nowMinutes
    )
}
