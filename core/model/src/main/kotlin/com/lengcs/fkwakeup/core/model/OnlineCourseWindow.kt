package com.lengcs.fkwakeup.core.model

import java.time.LocalDate

/**
 * 异步网课的一段可学习日期区间。
 *
 * 它没有周几、周次或节次；同一课程可以配置多个开放期。
 */
data class OnlineCourseWindow(
    val id: Long = 0L,
    val courseId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val platform: String? = null,
    val url: String? = null,
    val note: String? = null,
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "endDate($endDate) 不得早于 startDate($startDate)"
        }
    }
}
