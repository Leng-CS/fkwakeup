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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.PlannedSection
import com.lengcs.fkwakeup.core.common.SectionTimingPlanResult
import com.lengcs.fkwakeup.core.common.SectionTimingPlanner
import com.lengcs.fkwakeup.core.common.SectionTimingSegment
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
    val initialSegments = remember(sections) { SectionTimingPlanner.segmentsFrom(sections) }
    val segments = remember(sections) { mutableStateListOf<SectionTimingSegment>().apply { addAll(initialSegments) } }
    var sectionCount by remember(sections) { mutableIntStateOf(sections.size.coerceAtLeast(1)) }
    var showBreakpointDialog by remember { mutableStateOf(false) }
    val plan = SectionTimingPlanner.build(sectionCount, segments)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val canRemoveLast = sectionCount > 1 && sectionCount > (segments.maxOfOrNull { it.startIndex } ?: 1)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.sections_count, sectionCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { sectionCount-- }, enabled = canRemoveLast) {
                Text(stringResource(R.string.sections_remove_last))
            }
        }

        segments.sortedBy { it.startIndex }.forEachIndexed { position, segment ->
            key(segment.startIndex) {
                TimingSegmentCard(
                    number = position + 1,
                    segment = segment,
                    sectionCount = sectionCount,
                    otherStarts = segments.filterNot { it === segment }.map { it.startIndex }.toSet(),
                    isFirst = position == 0,
                    onChange = { replacement ->
                        val index = segments.indexOf(segment)
                        if (index >= 0) segments[index] = replacement
                    },
                    onRemove = { segments.remove(segment) },
                )
            }
        }

        OutlinedButton(
            onClick = { sectionCount++ },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.sections_add)) }

        OutlinedButton(
            onClick = { showBreakpointDialog = true },
            enabled = segments.size < sectionCount,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.sections_add_breakpoint)) }

        Text(
            text = stringResource(R.string.sections_preview),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        when (plan) {
            is SectionTimingPlanResult.Valid -> TimingPreview(plan.sections)
            is SectionTimingPlanResult.Invalid -> Text(
                text = plan.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sections_reset))
        }
        Button(
            onClick = {
                if (plan is SectionTimingPlanResult.Valid) {
                    onSave(plan.sections.map { it.toTemplate() })
                }
            },
            enabled = plan is SectionTimingPlanResult.Valid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(DsR.string.action_save)) }
    }

    if (showBreakpointDialog) {
        BreakpointDialog(
            sectionCount = sectionCount,
            occupiedStarts = segments.map { it.startIndex }.toSet(),
            plannedSections = (plan as? SectionTimingPlanResult.Valid)?.sections.orEmpty(),
            onDismiss = { showBreakpointDialog = false },
            onConfirm = { index, startMinutes ->
                val source = segments.filter { it.startIndex < index }.maxByOrNull { it.startIndex } ?: return@BreakpointDialog
                segments += SectionTimingSegment(
                    startIndex = index,
                    startMinutes = startMinutes,
                    lessonDurationMinutes = source.lessonDurationMinutes,
                    breakDurationMinutes = source.breakDurationMinutes,
                )
                showBreakpointDialog = false
            },
        )
    }
}

@Composable
private fun TimingSegmentCard(
    number: Int,
    segment: SectionTimingSegment,
    sectionCount: Int,
    otherStarts: Set<Int>,
    isFirst: Boolean,
    onChange: (SectionTimingSegment) -> Unit,
    onRemove: () -> Unit,
) {
    val durationItems = (1..180).map { stringResource(R.string.sections_minutes, it) }
    val breakItems = (0..360).map { stringResource(R.string.sections_minutes, it) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row {
                Text(stringResource(R.string.sections_segment, number), modifier = Modifier.weight(1f))
                if (!isFirst) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.sections_remove_breakpoint)) }
                }
            }
            if (isFirst) {
                Text(
                    text = stringResource(R.string.sections_start_section) + "：1",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                val availableStarts = (2..sectionCount).filter { it == segment.startIndex || it !in otherStarts }
                LabeledWheel(
                    label = stringResource(R.string.sections_start_section),
                    items = availableStarts.map(Int::toString),
                    selectedIndex = availableStarts.indexOf(segment.startIndex).coerceAtLeast(0),
                    onSelectedChange = { onChange(segment.copy(startIndex = availableStarts[it])) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledWheel(
                    label = stringResource(R.string.sections_hour),
                    items = (0..23).map { "%02d".format(it) },
                    selectedIndex = (segment.startMinutes / 60).coerceIn(0, 23),
                    onSelectedChange = { hour ->
                        onChange(segment.copy(startMinutes = hour * 60 + segment.startMinutes % 60))
                    },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = stringResource(R.string.sections_minute),
                    items = (0..59).map { "%02d".format(it) },
                    selectedIndex = (segment.startMinutes % 60).coerceIn(0, 59),
                    onSelectedChange = { minute ->
                        onChange(segment.copy(startMinutes = segment.startMinutes / 60 * 60 + minute))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledWheel(
                    label = stringResource(R.string.sections_lesson_duration),
                    items = durationItems,
                    selectedIndex = (segment.lessonDurationMinutes - 1).coerceIn(0, durationItems.lastIndex),
                    onSelectedChange = { onChange(segment.copy(lessonDurationMinutes = it + 1)) },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = stringResource(R.string.sections_break_duration),
                    items = breakItems,
                    selectedIndex = segment.breakDurationMinutes.coerceIn(0, breakItems.lastIndex),
                    onSelectedChange = { onChange(segment.copy(breakDurationMinutes = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TimingPreview(sections: List<PlannedSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        sections.forEach { section ->
            Text(
                text = "第 ${section.index} 节  ${formatMinutes(section.startMinutes)} — ${formatMinutes(section.endMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BreakpointDialog(
    sectionCount: Int,
    occupiedStarts: Set<Int>,
    plannedSections: List<PlannedSection>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val options = remember(sectionCount, occupiedStarts) { (2..sectionCount).filterNot(occupiedStarts::contains) }
    var selectedIndex by remember(options) { mutableIntStateOf(0) }
    val selectedSection = options.getOrElse(selectedIndex) { 2 }
    val suggestedStart = plannedSections.firstOrNull { it.index == selectedSection }?.startMinutes ?: 8 * 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sections_breakpoint_title)) },
        text = {
            Column {
                Text(stringResource(R.string.sections_breakpoint_hint))
                LabeledWheel(
                    label = stringResource(R.string.sections_start_section),
                    items = options.map { "第 $it 节" },
                    selectedIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                    onSelectedChange = { selectedIndex = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSection, suggestedStart) }) {
                Text(stringResource(DsR.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(DsR.string.action_cancel)) }
        },
    )
}

private fun PlannedSection.toTemplate(): SectionTemplate =
    SectionTemplate(termId = 0L, index = index, startMinutes = startMinutes, endMinutes = endMinutes)

private fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
