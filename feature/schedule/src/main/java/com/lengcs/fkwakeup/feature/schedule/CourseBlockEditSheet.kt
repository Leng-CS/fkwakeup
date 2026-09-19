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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.common.CourseTextColor
import com.lengcs.fkwakeup.core.common.ScheduleBlock
import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.designsystem.picker.LabeledWheel
import com.lengcs.fkwakeup.core.designsystem.picker.WeekPicker

/**
 * 周视图里点击课程块弹出的编辑抽屉。
 *
 * 分两段：
 * 1. 课程信息（名称 / 教师 / 颜色）—— 改了会影响这门课的所有时间段
 * 2. 本节课（周几 / 节次 / 周次 / 地点）—— 只影响当前这一节
 *
 * 操作按钮在**右上角**（保存 / 删除 / 取消），与下方输入区分离。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseBlockEditSheet(
    block: ScheduleBlock,
    totalWeeks: Int,
    sectionCount: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        teacher: String,
        colorArgb: Int?,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        weeks: Set<Int>,
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
    var selectedWeeks by remember(block.session.id) {
        mutableStateOf(
            runCatching { WeekSpecParser.parse(block.session.weekSpec, totalWeeks) }
                .getOrDefault(emptySet())
        )
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
            // 操作区：左侧标题、右侧图标按钮。
            // 用 Box 左右对齐而不是 Row + weight —— 实测 weight 在这里没把剩余宽度让出来，
            // 导致图标被压成 0 宽而完全不显示。
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(
                    onClick = {
                    onSave(
                        name.text,
                        teacher.text,
                        colorArgb,
                        dayOfWeek,
                        startSection,
                        endSection,
                        selectedWeeks,
                        location.text,
                    )
                }) {
                    Icon(
                        painter = painterResource(com.lengcs.fkwakeup.core.designsystem.R.drawable.ic_save),
                        contentDescription = stringResource(R.string.sheet_save),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        painter = painterResource(com.lengcs.fkwakeup.core.designsystem.R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.sheet_delete_session),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(com.lengcs.fkwakeup.core.designsystem.R.drawable.ic_close),
                        contentDescription = stringResource(R.string.sheet_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                } // 关闭图标 Row
            } // 关闭操作区 Box

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

            // ---- 2. 本节课：滚轮 ----
            Text(
                stringResource(R.string.sheet_this_session),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LabeledWheel(
                    label = stringResource(R.string.sheet_day),
                    items = WEEKDAY_ITEMS,
                    selectedIndex = (dayOfWeek - 1).coerceIn(0, 6),
                    onSelectedChange = { dayOfWeek = it + 1 },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = stringResource(R.string.sheet_start),
                    items = sectionItems(sectionCount),
                    selectedIndex = (startSection - 1).coerceIn(0, sectionCount - 1),
                    onSelectedChange = { startSection = it + 1 },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = stringResource(R.string.sheet_end),
                    items = sectionItems(sectionCount),
                    selectedIndex = (endSection - 1).coerceIn(0, sectionCount - 1),
                    onSelectedChange = { endSection = it + 1 },
                    modifier = Modifier.weight(1f),
                )
            }

            // ---- 周次：点选，不再手填表达式 ----
            Text(stringResource(R.string.sheet_weeks), style = MaterialTheme.typography.labelLarge)
            WeekPicker(
                selectedWeeks = selectedWeeks,
                totalWeeks = totalWeeks,
                onToggleWeek = { week ->
                    selectedWeeks = if (week in selectedWeeks) {
                        selectedWeeks - week
                    } else {
                        selectedWeeks + week
                    }
                },
                onSelectAll = { selectedWeeks = (1..totalWeeks).toSet() },
                onSelectOdd = { selectedWeeks = (1..totalWeeks).filter { it % 2 == 1 }.toSet() },
                onSelectEven = { selectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet() },
                onClear = { selectedWeeks = emptySet() },
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.sheet_location)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
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

private fun sectionItems(sectionCount: Int): List<String> =
    (1..sectionCount.coerceAtLeast(1)).map { "$it" }

private val WEEKDAY_ITEMS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

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

    // 13 个色块（12 预设 + 自动）一行放不下，用 FlowRow 自动换行
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 7,
    ) {
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
            Text("A", style = MaterialTheme.typography.labelSmall, color = textColor)
        } else if (selected) {
            Text("✓", style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}
