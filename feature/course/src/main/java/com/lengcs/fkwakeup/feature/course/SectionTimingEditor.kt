package com.lengcs.fkwakeup.feature.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.PlannedSection
import com.lengcs.fkwakeup.core.common.SectionTimingOverride
import com.lengcs.fkwakeup.core.common.SectionTimingPlanResult
import com.lengcs.fkwakeup.core.common.SectionTimingPlanner
import com.lengcs.fkwakeup.core.common.SectionTimingSettings
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.core.designsystem.picker.LabeledWheel
import com.lengcs.fkwakeup.core.model.SectionTemplate

@Composable
internal fun SectionTimingEditor(
    sections: List<SectionTemplate>,
    onSave: (List<SectionTemplate>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settings by remember(sections) { mutableStateOf(SectionTimingPlanner.settingsFrom(sections)) }
    val overrides = remember(sections) {
        mutableStateListOf<SectionTimingOverride>().apply { addAll(SectionTimingPlanner.overridesFrom(sections)) }
    }
    var sectionCount by remember(sections) { mutableIntStateOf(sections.maxOfOrNull(SectionTemplate::index) ?: 12) }
    var globalField by remember { mutableStateOf<GlobalTimingField?>(null) }
    var editingSection by remember { mutableStateOf<PlannedSection?>(null) }
    val plan = SectionTimingPlanner.build(sectionCount, settings, overrides)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sections_global_settings), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimingSettingButton(
                label = stringResource(R.string.sections_lesson_duration),
                value = settings.lessonDurationMinutes,
                onClick = { globalField = GlobalTimingField.LESSON },
                modifier = Modifier.weight(1f),
            )
            TimingSettingButton(
                label = stringResource(R.string.sections_break_duration),
                value = settings.breakDurationMinutes,
                onClick = { globalField = GlobalTimingField.BREAK },
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.sections_count, sectionCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { sectionCount-- }, enabled = sectionCount > (overrides.maxOfOrNull { it.index } ?: 1)) {
                Text(stringResource(R.string.sections_remove_last))
            }
        }

        when (plan) {
            is SectionTimingPlanResult.Valid -> plan.sections.forEach { section ->
                SectionTimeRow(section = section, onClick = { editingSection = section })
            }
            is SectionTimingPlanResult.Invalid -> Text(
                text = plan.message,
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedButton(onClick = { sectionCount++ }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sections_add))
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sections_reset))
        }
        Button(
            onClick = {
                if (plan is SectionTimingPlanResult.Valid) onSave(plan.sections.map(PlannedSection::toTemplate))
            },
            enabled = plan is SectionTimingPlanResult.Valid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(DsR.string.action_save)) }
    }

    globalField?.let { field ->
        DurationDialog(
            title = stringResource(
                if (field == GlobalTimingField.LESSON) R.string.sections_lesson_duration else R.string.sections_break_duration,
            ),
            selectedMinutes = if (field == GlobalTimingField.LESSON) settings.lessonDurationMinutes else settings.breakDurationMinutes,
            minimum = if (field == GlobalTimingField.LESSON) 1 else 0,
            maximum = if (field == GlobalTimingField.LESSON) 180 else 360,
            onDismiss = { globalField = null },
            onConfirm = { value ->
                val proposedSettings = if (field == GlobalTimingField.LESSON) {
                    settings.copy(lessonDurationMinutes = value)
                } else {
                    settings.copy(breakDurationMinutes = value)
                }
                when (val proposedPlan = SectionTimingPlanner.build(sectionCount, proposedSettings, overrides)) {
                    is SectionTimingPlanResult.Valid -> {
                        settings = proposedSettings
                        globalField = null
                        null
                    }
                    is SectionTimingPlanResult.Invalid -> proposedPlan.message
                }
            },
        )
    }

    editingSection?.let { section ->
        SectionTimeDialog(
            section = section,
            isBreakpoint = section.index in overrides.map(SectionTimingOverride::index),
            onDismiss = { editingSection = null },
            onConfirm = { startMinutes, endMinutes ->
                val proposedSettings = settings.copy(lessonDurationMinutes = endMinutes - startMinutes)
                val proposedOverrides = SectionTimingPlanner.overridesAfterEdit(overrides, section, startMinutes)
                when (val proposedPlan = SectionTimingPlanner.build(sectionCount, proposedSettings, proposedOverrides)) {
                    is SectionTimingPlanResult.Valid -> {
                        settings = proposedSettings
                        overrides.clear()
                        overrides.addAll(proposedOverrides)
                        editingSection = null
                        null
                    }
                    is SectionTimingPlanResult.Invalid -> proposedPlan.message
                }
            },
            onRestoreAuto = {
                overrides.removeAll { it.index == section.index && section.index != 1 }
                editingSection = null
            },
        )
    }
}

@Composable
private fun TimingSettingButton(label: String, value: Int, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.sections_minutes, value))
        }
    }
}

@Composable
private fun SectionTimeRow(section: PlannedSection, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("第 ${section.index} 节", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClick) {
                Text("${formatMinutes(section.startMinutes)} – ${formatMinutes(section.endMinutes)}")
            }
        }
    }
}

@Composable
private fun DurationDialog(
    title: String,
    selectedMinutes: Int,
    minimum: Int,
    maximum: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> String?,
) {
    var selectedIndex by remember(selectedMinutes) { mutableIntStateOf((selectedMinutes - minimum).coerceIn(0, maximum - minimum)) }
    var validationError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledWheel(
                    label = title,
                    items = (minimum..maximum).map { "$it 分钟" },
                    selectedIndex = selectedIndex,
                    onSelectedChange = {
                        selectedIndex = it
                        validationError = null
                    },
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { validationError = onConfirm(selectedIndex + minimum) }) {
                Text(stringResource(DsR.string.action_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) } },
    )
}

@Composable
private fun SectionTimeDialog(
    section: PlannedSection,
    isBreakpoint: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> String?,
    onRestoreAuto: () -> Unit,
) {
    var startHour by remember(section) { mutableIntStateOf(section.startMinutes / 60) }
    var startMinute by remember(section) { mutableIntStateOf(section.startMinutes % 60) }
    var endHour by remember(section) { mutableIntStateOf(section.endMinutes / 60) }
    var endMinute by remember(section) { mutableIntStateOf(section.endMinutes % 60) }
    val start = startHour * 60 + startMinute
    val end = endHour * 60 + endMinute
    var validationError by remember(section) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sections_edit_time, section.index)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ClockWheelRow(
                    label = stringResource(R.string.sections_start),
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it; validationError = null },
                    onMinuteChange = { startMinute = it; validationError = null },
                )
                ClockWheelRow(
                    label = stringResource(R.string.sections_end),
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it; validationError = null },
                    onMinuteChange = { endMinute = it; validationError = null },
                )
                val error = validationError ?: if (end <= start) stringResource(R.string.sections_invalid, section.index) else null
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { validationError = onConfirm(start, end) },
                enabled = end > start,
            ) {
                Text(stringResource(DsR.string.action_confirm))
            }
        },
        dismissButton = {
            Row {
                if (isBreakpoint && section.index != 1) {
                    TextButton(onClick = onRestoreAuto) { Text(stringResource(R.string.sections_restore_auto)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun ClockWheelRow(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledWheel(
            label = stringResource(R.string.sections_hour),
            items = (0..23).map { "%02d".format(it) },
            selectedIndex = hour,
            onSelectedChange = onHourChange,
            modifier = Modifier.weight(1f),
        )
        LabeledWheel(
            label = stringResource(R.string.sections_minute),
            items = (0..59).map { "%02d".format(it) },
            selectedIndex = minute,
            onSelectedChange = onMinuteChange,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun PlannedSection.toTemplate(): SectionTemplate =
    SectionTemplate(termId = 0L, index = index, startMinutes = startMinutes, endMinutes = endMinutes)

private fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

private enum class GlobalTimingField { LESSON, BREAK }
