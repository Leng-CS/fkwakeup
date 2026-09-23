package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.core.model.SectionTemplate
import org.junit.Test

class SectionTimingRegressionTest {
    private val defaults = DefaultSections.forTerm(7)
    private val overrides = SectionTimingPlanner.overridesFrom(defaults)

    @Test
    fun `untouched imported table has no unsaved changes`() {
        val imported = listOf(SectionTemplate(7, 1, 480, 525), SectionTemplate(7, 2, 535, 600))
        val initial = SectionTimingPlanner.draftFrom(imported)
        assertThat(SectionTimingPlanner.hasUnsavedChanges(initial, initial.copy())).isFalse()
    }

    @Test
    fun `global rule and section count changes are detected`() {
        val initial = SectionTimingPlanner.draftFrom(defaults)
        assertThat(SectionTimingPlanner.hasUnsavedChanges(initial, initial.copy(
            settings = initial.settings.copy(lessonDurationMinutes = 47),
        ))).isTrue()
        assertThat(SectionTimingPlanner.hasUnsavedChanges(initial, initial.copy(sectionCount = 11))).isTrue()
    }

    @Test
    fun `breakpoint edits are detected and reverting clears dirty state`() {
        val initial = SectionTimingPlanner.draftFrom(defaults)
        val changed = initial.copy(overrides = initial.overrides + SectionTimingOverride(2, 540))
        assertThat(SectionTimingPlanner.hasUnsavedChanges(initial, changed)).isTrue()
        assertThat(SectionTimingPlanner.hasUnsavedChanges(initial, changed.copy(overrides = initial.overrides))).isFalse()
        assertThat(SectionTimingPlanner.hasUnsavedChanges(
            initial,
            initial.copy(overrides = initial.overrides.reversed()),
        )).isFalse()
    }

    @Test
    fun `changing global duration updates automatic sections and preserves breaks`() {
        val result = SectionTimingPlanner.build(12, SectionTimingSettings(47, 10), overrides)
            as SectionTimingPlanResult.Valid
        assertThat(result.sections[0].endMinutes).isEqualTo(527)
        assertThat(result.sections[1].startMinutes).isEqualTo(537)
        assertThat(result.sections[4].startMinutes).isEqualTo(840)
        assertThat(result.sections[5].startMinutes).isEqualTo(897)
        assertThat(result.sections.last().endMinutes).isEqualTo(1358)
    }

    @Test
    fun `changing global break updates subsequent automatic starts`() {
        val result = SectionTimingPlanner.build(12, SectionTimingSettings(45, 5), overrides)
            as SectionTimingPlanResult.Valid
        assertThat(result.sections[1].startMinutes).isEqualTo(530)
        assertThat(result.sections[5].startMinutes).isEqualTo(890)
    }

    @Test
    fun `confirming unchanged start does not freeze automatic section`() {
        val edited = SectionTimingPlanner.overridesAfterEdit(overrides, PlannedSection(2, 535, 580), 535)
        assertThat(edited).isEqualTo(overrides)
        val result = SectionTimingPlanner.build(12, SectionTimingSettings(47, 10), edited)
            as SectionTimingPlanResult.Valid
        assertThat(result.sections[1].startMinutes).isEqualTo(537)
    }

    @Test
    fun `changing start creates a breakpoint`() {
        val edited = SectionTimingPlanner.overridesAfterEdit(overrides, PlannedSection(2, 535, 580), 540)
        assertThat(edited).contains(SectionTimingOverride(2, 540))
    }

    @Test
    fun `conflicting global duration is rejected without modifying breakpoints`() {
        val before = overrides.toList()
        val result = SectionTimingPlanner.build(12, SectionTimingSettings(60, 10), overrides)
        assertThat(result).isInstanceOf(SectionTimingPlanResult.Invalid::class.java)
        assertThat(overrides).isEqualTo(before)
    }

    @Test
    fun `saved generated table reopens with identical times`() {
        val result = SectionTimingPlanner.build(12, SectionTimingSettings(47, 10), overrides)
            as SectionTimingPlanResult.Valid
        val saved = result.sections.map { SectionTemplate(7, it.index, it.startMinutes, it.endMinutes) }
        assertThat(SectionTimingPlanner.build(12, SectionTimingPlanner.settingsFrom(saved),
            SectionTimingPlanner.overridesFrom(saved))).isEqualTo(result)
    }
}
