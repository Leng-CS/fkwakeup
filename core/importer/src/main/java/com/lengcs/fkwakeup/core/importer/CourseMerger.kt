package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.SessionDraft

/**
 * 课程归并：把扁平的时间段列表归成「一门课 + 多个时间段」。
 *
 * 归并键 = 课程名 + 教师，大小写不敏感、去除内部空格。
 * AI 容易在归并这一步出错，所以归并放在 App 侧做，用户可在预览页拆分/合并。
 */
object CourseMerger {

    fun merge(sessions: List<SessionDraft>): List<MergedCourse> {
        val grouped = linkedMapOf<String, MutableList<SessionDraft>>()
        val keyToCourse = linkedMapOf<String, MergedCourse>()

        for (session in sessions) {
            val name = session.name?.trim().orEmpty()
            if (name.isEmpty()) continue // 无名记录由校验阶段报错，不参与归并

            val key = mergeKey(name, session.teacher)
            grouped.getOrPut(key) { mutableListOf() }.add(session)
            keyToCourse.getOrPut(key) {
                MergedCourse(name = name, teacher = session.teacher, sessions = emptyList())
            }
        }

        return grouped.map { (key, list) ->
            val course = keyToCourse.getValue(key)
            course.copy(sessions = list.sortedWith(compareBy({ it.dayOfWeek ?: 0 }, { it.startSection ?: 0 })))
        }
    }

    /** 归并键：名称 + 教师，统一小写并去掉空格 */
    fun mergeKey(name: String, teacher: String?): String =
        buildString {
            append(name.trim().lowercase().replace(Regex("""\s+"""), ""))
            append('|')
            append(teacher?.trim()?.lowercase()?.replace(Regex("""\s+"""), "").orEmpty())
        }
}
