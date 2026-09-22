package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.SectionTemplate
import org.junit.Test

class SectionTimingPlannerTest {
    @Test
    fun `first lesson and global durations recalculate following lessons`() {
        val plan = SectionTimingPlanner.build(
            sectionCount = 4,
            settings = SectionTimingSettings(lessonDurationMinutes = 40, breakDurationMinutes = 5),
            overrides = listOf(SectionTimingOverride(1, 8 * 60 + 30)),
        ) as SectionTimingPlanResult.Valid

        assertThat(plan.sections.map { it.startMinutes to it.endMinutes }).containsExactly(
            510 to 550, 555 to 595, 600 to 640, 645 to 685,
        ).inOrder()
    }

    @Test
    fun `manual afternoon start becomes breakpoint and recalculates following lessons`() {
        val plan = SectionTimingPlanner.build(
            sectionCount = 6,
            settings = SectionTimingSettings(40, 5),
            overrides = listOf(SectionTimingOverride(1, 510), SectionTimingOverride(5, 14 * 60)),
        ) as SectionTimingPlanResult.Valid

        assertThat(plan.sections.drop(4).map { it.startMinutes to it.endMinutes }).containsExactly(
            840 to 880, 885 to 925,
        ).inOrder()
    }

    @Test
    fun `breakpoint cannot overlap its previous lesson`() {
        val plan = SectionTimingPlanner.build(
            sectionCount = 4,
            settings = SectionTimingSettings(40, 5),
            overrides = listOf(SectionTimingOverride(1, 510), SectionTimingOverride(3, 550)),
        )

        assertThat(plan).isInstanceOf(SectionTimingPlanResult.Invalid::class.java)
        assertThat((plan as SectionTimingPlanResult.Invalid).message).contains("第 2 节")
    }

    @Test
    fun `longer lesson duration cannot overlap a later manually set lesson`() {
        val plan = SectionTimingPlanner.build(
            sectionCount = 2,
            settings = SectionTimingSettings(60, 5),
            overrides = listOf(SectionTimingOverride(1, 480), SectionTimingOverride(2, 520)),
        )

        assertThat(plan).isInstanceOf(SectionTimingPlanResult.Invalid::class.java)
        assertThat((plan as SectionTimingPlanResult.Invalid).message).contains("第 1 节")
    }

    @Test
    fun `saved table restores global settings and manual breakpoints`() {
        val templates = listOf(
            SectionTemplate(1, 1, 510, 550),
            SectionTemplate(1, 2, 555, 595),
            SectionTemplate(1, 3, 840, 880),
            SectionTemplate(1, 4, 885, 925),
        )

        assertThat(SectionTimingPlanner.settingsFrom(templates)).isEqualTo(SectionTimingSettings(40, 5))
        assertThat(SectionTimingPlanner.overridesFrom(templates)).containsExactly(
            SectionTimingOverride(1, 510), SectionTimingOverride(3, 840),
        ).inOrder()
    }
}
