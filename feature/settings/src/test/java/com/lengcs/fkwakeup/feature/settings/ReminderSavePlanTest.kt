package com.lengcs.fkwakeup.feature.settings

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.RecurringReminderMode
import com.lengcs.fkwakeup.core.model.ReminderOccurrenceKind
import org.junit.Test
import java.time.LocalDate

class ReminderSavePlanTest {
    private val first = ReminderChoice("session:1:week:1", ReminderOccurrenceKind.SESSION, 1, 1, LocalDate.of(2026, 9, 21), "第一周")
    private val second = ReminderChoice("session:1:week:2", ReminderOccurrenceKind.SESSION, 1, 2, LocalDate.of(2026, 9, 28), "第二周")

    @Test
    fun customScopeStoresOnlySelectedOccurrences() {
        val plan = buildReminderSavePlan(9, CourseReminderUiState(scope = ReminderScope.CUSTOM, choices = listOf(first, second), selectedKeys = setOf(second.key)))

        assertThat(plan.rule.recurringMode).isEqualTo(RecurringReminderMode.NONE)
        assertThat(plan.overrides.map { it.occurrenceKey }).containsExactly(second.key)
        assertThat(plan.overrides.single().enabled).isTrue()
    }

    @Test
    fun allFutureStoresOnlyExcludedOccurrences() {
        val plan = buildReminderSavePlan(9, CourseReminderUiState(scope = ReminderScope.ALL_FUTURE, choices = listOf(first, second), selectedKeys = setOf(first.key)))

        assertThat(plan.rule.recurringMode).isEqualTo(RecurringReminderMode.ALL_FUTURE)
        assertThat(plan.overrides.map { it.occurrenceKey }).containsExactly(second.key)
        assertThat(plan.overrides.single().enabled).isFalse()
    }

    @Test
    fun offClearsRecurringAndOccurrenceSelections() {
        val plan = buildReminderSavePlan(9, CourseReminderUiState(scope = ReminderScope.OFF, choices = listOf(first), selectedKeys = setOf(first.key)))

        assertThat(plan.rule.recurringMode).isEqualTo(RecurringReminderMode.NONE)
        assertThat(plan.overrides).isEmpty()
    }
}
