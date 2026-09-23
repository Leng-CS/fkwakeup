package com.lengcs.fkwakeup.feature.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnlineWindowEditSheet(
    courseName: String,
    courseTeacher: String?,
    window: OnlineWindowDraft,
    onSave: (String, String?, OnlineWindowDraft) -> Unit,
    onConvertToLive: () -> Unit,
    onDismiss: () -> Unit,
) {
    var startDate by remember(window) { mutableStateOf(window.startDate) }
    var endDate by remember(window) { mutableStateOf(window.endDate) }
    var name by remember(window) { mutableStateOf(courseName) }
    var teacher by remember(window) { mutableStateOf(courseTeacher.orEmpty()) }
    var platform by remember(window) { mutableStateOf(window.platform.orEmpty()) }
    var url by remember(window) { mutableStateOf(window.url.orEmpty()) }
    var note by remember(window) { mutableStateOf(window.note.orEmpty()) }
    val valid = name.isNotBlank() && startDate?.let { start -> endDate?.let { end -> !end.isBefore(start) } } == true

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.preview_online_edit_title))
            Text(stringResource(R.string.preview_online_edit_hint))
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.preview_course_name)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(teacher, { teacher = it }, label = { Text(stringResource(R.string.preview_course_teacher)) }, modifier = Modifier.fillMaxWidth())
            ImportDatePickerField(stringResource(R.string.preview_online_start), startDate) { startDate = it }
            ImportDatePickerField(stringResource(R.string.preview_online_end), endDate) { endDate = it }
            OutlinedTextField(platform, { platform = it }, label = { Text(stringResource(R.string.preview_online_platform)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.preview_online_url)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.preview_online_note)) }, modifier = Modifier.fillMaxWidth())
            if (!valid) Text(stringResource(R.string.preview_online_invalid))
            Button(
                onClick = {
                    onSave(name.trim(), teacher.trim().ifBlank { null }, window.copy(
                        startDate = startDate, endDate = endDate,
                        platform = platform.trim().ifBlank { null }, url = url.trim().ifBlank { null },
                        note = note.replace("待确认授课方式", "").trim().ifBlank { null },
                    ))
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.preview_online_save)) }
            OutlinedButton(onClick = onConvertToLive, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.preview_online_to_live))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDatePickerField(label: String, date: LocalDate?, onPick: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.preview_online_date_value, label, date?.toString() ?: stringResource(R.string.preview_online_missing)))
    }
    if (show) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text(stringResource(DsR.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(stringResource(DsR.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}
