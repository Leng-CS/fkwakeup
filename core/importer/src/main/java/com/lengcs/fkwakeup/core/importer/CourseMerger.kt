package com.lengcs.fkwakeup.core.importer

import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft

/**
 * 课程归并：把扁平的时间段列表归成「一门课 + 多个时间段」。
 *
 * 归并键 = 课程名 + 教师，大小写不敏感、去除内部空格。
 * AI 容易在归并这一步出错，所以归并放在 App 侧做，用户可在预览页拆分/合并。
 */
object CourseMerger {

    fun merge(
        sessions: List<SessionDraft>,
        onlineWindows: List<OnlineWindowDraft> = emptyList(),
    ): List<MergedCourse> {
        val keyToCourse = linkedMapOf<String, MergedCourse>()

        for (session in sessions) {
            val name = session.name?.trim().orEmpty()
            if (name.isEmpty()) continue // 无名记录由校验阶段报错，不参与归并

            val key = mergeKey(name, session.teacher)
            val course = keyToCourse.getOrPut(key) {
                MergedCourse(name = name, teacher = session.teacher, sessions = emptyList())
            }
            keyToCourse[key] = course.copy(sessions = course.sessions + session)
        }

        for (window in onlineWindows) {
            val name = window.name?.trim().orEmpty()
            if (name.isEmpty()) continue
            val key = mergeKey(name, window.teacher)
            val course = keyToCourse.getOrPut(key) {
                MergedCourse(name = name, teacher = window.teacher, sessions = emptyList())
            }
            keyToCourse[key] = course.copy(onlineWindows = course.onlineWindows + window)
        }

        return keyToCourse.values.map { course ->
            course.copy(
                sessions = course.sessions.sortedWith(compareBy({ it.dayOfWeek ?: 0 }, { it.startSection ?: 0 })),
                onlineWindows = course.onlineWindows.sortedBy { it.startDate },
            )
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
