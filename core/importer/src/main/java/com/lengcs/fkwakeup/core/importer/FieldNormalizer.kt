package com.lengcs.fkwakeup.core.importer

/**
 * L2 字段归一化。AI 输出的字段名和取值写法五花八门，这里统一收口。
 */
object FieldNormalizer {

    /** 被视为「没有值」的占位文本，统一归一为 null */
    private val EMPTY_PLACEHOLDERS = setOf("", "-", "--", "无", "未知", "待定", "暂无", "null", "NULL", "N/A", "n/a")

    private val NAME_KEYS = listOf("name", "courseName", "course", "className", "title", "课程名", "课程", "科目")
    private val TEACHER_KEYS = listOf("teacher", "instructor", "lecturer", "教师", "老师", "任课教师")
    private val LOCATION_KEYS = listOf("location", "classroom", "room", "place", "地点", "教室", "上课地点")
    private val DOW_KEYS = listOf("dayOfWeek", "day", "weekday", "dow", "星期", "周几", "星期几")
    private val START_KEYS = listOf("startSection", "startPeriod", "start", "from", "开始节次", "起始节次")
    private val END_KEYS = listOf("endSection", "endPeriod", "end", "to", "结束节次")
    private val WEEKS_KEYS = listOf("weeks", "weekSpec", "weekList", "weekRanges", "周次", "上课周")

    fun nameKey(obj: Map<String, String>): String? = pick(obj, NAME_KEYS)
    fun teacherKey(obj: Map<String, String>): String? = pick(obj, TEACHER_KEYS)
    fun locationKey(obj: Map<String, String>): String? = pick(obj, LOCATION_KEYS)
    fun dowKey(obj: Map<String, String>): String? = pick(obj, DOW_KEYS)
    fun startKey(obj: Map<String, String>): String? = pick(obj, START_KEYS)
    fun endKey(obj: Map<String, String>): String? = pick(obj, END_KEYS)
    fun weeksKey(obj: Map<String, String>): String? = pick(obj, WEEKS_KEYS)

    private fun pick(obj: Map<String, String>, keys: List<String>): String? {
        for (key in keys) {
            val hit = obj.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
            if (hit != null) return hit.value
        }
        return null
    }

    /** 空占位文本归一为 null，其余去首尾空白 */
    fun normalizeOptionalText(value: String?): String? {
        val trimmed = value?.trim() ?: return null
        return if (EMPTY_PLACEHOLDERS.contains(trimmed)) null else trimmed
    }

    /**
     * 星期归一化。支持数字、中文、英文；0 视为周日。
     * 无法识别时返回 null，交给校验阶段报错。
     */
    fun normalizeDayOfWeek(raw: String?): Int? {
        val text = raw?.trim()?.lowercase() ?: return null
        if (text.isEmpty()) return null

        val asNumber = text.toIntOrNull()
        if (asNumber != null) {
            return when (asNumber) {
                0 -> 7 // 部分模型用 0 表示周日
                in 1..7 -> asNumber
                else -> null
            }
        }

        return when (text) {
            "周一", "星期一", "礼拜一", "mon", "monday" -> 1
            "周二", "星期二", "礼拜二", "tue", "tues", "tuesday" -> 2
            "周三", "星期三", "礼拜三", "wed", "wednesday" -> 3
            "周四", "星期四", "礼拜四", "thu", "thur", "thurs", "thursday" -> 4
            "周五", "星期五", "礼拜五", "fri", "friday" -> 5
            "周六", "星期六", "礼拜六", "sat", "saturday" -> 6
            "周日", "周天", "星期天", "星期日", "礼拜天", "礼拜日", "sun", "sunday" -> 7
            else -> normalizeChineseNumeral(text)
        }
    }

    /**
     * 兜底：先抠数字（"星期3"、"第5天"），再认中文数字。
     *
     * 顺序不能反 —— "第5天" 里的 "天" 会被误当成周日。
     */
    private fun normalizeChineseNumeral(text: String): Int? {
        val digits = Regex("""\d""").find(text)?.value?.toIntOrNull()
        if (digits != null && digits in 1..7) return digits

        CN_NUMERALS.forEach { (cn, value) ->
            if (text.contains(cn)) return value
        }
        return null
    }

    private val CN_NUMERALS = listOf(
        "一" to 1, "二" to 2, "两" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "日" to 7, "天" to 7,
    )

    /**
     * 节次区间归一化。
     *
     * 支持："1-2"、"1,2"、"第1-2节"、"1-2节"、"3"（单节）、"第3节"。
     * @return Pair(start, end)，无法识别返回 null
     */
    fun normalizeSectionRange(raw: String?): Pair<Int, Int>? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty()) return null

        val cleaned = text
            .replace("第", "")
            .replace("节", "")
            .replace("节次", "")
            .trim()

        val parts = cleaned.split(Regex("""[-~,，]""")).map { it.trim() }
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        if (numbers.isEmpty()) return null

        val start = numbers.first()
        val end = numbers.getOrNull(1) ?: start
        if (start < 1 || end < 1) return null
        return start to end
    }

    /** 节次可能是数字或字符串，统一成字符串再交给 normalizeSectionRange */
    fun normalizeSectionValue(raw: String?): Int? = raw?.trim()?.toIntOrNull()

    /** 周次表达式：只做去空白，不解析（解析在校验阶段用 WeekSpecParser） */
    fun normalizeWeekSpec(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }
}
