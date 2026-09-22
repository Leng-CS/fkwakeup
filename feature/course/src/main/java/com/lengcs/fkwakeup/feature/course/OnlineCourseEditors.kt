package com.lengcs.fkwakeup.feature.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.OnlineCourseTimeline
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.Term
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class EditableOnlineWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val platform: String? = null,
    val url: String? = null,
    val note: String? = null,
) {
    fun toDomain(courseId: Long): OnlineCourseWindow = OnlineCourseWindow(
        courseId = courseId,
        startDate = startDate,
        endDate = endDate,
        platform = platform?.trim()?.ifBlank { null },
        url = url?.trim()?.ifBlank { null },
        note = note?.trim()?.ifBlank { null },
    )

    companion object {
        fun fromDomain(window: OnlineCourseWindow) = EditableOnlineWindow(
            startDate = window.startDate,
            endDate = window.endDate,
            platform = window.platform,
            url = window.url,
            note = window.note,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun OnlineWindowEditor(
    index: Int,
    window: EditableOnlineWindow,
    term: Term?,
    onChange: (EditableOnlineWindow) -> Unit,
    onRemove: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    label = stringResource(R.string.edit_online_start),
                    date = window.startDate,
                    onDateSelected = { onChange(window.copy(startDate = it)) },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(R.string.edit_online_end),
                    date = window.endDate,
                    onDateSelected = { onChange(window.copy(endDate = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            val overlapMessage = term?.let { dateRangeMessage(window, it) }
            if (overlapMessage != null) {
                Text(
                    text = overlapMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = window.platform.orEmpty(),
                onValueChange = { onChange(window.copy(platform = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.edit_online_platform)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = window.url.orEmpty(),
                onValueChange = { onChange(window.copy(url = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.edit_online_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!window.url.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(window.url) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.edit_online_open)) }
            }
            OutlinedTextField(
                value = window.note.orEmpty(),
                onValueChange = { onChange(window.copy(note = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.edit_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_online_item, index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) { Text(stringResource(DsR.string.action_remove)) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DateField(
    label: String,
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(date.toString(), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (showPicker) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text(stringResource(DsR.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(DsR.string.action_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }
}

private fun dateRangeMessage(window: EditableOnlineWindow, term: Term): String? {
    if (window.endDate.isBefore(window.startDate)) return "结束日期不能早于开始日期"
    val domain = window.toDomain(0L)
    if (!OnlineCourseTimeline.overlapsTerm(domain, term)) return "开放期必须与当前学期至少重叠一天"
    val termEnd = OnlineCourseTimeline.termEnd(term)
    return if (window.startDate.isBefore(term.startMonday) || window.endDate.isAfter(termEnd)) {
        "部分日期位于学期范围外，仍可保存"
    } else {
        null
    }
}
