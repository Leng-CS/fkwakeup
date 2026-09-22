package com.lengcs.fkwakeup.core.exporter

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.importer.TimetableImporter
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TimetableExporterTest {

    private val term = Term(
        id = 1L,
        name = "2026-2027 秋季学期",
        startMonday = LocalDate.of(2026, 9, 7),
        totalWeeks = 18,
        isArchived = false,
        createdAt = Instant.EPOCH,
    )

    /**
     * 节次时间表要覆盖课程用到的最大学号，否则校验会报 E_SECTION_OOB
     * （本用例的课程用到了第 6 节）
     */
    private val sections = (1..6).map { index ->
        val start = 480 + (index - 1) * 60
        SectionTemplate(1L, index, start, start + 45)
    }

    private val courses = listOf(
        CourseWithSessions(
            course = Course(id = 1L, termId = 1L, name = "高等数学A", teacher = "张伟"),
            sessions = listOf(
                CourseSession(1L, 1L, dayOfWeek = 1, startSection = 1, endSection = 2, weekSpec = "1-16", location = "教三-301"),
                CourseSession(
                    2L,
                    1L,
                    dayOfWeek = 3,
                    startSection = 3,
                    endSection = 4,
                    weekSpec = "2-16双",
                    deliveryMode = SessionDeliveryMode.LIVE_ONLINE,
                    onlinePlatform = "腾讯会议",
                    onlineUrl = "https://example.edu/live",
                ),
            ),
            onlineWindows = listOf(
                OnlineCourseWindow(
                    id = 1L,
                    courseId = 1L,
                    startDate = LocalDate.parse("2026-09-01"),
                    endDate = LocalDate.parse("2026-12-31"),
                    platform = "学习通",
                    url = "https://example.edu/course",
                ),
            ),
        ),
        CourseWithSessions(
            // 教师与地点都为 null —— 用来验证空字段导出后仍能被正确读回
            course = Course(id = 2L, termId = 1L, name = "线性代数", teacher = null),
            sessions = listOf(
                CourseSession(3L, 2L, dayOfWeek = 4, startSection = 5, endSection = 6, weekSpec = "1-9,11-18", location = null),
            ),
        ),
    )

    @Test
    fun `导出内容包含 format 与 version`() {
        val json = TimetableExporter.export(term, sections, courses)
        assertThat(json).contains("\"format\"" )
        assertThat(json).contains("campus-timetable")
        assertThat(json).contains("1.1")

        val result = TimetableImporter.import(json)
        assertThat(result.errors).isEmpty()
        assertThat(result.term?.name).isEqualTo("2026-2027 秋季学期")
    }

    @Test
    fun `往返后学期信息一致`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        assertThat(result.term).isNotNull()
        assertThat(result.term!!.name).isEqualTo(term.name)
        assertThat(result.term!!.startMonday).isEqualTo(term.startMonday)
        assertThat(result.term!!.totalWeeks).isEqualTo(18)
    }

    @Test
    fun `往返后课程与时间段数量一致`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        assertThat(result.sessionCount).isEqualTo(3)
        assertThat(result.courses).hasSize(2)
    }

    @Test
    fun `往返后字段值保持一致`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        val math = result.courses.first { it.name == "高等数学A" }
        assertThat(math.teacher).isEqualTo("张伟")
        assertThat(math.sessions).hasSize(2)

        // 注意：导入结果里的 sessions 是 SessionDraft（字段可空、周次字段名是 weeks）
        val first = math.sessions.minByOrNull { it.dayOfWeek ?: 0 }
        assertThat(first).isNotNull()
        assertThat(first!!.dayOfWeek).isEqualTo(1)
        assertThat(first.startSection).isEqualTo(1)
        assertThat(first.endSection).isEqualTo(2)
        assertThat(first.weeks).isEqualTo("1-16")
        assertThat(first.location).isEqualTo("教三-301")
    }

    @Test
    fun `单双周与跳周表达式往返后原样保留`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        val specs = result.courses.flatMap { it.sessions }.map { it.weeks }
        assertThat(specs).containsExactly("1-16", "2-16双", "1-9,11-18")
    }

    @Test
    fun `空教师与空地点往返后为 null`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        val linalg = result.courses.first { it.name == "线性代数" }
        assertThat(linalg.teacher).isNull()
        assertThat(linalg.sessions.single().location).isNull()
    }

    @Test
    fun `节次时间表往返后时间一致`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))

        val templates = result.sectionTemplates
        assertThat(templates).hasSize(6)
        assertThat(templates!!.first().startMinutes).isEqualTo(480)
        assertThat(templates!!.first().endMinutes).isEqualTo(525)
        assertThat(templates.last().index).isEqualTo(6)
    }

    @Test
    fun `直播与异步网课字段可往返`() {
        val result = TimetableImporter.import(TimetableExporter.export(term, sections, courses))
        val math = result.courses.first { it.name == "高等数学A" }

        val live = math.sessions.first { it.deliveryMode == SessionDeliveryMode.LIVE_ONLINE }
        assertThat(live.onlinePlatform).isEqualTo("腾讯会议")
        assertThat(live.onlineUrl).isEqualTo("https://example.edu/live")
        assertThat(math.onlineWindows).hasSize(1)
        assertThat(math.onlineWindows.single().startDate).isEqualTo(LocalDate.parse("2026-09-01"))
        assertThat(math.onlineWindows.single().endDate).isEqualTo(LocalDate.parse("2026-12-31"))
        assertThat(math.onlineWindows.single().platform).isEqualTo("学习通")
    }

    @Test
    fun `没有课程时也能导出并可导入`() {
        val json = TimetableExporter.export(term, sections, emptyList())
        val result = TimetableImporter.import(json)
        assertThat(result.errors).isEmpty()
        assertThat(result.courses).isEmpty()
    }
}
