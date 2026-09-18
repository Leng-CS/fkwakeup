package com.lengcs.fkwakeup.core.model

/**
 * 节次时间表条目：第 [index] 节课的起止时间（自 00:00 起的分钟数）。
 *
 * 各校节次时间不同，禁止硬编码，一律走本表配置。
 */
data class SectionTemplate(
    val termId: Long,
    val index: Int,
    val startMinutes: Int,
    val endMinutes: Int,
) {
    init {
        require(index >= 1) { "节次必须从 1 开始，实际为 $index" }
        require(startMinutes in 0..(24 * 60 - 1)) { "startMinutes 越界: $startMinutes" }
        require(endMinutes in 1..(24 * 60)) { "endMinutes 越界: $endMinutes" }
        require(endMinutes > startMinutes) { "结束时间必须晚于开始时间: $startMinutes-$endMinutes" }
    }
}
