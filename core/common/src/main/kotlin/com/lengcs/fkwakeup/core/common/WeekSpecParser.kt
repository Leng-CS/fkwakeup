package com.lengcs.fkwakeup.core.common

/**
 * 周次片段解析失败。携带出错的片段，便于定位到第 N 条记录。
 */
class WeekSpecParseException(
    val segment: String,
    message: String,
) : IllegalArgumentException(message)

/**
 * 周次表达式解析器。
 *
 * 语法（见主开发文档 4.3）：
 * ```
 * weekSpec := segment ("," segment)*
 * segment  := range | single
 * range    := INT "-" INT [pattern]
 * single   := INT [pattern]
 * pattern  := "单" | "双" | "odd" | "even"
 * ```
 */
object WeekSpecParser {

    private val SEGMENT_REGEX = Regex("""^(\d+)(?:-(\d+))?(单|双|odd|even)?$""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    /**
     * @param spec 周次表达式，如 "1-16"、"2-16双"、"1-9,11-18"
     * @param totalWeeks 学期总周数，超出部分会被钳制
     * @throws WeekSpecParseException 表达式非法或解析结果为空
     */
    fun parse(spec: String, totalWeeks: Int): Set<Int> {
        require(totalWeeks >= 1) { "totalWeeks 必须 >= 1，实际为 $totalWeeks" }

        val normalized = WHITESPACE_REGEX.replace(spec, "")
        if (normalized.isEmpty()) {
            throw WeekSpecParseException(spec, "周次表达式为空")
        }

        val weeks = sortedSetOf<Int>()
        for (segment in normalized.split(",")) {
            if (segment.isEmpty()) {
                throw WeekSpecParseException(spec, "存在空的周次片段：\"$spec\"")
            }
            val match = SEGMENT_REGEX.matchEntire(segment)
                ?: throw WeekSpecParseException(segment, "周次片段 \"$segment\" 无法识别")

            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: start
            val pattern = match.groupValues[3]

            if (start < 1) {
                throw WeekSpecParseException(segment, "起始周必须 >= 1，实际为 $start")
            }
            if (end < start) {
                throw WeekSpecParseException(segment, "结束周($end) 小于起始周($start)")
            }
            if (start > totalWeeks) {
                throw WeekSpecParseException(
                    segment,
                    "起始周($start) 超出学期总周数($totalWeeks)",
                )
            }

            // 超出学期总周数的部分钳制，不报错
            val clampedEnd = minOf(end, totalWeeks)
            for (week in start..clampedEnd) {
                if (matchesPattern(week, pattern)) weeks.add(week)
            }
        }

        if (weeks.isEmpty()) {
            throw WeekSpecParseException(spec, "解析结果为空：\"$spec\"")
        }
        return weeks
    }

    /**
     * 宽松解析：失败时返回 null，供导入预览页逐条标注错误，避免整体失败。
     */
    fun parseOrNull(spec: String, totalWeeks: Int): Set<Int>? =
        runCatching { parse(spec, totalWeeks) }.getOrNull()

    private fun matchesPattern(week: Int, pattern: String): Boolean =
        when (pattern) {
            "" -> true
            "单", "odd" -> week % 2 == 1
            "双", "even" -> week % 2 == 0
            else -> true
        }
}
