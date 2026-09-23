package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import java.time.LocalDate

/** Final check after manual correction. No incomplete record may be silently skipped on save. */
object ImportPreviewGate {
    fun problems(courses: List<MergedCourse>, totalWeeks: Int, termStart: LocalDate?): List<String> = buildList {
        val termEnd = termStart?.plusWeeks(totalWeeks.toLong())?.minusDays(1)
        courses.forEach { course ->
            if (course.sessions.isEmpty() && course.onlineWindows.isEmpty()) {
                add("${course.name}：请添加安排或删除空课程")
            }
            course.sessions.forEach { session ->
                val start = session.startSection
                val end = session.endSection
                val day = session.dayOfWeek
                if (day == null || day !in 1..7 || start == null || end == null || start < 1 || end < start) {
                    add("${course.name}：请补齐并核对星期和节次")
                }
                if (WeekSpecParser.parseOrNull(session.weeks.orEmpty(), totalWeeks) == null) {
                    add("${course.name}：请核对上课周次")
                }
                if (session.deliveryMode == null) add("${course.name}：请确认授课方式")
            }
            course.onlineWindows.forEach { window ->
                val start = window.startDate
                val end = window.endDate
                if (window.note?.contains("待确认授课方式") == true) {
                    add("${course.name}：请确认是直播还是异步网课")
                }
                if (start == null || end == null || end.isBefore(start)) {
                    add("${course.name}：请补齐并核对开放日期")
                } else if (termStart != null && termEnd != null && (end.isBefore(termStart) || start.isAfter(termEnd))) {
                    add("${course.name}：开放期与学期没有重叠")
                }
            }
        }
    }
}
