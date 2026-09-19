package com.lengcs.fkwakeup.core.exporter

import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 导出为 campus-timetable v1.0 JSON。
 *
 * 关键要求是**可往返**：导出的文件必须能被 [com.lengcs.fkwakeup.core.importer.TimetableImporter] 原样读回，
 * 所以字段、类型、null 处理都严格对齐主开发文档 5.1 / 5.2。
 * 这一条有单测守着：core:exporter 的 TimetableExporterTest。
 */
object TimetableExporter {

    const val FORMAT = "campus-timetable"
    const val VERSION = "1.0"

    fun export(
        term: Term,
        sections: List<SectionTemplate>,
        courses: List<CourseWithSessions>,
    ): String {
        val root = buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("term", buildJsonObject {
                put("name", term.name)
                put("startMonday", term.startMonday.toString())
                put("totalWeeks", term.totalWeeks)
            })

            if (sections.isNotEmpty()) {
                put("sectionTemplates", buildJsonArray {
                    sections.sortedBy { it.index }.forEach { section ->
                        add(buildJsonObject {
                            put("index", section.index)
                            put("start", formatClock(section.startMinutes))
                            put("end", formatClock(section.endMinutes))
                        })
                    }
                })
            }

            put("sessions", buildJsonArray {
                courses.forEach { entry ->
                    entry.sessions.forEach { session ->
                        add(buildJsonObject {
                            put("name", entry.course.name)
                            putNullableString("teacher", entry.course.teacher)
                            putNullableString("location", session.location)
                            put("dayOfWeek", session.dayOfWeek)
                            put("startSection", session.startSection)
                            put("endSection", session.endSection)
                            put("weeks", session.weekSpec)
                            putNullableString("note", session.note)
                        })
                    }
                }
            })
        }
        return root.toString()
    }

    private fun formatClock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
}

/** 缺失字段显式写成 null，而不是省略 —— 导入侧的别名映射依赖字段存在 */
private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableString(key: String, value: String?) {
    put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
}
