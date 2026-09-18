package com.lengcs.fkwakeup.core.model

/**
 * 上课时间段：周几 + 第几节到第几节 + 周次 + 地点。
 *
 * [weekSpec] 保存原始表达式（如 "1-16"、"2-16双"、"1-9,11-18"），
 * 解析后的周集合由 [CourseSessionResolved] 承载，读取时计算、不落库。
 */
data class CourseSession(
    val id: Long = 0L,
    val courseId: Long,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val weekSpec: String,
    val location: String? = null,
    val note: String? = null,
) {
    init {
        require(dayOfWeek in 1..7) { "dayOfWeek 应为 1-7，实际为 $dayOfWeek" }
        require(startSection >= 1) { "startSection 应 >= 1，实际为 $startSection" }
        require(endSection >= startSection) {
            "endSection($endSection) 应 >= startSection($startSection)"
        }
        require(weekSpec.isNotBlank()) { "weekSpec 不能为空" }
    }
}
