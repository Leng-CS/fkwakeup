package com.lengcs.fkwakeup.core.model

/**
 * 课程。一门课可有多个 [CourseSession]（上课时间段）。
 */
data class Course(
    val id: Long = 0L,
    val termId: Long,
    val name: String,
    val teacher: String? = null,
    val colorArgb: Int? = null,
    val note: String? = null,
) {
    init {
        require(name.isNotBlank()) { "课程名不能为空" }
    }
}
