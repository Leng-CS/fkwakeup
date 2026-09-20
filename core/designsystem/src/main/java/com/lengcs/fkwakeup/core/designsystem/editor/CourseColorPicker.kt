package com.lengcs.fkwakeup.core.designsystem.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.common.CourseTextColor

/**
 * 课块颜色选择：12 个预设色 + 「自动」。
 *
 * 「自动」= 按课名哈希取色（也就是没自定义时的行为），选它可以改回自动。
 *
 * 颜色粒度是**课程**，不是时间段 —— 同一门课的所有时间段共用同一个色值。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseColorPicker(
    selectedArgb: Int?,
    previewName: String,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val autoColor = CourseColorPalette.pickArgb(previewName)

    // 13 个色块（12 预设 + 自动）一行放不下，用 FlowRow 换两行
    FlowRow(
        modifier = modifier.fillMaxWidth(),
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
    // 自定义色不一定是深色，按感知亮度自动切换黑/白文字
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
