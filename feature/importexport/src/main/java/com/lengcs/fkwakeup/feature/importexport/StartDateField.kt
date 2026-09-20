package com.lengcs.fkwakeup.feature.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 学期起始日的展示 + 日期选择（#22）。
 *
 * 起始日决定「今天是第几周」，AI 认错一周会让周视图高亮、已结束判定与桌面小组件**全部跟着错**，
 * 所以必须在落库前能改。
 *
 * 选中的日期会被调用方**对齐到周一** —— `startMonday` 的语义就是「第一周的周一」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateField(
    dateText: String?,
    onPick: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dateText?.let { stringResource(R.string.preview_start_date, it) }
                ?: stringResource(R.string.preview_start_date_unknown),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { showPicker = true }) {
            Text(stringResource(R.string.preview_pick_date))
        }
    }

    if (showPicker) {
        val initialMillis = dateText?.let {
            runCatching {
                LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker 给的是 UTC 毫秒，按 UTC 还原成日期，避免时区把日期挪一天
                    pickerState.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
