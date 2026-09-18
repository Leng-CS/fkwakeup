package com.lengcs.fkwakeup.core.designsystem.palette

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

/**
 * 课程调色板。课程未指定颜色时，按名称哈希取色，保证同名课程颜色稳定。
 */
object CoursePalette {

    val colors = listOf(
        Color(0xFFE8536F),
        Color(0xFFE88D53),
        Color(0xFFD9B23C),
        Color(0xFF7CB342),
        Color(0xFF38A3A5),
        Color(0xFF4E86D4),
        Color(0xFF7A6BD8),
        Color(0xFFB06BC0),
        Color(0xFFC05B85),
        Color(0xFF6B8EA9),
    )

    fun pickFor(name: String): Color =
        colors[name.trim().lowercase().hashCode().absoluteValue % colors.size]
}
