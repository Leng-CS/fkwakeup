package com.lengcs.fkwakeup.core.importer

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import org.junit.Test
import java.time.LocalDate

class ImportPreviewGateTest {
    private val termStart = LocalDate.parse("2026-09-07")

    @Test fun `unknown online mode and missing dates block import`() {
        val course = MergedCourse("网课", null, sessions = emptyList(), onlineWindows = listOf(
            OnlineWindowDraft(1, "网课", null, null, null, null, null, "待确认授课方式"),
        ))
        val problems = ImportPreviewGate.problems(listOf(course), 18, termStart)
        assertThat(problems).hasSize(2)
        assertThat(problems.joinToString()).contains("确认是直播还是异步")
        assertThat(problems.joinToString()).contains("开放日期")
    }

    @Test fun `corrected online window allows import`() {
        val course = MergedCourse("网课", null, sessions = emptyList(), onlineWindows = listOf(
            OnlineWindowDraft(1, "网课", null, termStart, termStart.plusDays(7), "学习通", null, null),
        ))
        assertThat(ImportPreviewGate.problems(listOf(course), 18, termStart)).isEmpty()
    }

    @Test fun `live lesson without section blocks until corrected`() {
        val missing = SessionDraft(1, "直播", null, null, 3, null, null, "1-16", null, SessionDeliveryMode.LIVE_ONLINE)
        val course = MergedCourse("直播", null, sessions = listOf(missing))
        assertThat(ImportPreviewGate.problems(listOf(course), 18, termStart)).isNotEmpty()
        val corrected = course.copy(sessions = listOf(missing.copy(startSection = 2, endSection = 3)))
        assertThat(ImportPreviewGate.problems(listOf(corrected), 18, termStart)).isEmpty()
    }

    @Test fun `online window outside term blocks import`() {
        val course = MergedCourse("网课", null, sessions = emptyList(), onlineWindows = listOf(
            OnlineWindowDraft(1, "网课", null, termStart.minusDays(30), termStart.minusDays(1), null, null, null),
        ))
        assertThat(ImportPreviewGate.problems(listOf(course), 18, termStart).single()).contains("没有重叠")
    }
}
