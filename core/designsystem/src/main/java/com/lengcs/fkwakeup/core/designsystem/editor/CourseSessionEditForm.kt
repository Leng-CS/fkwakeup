package com.lengcs.fkwakeup.core.designsystem.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.designsystem.R
import com.lengcs.fkwakeup.core.designsystem.picker.LabeledWheel
import com.lengcs.fkwakeup.core.designsystem.picker.WeekPicker

/**
 * 「课程信息 + 本节课」编辑表单的全部可编辑状态。
 *
 * 纯数据、不依赖 Compose 运行时，便于调用方持有与比较。
 *
 * 注意**作用范围不同**：
 * - `name` / `teacher` / `colorArgb` 是**课程级**，改了影响该课的所有时间段
 * - `dayOfWeek` / 起止节 / `weeks` / `location` / `note` 是**时间段级**，只影响当前这一条
 */
data class CourseSessionEditState(
    val name: String = "",
    val teacher: String = "",
    val colorArgb: Int? = null,
    val dayOfWeek: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val weeks: Set<Int> = emptySet(),
    val location: String = "",
    val note: String = "",
)

private val WEEKDAY_KEYS = listOf(
    R.string.weekday_1,
    R.string.weekday_2,
    R.string.weekday_3,
    R.string.weekday_4,
    R.string.weekday_5,
    R.string.weekday_6,
    R.string.weekday_7,
)

/**
 * 课程 / 时间段编辑表单的**主体**（不做抽屉包裹、不含动作按钮）。
 *
 * 由 `feature:schedule`（周视图抽屉）与 `feature:importexport`（导入预览抽屉）共用，
 * 两边的差异只在数据源与动作按钮，字段与交互必须一致 —— 否则用户会困惑
 * 「为什么这里要点选、那里要手填」。
 *
 * 状态由调用方持有（单向数据流），本组件不做任何校验与副作用。
 */
@Composable
fun CourseSessionEditForm(
    state: CourseSessionEditState,
    onStateChange: (CourseSessionEditState) -> Unit,
    totalWeeks: Int,
    sectionCount: Int,
    modifier: Modifier = Modifier,
) {
    val sectionItems = rememberSectionItems(sectionCount)
    val weekdayItems = WEEKDAY_KEYS.map { stringResource(it) }
    val safeSectionCount = sectionCount.coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---------- 课程信息（作用于该课全部时间段）----------
        Text(
            text = stringResource(R.string.sheet_course_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = { onStateChange(state.copy(name = it)) },
            label = { Text(stringResource(R.string.sheet_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.teacher,
            onValueChange = { onStateChange(state.copy(teacher = it)) },
            label = { Text(stringResource(R.string.sheet_teacher)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.sheet_color), style = MaterialTheme.typography.labelLarge)
        CourseColorPicker(
            selectedArgb = state.colorArgb,
            previewName = state.name,
            onSelect = { onStateChange(state.copy(colorArgb = it)) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // ---------- 本节课（只作用于当前这一条）----------
        Text(
            text = stringResource(R.string.sheet_this_session),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LabeledWheel(
                label = stringResource(R.string.sheet_day),
                items = weekdayItems,
                selectedIndex = (state.dayOfWeek - 1).coerceIn(0, 6),
                onSelectedChange = { onStateChange(state.copy(dayOfWeek = it + 1)) },
                modifier = Modifier.weight(1f),
            )
            LabeledWheel(
                label = stringResource(R.string.sheet_start),
                items = sectionItems,
                selectedIndex = (state.startSection - 1).coerceIn(0, safeSectionCount - 1),
                onSelectedChange = { onStateChange(state.copy(startSection = it + 1)) },
                modifier = Modifier.weight(1f),
            )
            LabeledWheel(
                label = stringResource(R.string.sheet_end),
                items = sectionItems,
                selectedIndex = (state.endSection - 1).coerceIn(0, safeSectionCount - 1),
                onSelectedChange = { onStateChange(state.copy(endSection = it + 1)) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(stringResource(R.string.sheet_weeks), style = MaterialTheme.typography.labelLarge)
        WeekPicker(
            selectedWeeks = state.weeks,
            totalWeeks = totalWeeks,
            onToggleWeek = { week ->
                val next = if (week in state.weeks) state.weeks - week else state.weeks + week
                onStateChange(state.copy(weeks = next))
            },
            onSelectAll = { onStateChange(state.copy(weeks = (1..totalWeeks).toSet())) },
            onSelectOdd = {
                onStateChange(state.copy(weeks = (1..totalWeeks).filter { it % 2 == 1 }.toSet()))
            },
            onSelectEven = {
                onStateChange(state.copy(weeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()))
            },
            onClear = { onStateChange(state.copy(weeks = emptySet())) },
        )

        OutlinedTextField(
            value = state.location,
            onValueChange = { onStateChange(state.copy(location = it)) },
            label = { Text(stringResource(R.string.sheet_location)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = { onStateChange(state.copy(note = it)) },
            label = { Text(stringResource(R.string.sheet_note)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberSectionItems(sectionCount: Int): List<String> {
    val n = sectionCount.coerceAtLeast(1)
    return androidx.compose.runtime.remember(n) { (1..n).map { it.toString() } }
}
