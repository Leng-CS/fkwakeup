package com.lengcs.fkwakeup.core.common

/**
 * 课程列表的当前窗口选择。
 *
 * 小组件的“今日未结束”范围和课程边界精确刷新共用这些纯函数；调用方传入
 * 按开始时间排序的当天课程起止分钟数。
 */
object LessonWindow {

    /**
     * 仍应显示的课程下标：正在上与之后的课程都保留，结束时刻等于当前时刻
     * 的课程视为已结束。
     */
    fun remainingIndexes(
        starts: List<Int>,
        ends: List<Int>,
        nowMinutes: Int,
    ): List<Int> = starts.indices.mapNotNull { index ->
        ends.getOrNull(index)
            ?.takeIf { it > nowMinutes }
            ?.let { index }
    }

    /**
     * 下一个需要刷新的课程边界。未开始课程取开始时刻，进行中课程取结束时刻；
     * 没有未结束课程时返回 null。
     */
    fun nextBoundaryMinutes(
        starts: List<Int>,
        ends: List<Int>,
        nowMinutes: Int,
    ): Int? = starts.indices.mapNotNull { index ->
        val end = ends.getOrNull(index) ?: return@mapNotNull null
        if (end <= nowMinutes) return@mapNotNull null
        if (starts[index] > nowMinutes) starts[index] else end
    }.minOrNull()
}
