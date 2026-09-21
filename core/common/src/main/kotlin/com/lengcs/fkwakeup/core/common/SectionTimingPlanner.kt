package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.SectionTemplate

/** 整张课表共用的单节与课间时长。 */
data class SectionTimingSettings(
    val lessonDurationMinutes: Int,
    val breakDurationMinutes: Int,
)

/** 用户手动设置开始时间的节次；它会成为后续节次的递推断点。 */
data class SectionTimingOverride(
    val index: Int,
    val startMinutes: Int,
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
 * 将全局时长和用户手动设置的节次展开成最终时间表。
 *
 * 数据库依然只保存 [SectionTemplate] 的起止分钟数。本对象只决定编辑界面的递推方式，
 * 因而不会改变导入、导出或 Room 数据契约。
 */
object SectionTimingPlanner {

    fun build(
        sectionCount: Int,
        settings: SectionTimingSettings,
        overrides: List<SectionTimingOverride>,
    ): SectionTimingPlanResult {
        if (sectionCount < 1) return SectionTimingPlanResult.Invalid("至少需要一节课")
        if (settings.lessonDurationMinutes <= 0 || settings.breakDurationMinutes < 0) {
            return SectionTimingPlanResult.Invalid("单节时长或课间时长无效")
        }
        val starts = overrides.associateBy(SectionTimingOverride::index)
        if (starts.size != overrides.size || 1 !in starts || starts.keys.any { it !in 1..sectionCount }) {
            return SectionTimingPlanResult.Invalid("节次开始时间无效")
        }

        val result = mutableListOf<PlannedSection>()
        for (index in 1..sectionCount) {
            val start = starts[index]?.startMinutes ?: run {
                val previous = result.last()
                previous.startMinutes + settings.lessonDurationMinutes + settings.breakDurationMinutes
            }
            val end = start + settings.lessonDurationMinutes
            if (start !in 0 until DAY_MINUTES || end !in 1..DAY_MINUTES) {
                return SectionTimingPlanResult.Invalid("第 $index 节超出当天时间范围")
            }
            result += PlannedSection(index, start, end)
        }
        result.zipWithNext().firstOrNull { (previous, next) -> previous.endMinutes > next.startMinutes }?.let { (previous, next) ->
            return SectionTimingPlanResult.Invalid("第 ${previous.index} 节结束时间晚于第 ${next.index} 节开始时间")
        }
        return SectionTimingPlanResult.Valid(result)
    }

    fun settingsFrom(templates: List<SectionTemplate>): SectionTimingSettings {
        val rows = templates.sortedBy(SectionTemplate::index)
        val first = rows.firstOrNull() ?: return SectionTimingSettings(DEFAULT_LESSON_MINUTES, DEFAULT_BREAK_MINUTES)
        val breakMinutes = rows.getOrNull(1)?.let { it.startMinutes - first.endMinutes } ?: DEFAULT_BREAK_MINUTES
        return SectionTimingSettings(
            lessonDurationMinutes = first.endMinutes - first.startMinutes,
            breakDurationMinutes = breakMinutes.coerceAtLeast(0),
        )
    }

    /** 从已保存时间表找出非全局递推结果，重新打开页面时仍保留午间等断点。 */
    fun overridesFrom(templates: List<SectionTemplate>): List<SectionTimingOverride> {
        val rows = templates.sortedBy(SectionTemplate::index)
        if (rows.isEmpty()) return listOf(SectionTimingOverride(1, DEFAULT_START_MINUTES))
        val settings = settingsFrom(rows)
        return buildList {
            add(SectionTimingOverride(1, rows.first().startMinutes))
            rows.zipWithNext().forEach { (previous, next) ->
                val expectedStart = previous.startMinutes + settings.lessonDurationMinutes + settings.breakDurationMinutes
                if (next.startMinutes != expectedStart) add(SectionTimingOverride(next.index, next.startMinutes))
            }
        }
    }

    private const val DAY_MINUTES = 24 * 60
    private const val DEFAULT_START_MINUTES = 8 * 60
    private const val DEFAULT_LESSON_MINUTES = 45
    private const val DEFAULT_BREAK_MINUTES = 10
}
