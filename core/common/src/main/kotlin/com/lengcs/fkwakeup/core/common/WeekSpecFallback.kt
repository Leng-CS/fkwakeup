package com.lengcs.fkwakeup.core.common

/**
 * 周次表达式的兜底（对应 Issue #23）。
 *
 * **为什么需要它**：导入落库时曾这样写
 * ```
 * weekSpec = draft.weeks ?: "1-$totalWeeks"
 * ```
 * 只按 `null` 兜底。但周次点选器有「清空」操作，清空后 `weeks` 是**空字符串**而不是 `null`，
 * 兜底不生效 → 库里写入 `week_spec = ""` → `WeekSpecParser.parseOrNull("")` 返回 null →
 * 该时间段在周视图里被 `?: continue` 静默跳过 → **整门课永久不显示，且没有任何报错**。
 *
 * 所以判空必须覆盖 null 与空白两种情况。
 */
object WeekSpecFallback {

    /** 整学期的表达式，如 `1-18` */
    fun fullTerm(totalWeeks: Int): String = "1-${totalWeeks.coerceAtLeast(1)}"

    /**
     * 取可用的周次表达式：空白（null / 空串 / 全空格）时回退到整学期。
     *
     * @return 一定是可以被 [WeekSpecParser] 解析的合法表达式
     */
    fun orFullTerm(weeks: String?, totalWeeks: Int): String {
        val trimmed = weeks?.trim()
        return if (trimmed.isNullOrEmpty()) fullTerm(totalWeeks) else trimmed
    }
}
