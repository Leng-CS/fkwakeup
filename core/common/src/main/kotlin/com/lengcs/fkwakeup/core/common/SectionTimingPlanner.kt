package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.SectionTemplate

/** 连续节次的一组规则；[startIndex] 是用户手动设定时间的断点。 */
data class SectionTimingSegment(
    val startIndex: Int,
    val startMinutes: Int,
    val lessonDurationMinutes: Int,
    val breakDurationMinutes: Int,
)

data class PlannedSection(
    val index: Int,
    val startMinutes: Int,
    val endMinutes: Int,
)

sealed interface SectionTimingPlanResult {
    data class Valid(val sections: List<PlannedSection>) : SectionTimingPlanResult
    data class Invalid(val message: String) : SectionTimingPlanResult
}

/**
 * 将「断点 + 单节时长 + 课间时长」展开为最终节次时间。
 *
 * 最终数据库仍只存 [SectionTemplate] 的起止分钟数；本规则只服务编辑界面的智能递推，
 * 因而不改变导入/导出数据契约。
 */
object SectionTimingPlanner {

    fun build(sectionCount: Int, segments: List<SectionTimingSegment>): SectionTimingPlanResult {
        if (sectionCount < 1) return SectionTimingPlanResult.Invalid("至少需要一节课")
        val sorted = segments.sortedBy(SectionTimingSegment::startIndex)
        if (sorted.firstOrNull()?.startIndex != 1) return SectionTimingPlanResult.Invalid("第 1 节必须是第一个连续时段")
        if (sorted.map(SectionTimingSegment::startIndex).distinct().size != sorted.size) {
            return SectionTimingPlanResult.Invalid("连续时段不能从同一节开始")
        }
        if (sorted.any { it.startIndex !in 1..sectionCount }) return SectionTimingPlanResult.Invalid("断点节次越界")

        val result = mutableListOf<PlannedSection>()
        sorted.forEachIndexed { position, segment ->
            if (segment.startMinutes !in 0 until DAY_MINUTES || segment.lessonDurationMinutes <= 0 || segment.breakDurationMinutes < 0) {
                return SectionTimingPlanResult.Invalid("时长或开始时间无效")
            }
            val endExclusive = sorted.getOrNull(position + 1)?.startIndex ?: sectionCount + 1
            for (index in segment.startIndex until endExclusive) {
                val offset = index - segment.startIndex
                val start = segment.startMinutes + offset * (segment.lessonDurationMinutes + segment.breakDurationMinutes)
                val end = start + segment.lessonDurationMinutes
                if (start !in 0 until DAY_MINUTES || end !in 1..DAY_MINUTES) {
                    return SectionTimingPlanResult.Invalid("第 $index 节超出当天时间范围")
                }
                result += PlannedSection(index, start, end)
            }
        }
        result.zipWithNext().firstOrNull { (previous, next) -> previous.endMinutes > next.startMinutes }?.let { (previous, next) ->
            return SectionTimingPlanResult.Invalid("第 ${previous.index} 节结束时间晚于第 ${next.index} 节开始时间")
        }
        return SectionTimingPlanResult.Valid(result)
    }

    /** 从已保存的节次表反推连续段，让再次打开页面仍可编辑断点。 */
    fun segmentsFrom(templates: List<SectionTemplate>): List<SectionTimingSegment> {
        val rows = templates.sortedBy(SectionTemplate::index)
        if (rows.isEmpty()) return emptyList()
        val result = mutableListOf<SectionTimingSegment>()
        var cursor = 0
        while (cursor < rows.size) {
            val first = rows[cursor]
            val duration = first.endMinutes - first.startMinutes
            val breakDuration = rows.getOrNull(cursor + 1)?.let { it.startMinutes - first.endMinutes }?.coerceAtLeast(0) ?: DEFAULT_BREAK_MINUTES
            var end = cursor
            while (end + 1 < rows.size) {
                val previous = rows[end]
                val next = rows[end + 1]
                val expectedStart = previous.startMinutes + duration + breakDuration
                if (next.index != previous.index + 1 || next.startMinutes != expectedStart || next.endMinutes - next.startMinutes != duration) break
                end++
            }
            result += SectionTimingSegment(first.index, first.startMinutes, duration, breakDuration)
            cursor = end + 1
        }
        return result
    }

    private const val DAY_MINUTES = 24 * 60
    private const val DEFAULT_BREAK_MINUTES = 10
}
