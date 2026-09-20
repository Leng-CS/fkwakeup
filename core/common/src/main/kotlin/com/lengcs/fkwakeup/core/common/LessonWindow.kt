package com.lengcs.fkwakeup.core.common

/**
 * 「今天最近的课程」窗口选择（#30/#31）。
 *
 * 小组件的列表逻辑与刷新调度都依赖这两个判定，抽成纯函数便于单测；
 * 输入是「按开始时间排好序」的课程起止分钟列表（当天第几分钟），与当前分钟。
 */
object LessonWindow {

    /**
     * 今天**尚未结束**的课程下标（含正在上的），最多取 [max] 条。
     *
     * @param starts 各课程开始时刻（当天第几分钟），需与 [ends] 等长且已按开始时间排序
     * @param ends   各课程结束时刻；`endMinutes <= nowMinutes` 视为已结束
     *
     * 判定只看时间：`now == end` 算已结束（与 [BlockPhaseCalculator] 的语义一致），
     * `now == start` 算已开始（含进行中）。
     */
    fun upcomingIndexes(
        starts: List<Int>,
        ends: List<Int>,
        nowMinutes: Int,
        max: Int,
    ): List<Int> {
        if (max <= 0) return emptyList()
        val result = mutableListOf<Int>()
        for (i in starts.indices) {
            if (i >= ends.size) break
            if (ends[i] > nowMinutes) {
                result += i
                if (result.size == max) break
            }
        }
        return result
    }

    /**
     * 下一个需要刷新的「课程边界」时刻（当天第几分钟）：
     * 未开始课程取其开始时刻，进行中课程取其结束时刻，取**最早**者。
     *
     * 返回 null 表示今天没有剩余边界（全上完 / 没课）——
     * 跨天由 `DATE_CHANGED` 广播与 15 分钟周期任务兜底，不必排精确任务。
     */
    fun nextBoundaryMinutes(
        starts: List<Int>,
        ends: List<Int>,
        nowMinutes: Int,
    ): Int? {
        var boundary: Int? = null
        for (i in starts.indices) {
            if (i >= ends.size) break
            val end = ends[i]
            if (end <= nowMinutes) continue // 已结束，与它相关的边界都过去了
            val candidate = if (starts[i] > nowMinutes) starts[i] else end
            if (boundary == null || candidate < boundary) boundary = candidate
        }
        return boundary
    }
}
