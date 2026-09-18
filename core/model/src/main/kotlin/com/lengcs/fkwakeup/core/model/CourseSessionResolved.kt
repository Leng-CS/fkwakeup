package com.lengcs.fkwakeup.core.model

/**
 * [CourseSession] 加上解析后的周集合，读取时派生，不落库。
 */
data class CourseSessionResolved(
    val session: CourseSession,
    val weeks: Set<Int>,
) {
    operator fun contains(week: Int): Boolean = week in weeks
}
