package com.lengcs.fkwakeup.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.common.CourseTextColor
import com.lengcs.fkwakeup.core.common.ScheduleBlock

/**
 * 周视图里点击课程块弹出的编辑抽屉。
 *
 * 分两段：
 * 1. 课程信息（名称 / 教师 / 颜色）—— 改了会影响这门课的所有时间段
 * 2. 本节课（周几 / 节次 / 周次 / 地点）—— 只影响当前这一节
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseBlockEditSheet(
    block: ScheduleBlock,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        teacher: String,
        colorArgb: Int?,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        weekSpec: String,
        location: String?,
    ) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(block.course.id) { mutableStateOf(TextFieldValue(block.course.name)) }
    var teacher by remember(block.course.id) {
        mutableStateOf(TextFieldValue(block.course.teacher.orEmpty()))
    }
    var colorArgb by remember(block.course.id) { mutableStateOf(block.course.colorArgb) }

    var dayOfWeek by remember(block.session.id) { mutableStateOf(block.session.dayOfWeek) }
    var startSection by remember(block.session.id) { mutableStateOf(block.session.startSection) }
    var endSection by remember(block.session.id) { mutableStateOf(block.session.endSection) }
    var weekSpec by remember(block.session.id) {
        mutableStateOf(TextFieldValue(block.session.weekSpec))
    }
    var location by remember(block.session.id) {
        mutableStateOf(TextFieldValue(block.session.location.orEmpty()))
    }

    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.sheet_title), style = MaterialTheme.typography.titleMedium)

            // ---- 1. 课程信息 ----
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.sheet_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text(stringResource(R.string.sheet_teacher)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.sheet_color), style = MaterialTheme.typography.labelLarge)
            ColorPickerRow(
                selectedArgb = colorArgb,
                previewName = name.text,
                onSelect = { colorArgb = it },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ---- 2. 本节课 ----
            Text(
                stringResource(R.string.sheet_this_session),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = stringResource(R.string.sheet_day),
                    value = dayOfWeek,
                    onValueChange = { dayOfWeek = it },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = stringResource(R.string.sheet_start),
                    value = startSection,
                    onValueChange = { startSection = it },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    label = stringResource(R.string.sheet_end),
                    value = endSection,
                    onValueChange = { endSection = it },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = weekSpec,
                onValueChange = { weekSpec = it },
                label = { Text(stringResource(R.string.sheet_weeks)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.sheet_location)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        name.text,
                        teacher.text,
                        colorArgb,
                        dayOfWeek,
                        startSection,
                        endSection,
                        weekSpec.text,
                        location.text,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sheet_save))
            }

            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sheet_delete_session))
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sheet_cancel))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.sheet_delete_title)) },
            text = { Text(stringResource(R.string.sheet_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * 12 个预设色 + 「自动」。
 *
 * 「自动」= 按课名哈希取色（也就是没自定义时的行为），选它可以改回自动。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerRow(
    selectedArgb: Int?,
    previewName: String,
    onSelect: (Int?) -> Unit,
) {
    val autoColor = CourseColorPalette.pickArgb(previewName)

    // 13 个色块（12 预设 + 自动）一行放不下，用 FlowRow 自动换到第二行
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 7,
    ) {
        // 自动
        ColorSwatch(
            argb = autoColor,
            selected = selectedArgb == null,
            isAuto = true,
            onClick = { onSelect(null) },
        )

        CourseColorPalette.colors.forEach { argb ->
            ColorSwatch(
                argb = argb,
                selected = selectedArgb == argb,
                isAuto = false,
                onClick = { onSelect(argb) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    argb: Int,
    selected: Boolean,
    isAuto: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(argb)
    val textColor = if (CourseTextColor.shouldUseDarkText(argb)) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(30.dp)
            .background(color, shape = MaterialTheme.shapes.small)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isAuto) {
            Text(
                text = "A",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        } else if (selected) {
            // 选中的色块用对勾标记
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw -> raw.toIntOrNull()?.let(onValueChange) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = modifier,
    )
}
