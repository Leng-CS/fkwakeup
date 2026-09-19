package com.lengcs.fkwakeup.core.designsystem.palette

import androidx.compose.ui.graphics.Color
import com.lengcs.fkwakeup.core.common.CourseColorPalette

/**
 * 课程调色板。课程未指定颜色时，按名称哈希取色，保证同名课程颜色稳定。
 *
 * **色值本身定义在 core:common 的 [CourseColorPalette]（纯整数）**，这里只做 Compose 的 `Color` 包装 ——
 * 目的是让周视图与桌面小组件拿到完全一致的色值。
 */
object CoursePalette {

    /** 12 个预设色 */
    val colors: List<Color> = CourseColorPalette.colors.map { Color(it) }

    /** 按名称哈希取色 */
    fun pickFor(name: String): Color = Color(CourseColorPalette.pickArgb(name))

    /** 自定义色优先，否则按名称哈希 */
    fun resolve(customArgb: Int?, name: String): Color =
        Color(CourseColorPalette.resolve(customArgb, name))
}
