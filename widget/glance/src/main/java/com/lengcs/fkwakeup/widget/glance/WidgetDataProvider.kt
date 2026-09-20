package com.lengcs.fkwakeup.widget.glance

import com.lengcs.fkwakeup.core.common.BlockPhase
import com.lengcs.fkwakeup.core.common.BlockPhaseCalculator
import com.lengcs.fkwakeup.core.common.CurrentTermPick
import com.lengcs.fkwakeup.core.common.CurrentWeekCalculator
import com.lengcs.fkwakeup.core.common.MutedBlockColor
import com.lengcs.fkwakeup.core.common.ScheduleLayout
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

/** 小组件要展示的一节课 */
data class WidgetLesson(
    val name: String,
    val teacher: String?,
    val location: String?,
    val startMinutes: Int,
    val endMinutes: Int,
    val sectionText: String,
    val timeText: String,
)

/** 网格里的一个格子 */
data class WidgetCell(
    val label: String,
    /** 该课程最终使用的颜色（自定义色优先，否则按课名哈希） */
    val colorArgb: Int,
    /** 相对此刻的状态（#29）：已上完的在小组件上同样变灰 */
    val phase: BlockPhase = BlockPhase.Upcoming,
)

/** 小组件数据快照 */
data class WidgetData(
    val termName: String?,
    val weekText: String,
    val nextLesson: WidgetLesson?,
    val remainingToday: List<WidgetLesson>,
    /** [节次][星期] -> 格子；没有课为 null */
    val weekGrid: List<List<WidgetCell?>>,
)

class WidgetDataProvider(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun load(now: LocalDateTime = LocalDateTime.now()): WidgetData? {
        val settings = settingsRepository.settings.first()
        // 与 CurrentTermProvider 共用同一套「当前学期」选择规则（CHANGELOG #26）：
        // 配置值优先，无效才退回「开学日期最晚的未归档学期」。
        // 小组件这边没走 CurrentTermProvider，是因为 Glance 侧是手动构造（见 WidgetDependencies）。
        val termId = CurrentTermPick.pick(
            configuredTermId = settings.currentTermId,
            fallbackIds = termRepository.observeTerms().first().map { it.id },
        ) ?: return null

        val term = termRepository.getTerm(termId) ?: return null
        val sections = termRepository.getSections(termId)
        val courses = courseRepository.observeCourses(termId).first()

        val today = now.toLocalDate()
        val week = CurrentWeekCalculator.status(term, today).week
        val dayOfWeek = today.dayOfWeek.value
        val nowMinutes = now.hour * 60 + now.minute

        val lessons = todayLessons(courses, sections, week, dayOfWeek, term.totalWeeks)
        val remaining = lessons.filter { it.endMinutes > nowMinutes }

        return WidgetData(
            termName = term.name,
            weekText = "第 ${week}周",
            nextLesson = remaining.firstOrNull(),
            remainingToday = remaining,
            weekGrid = buildGrid(
                courses = courses,
                sections = sections,
                week = week,
                totalWeeks = term.totalWeeks,
                weekMonday = BlockPhaseCalculator.weekMonday(term.startMonday, week),
                now = now,
            ),
        )
    }

    private fun todayLessons(
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        week: Int,
        dayOfWeek: Int,
        totalWeeks: Int,
    ): List<WidgetLesson> {
        val sectionTimes = sections.associateBy { it.index }
        val result = mutableListOf<WidgetLesson>()

        for (entry in courses) {
            for (session in entry.sessions) {
                if (session.dayOfWeek != dayOfWeek) continue
                val weeks = ScheduleLayout.build(
                    listOf(CourseWithSessions(entry.course, listOf(session))),
                    week,
                    totalWeeks,
                )
                if (weeks.isEmpty()) continue

                val start = sectionTimes[session.startSection]
                val end = sectionTimes[session.endSection]
                if (start == null || end == null) continue

                val sectionText = if (session.startSection == session.endSection) {
                    "第${session.startSection}节"
                } else {
                    "第${session.startSection}-${session.endSection}节"
                }

                result += WidgetLesson(
                    name = entry.course.name,
                    teacher = entry.course.teacher,
                    location = session.location,
                    startMinutes = start.startMinutes,
                    endMinutes = end.endMinutes,
                    sectionText = sectionText,
                    timeText = "${formatClock(start.startMinutes)}-${formatClock(end.endMinutes)}",
                )
            }
        }
        return result.sortedBy { it.startMinutes }
    }

    /**
     * 本周网格。跨节次的课会在它覆盖的每一节都显示课程名 ——
     * Glance 没有自定义 Layout，做不了 App 里那种跨行长块，
     * 所以小组件上是「每个格子标课名」的缩略形式。
     */
    private fun buildGrid(
        courses: List<CourseWithSessions>,
        sections: List<SectionTemplate>,
        week: Int,
        totalWeeks: Int,
        weekMonday: LocalDate,
        now: LocalDateTime,
    ): List<List<WidgetCell?>> {
        val rows = sections.size.coerceAtMost(MAX_GRID_ROWS)
        val grid = List(rows) { MutableList<WidgetCell?>(7) { null } }
        val blocks = ScheduleLayout.build(courses, week, totalWeeks)

        for (block in blocks) {
            val label = block.course.name.take(3)
            val from = block.startRow
            val to = (block.startRow + block.rowSpan - 1).coerceAtMost(rows - 1)
            for (row in from..to) {
                if (row in 0 until rows) {
                    // 小组件是「每格标课名」的缩略形式，一节课跨几节就出现几次，
                    // 所以相位要**按这一格所在的节次**单独判，不能整块共用一个结果 ——
                    // 否则正在上的那节课会被它后面那节判定成「已上完」而一起变灰。
                    val section = sections.getOrNull(row)
                    val phase = section?.let {
                        BlockPhaseCalculator.of(
                            date = weekMonday.plusDays(block.dayIndex.toLong()),
                            startMinutes = it.startMinutes,
                            endMinutes = it.endMinutes,
                            now = now,
                        )
                    } ?: BlockPhase.Upcoming
                    grid[row][block.dayIndex] = WidgetCell(label, block.colorArgb, phase)
                }
            }
        }
        return grid
    }

    private fun formatClock(minutes: Int): String =
        "%02d:%02d".format(minutes / 60, minutes % 60)

    private companion object {
        const val MAX_GRID_ROWS = 12
    }
}
