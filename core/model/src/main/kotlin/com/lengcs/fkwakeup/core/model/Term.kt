package com.lengcs.fkwakeup.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * 学期（课表容器）。
 *
 * [startMonday] 必须是周一；导入时若不是周一，由导入层向前对齐并提示用户。
 */
data class Term(
    val id: Long = 0L,
    val name: String,
    val startMonday: LocalDate,
    val totalWeeks: Int = DEFAULT_TOTAL_WEEKS,
    val isArchived: Boolean = false,
    val createdAt: Instant = Instant.EPOCH,
) {
    companion object {
        const val DEFAULT_TOTAL_WEEKS = 18
    }
}
