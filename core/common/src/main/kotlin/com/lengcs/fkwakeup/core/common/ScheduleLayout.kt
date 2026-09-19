package com.lengcs.fkwakeup.core.common

import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate

/**
 * 课表上的一个课程块。
 *
 * 坐标全部是 0 起的行列下标，由 UI 层换算成像素：
 * `x = columnWidth * (dayIndex + columnIndex.toFloat() / columnCount)`
 * `y = rowHeight * startRow`
 */
data class ScheduleBlock(
    val course: Course,
    val session: CourseSession,
    /** 0 = 周一 … 6 = 周日 */
    val dayIndex: Int,
    /** 0 起的节次下标 */
    val startRow: Int,
    /** 跨几节 */
    val rowSpan: Int,
    /** 同一时间段内有冲突时，本块排在第几位（0 起） */
    val columnIndex: Int = 0,
    /** 同一时间段内一共几个块（1 表示无冲突） */
    val columnCount: Int = 1,
    /** 本块开始时间（自 00:00 起的分钟数），节次时间表缺失时为 null */
    val startMinutes: Int? = null,
    /** 本块结束时间，节次时间表缺失时为 null */
    val endMinutes: Int? = null,
)

/**
 * 课表网格布局计算（主开发文档 7.1）。
 *
 * 纯 Kotlin，不依赖 Android，便于单测覆盖跨节次与冲突两种 hardest case。
 */
object ScheduleLayout {

    /**
     * @param courses 课程及其全部时间段
     * @param week 要渲染的周次（1 起）
     * @param totalWeeks 学期总周数，用于解析周次表达式
     * @param sections 节次时间表，用于把节次换算成具体时间；不传则块上没有时间
     */
    fun build(
        courses: List<CourseWithSessions>,
        week: Int,
        totalWeeks: Int,
        sections: List<SectionTemplate> = emptyList(),
    ): List<ScheduleBlock> {
        val sectionTimes = sections.associateBy { it.index }
        val raw = mutableListOf<ScheduleBlock>()

        for (entry in courses) {
            for (session in entry.sessions) {
                // 周次表达式解析不出来就跳过，不让一条坏数据炸掉整页
                val weeks = WeekSpecParser.parseOrNull(session.weekSpec, totalWeeks) ?: continue
                if (week !in weeks) continue

                raw += ScheduleBlock(
                    course = entry.course,
                    session = session,
                    dayIndex = (session.dayOfWeek - 1).coerceIn(0, 6),
                    startRow = (session.startSection - 1).coerceAtLeast(0),
                    rowSpan = (session.endSection - session.startSection + 1).coerceAtLeast(1),
                    startMinutes = sectionTimes[session.startSection]?.startMinutes,
                    endMinutes = sectionTimes[session.endSection]?.endMinutes,
                )
            }
        }

        return resolveConflicts(raw)
    }

    /**
     * 同一天、时间有交集的课程块并排显示，各占 1/N 宽度。
     *
     * 用「簇」的方式处理：只要某个块的开始行还在当前簇的最大结束行之内，
     * 就归入同一簇，簇内所有块等分宽度。这样跨节次的课程和它重叠的短课程
     * 也能正确并排，而不是简单按起始行分组。
     */
    private fun resolveConflicts(blocks: List<ScheduleBlock>): List<ScheduleBlock> {
        val result = mutableListOf<ScheduleBlock>()

        blocks.groupBy { it.dayIndex }.forEach { (_, dayBlocks) ->
            val sorted = dayBlocks.sortedWith(compareBy({ it.startRow }, { it.rowSpan }))
            var cluster = mutableListOf<ScheduleBlock>()
            var clusterEndRow = -1

            fun flushCluster() {
                if (cluster.isEmpty()) return
                val count = cluster.size
                cluster.forEachIndexed { index, block ->
                    result += block.copy(columnIndex = index, columnCount = count)
                }
                cluster = mutableListOf()
                clusterEndRow = -1
            }

            for (block in sorted) {
                val lastRow = block.startRow + block.rowSpan - 1
                if (cluster.isNotEmpty() && block.startRow > clusterEndRow) flushCluster()
                cluster += block
                clusterEndRow = maxOf(clusterEndRow, lastRow)
            }
            flushCluster()
        }

        return result
    }

    /**
     * 当前处于第几节；课间或不在任何节次内返回 null。
     */
    fun currentSectionIndex(sections: List<SectionTemplate>, minutesOfDay: Int): Int? =
        sections.firstOrNull { minutesOfDay in it.startMinutes until it.endMinutes }?.index
}
