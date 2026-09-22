package com.lengcs.fkwakeup.core.model

/**
 * 课程连同其全部上课时间段的读模型（M1 新增）。
 *
 * 主开发文档 4.1 的实体定义不变，这个类只是为了方便 UI 一次性拿到
 * 「一门课 + 它的多个时间段」。
 */
data class CourseWithSessions(
    val course: Course,
    val sessions: List<CourseSession>,
    val onlineWindows: List<OnlineCourseWindow> = emptyList(),
)
