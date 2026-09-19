package com.lengcs.fkwakeup.core.common

import kotlin.math.absoluteValue

/**
 * 课程色板。**纯 Kotlin，只存 ARGB 整数** ——
 *
 * 这样周视图、桌面小组件、以及未来任何 UI 拿到的都是同一个色值，
 * 不会出现「App 里一个色、小组件里另一个色」的不一致。
 * Compose 层只负责 `Color(argb)` 的包装（见 designsystem 的 CoursePalette）。
 */
object CourseColorPalette {

    /** 12 个预设色。都是中等饱和度的实色，白字在上面可读 */
    val colors: List<Int> = listOf(
        0xFFE8536F.toInt(), // 玫红
        0xFFE88D53.toInt(), // 橙
        0xFFD9B23C.toInt(), // 金黄
        0xFF7CB342.toInt(), // 绿
        0xFF38A3A5.toInt(), // 青
        0xFF4E86D4.toInt(), // 蓝
        0xFF7A6BD8.toInt(), // 紫
        0xFFB06BC0.toInt(), // 品紫
        0xFFC05B85.toInt(), // 桃红
        0xFF6B8EA9.toInt(), // 灰蓝
        0xFF8D6E63.toInt(), // 棕
        0xFF009688.toInt(), // 墨绿
    )

    /**
     * 自动取色只在前 [AUTO_COLOR_COUNT] 个色里轮转。
     *
     * **这个常量不能跟着 colors.size 走** —— 否则以后往色板里加一个颜色，
     * 所有已存在的「自动色」课程会整体变色（模数变了，映射全错位）。
     * 曾经从 10 色扩到 12 色时踩过这个坑：高等数学A 从金黄变成了玫红。
     */
    private const val AUTO_COLOR_COUNT = 10

    /** 按课程名哈希取色：同名课程始终同色 */
    fun pickArgb(name: String): Int {
        val index = name.trim().lowercase().hashCode().absoluteValue % AUTO_COLOR_COUNT
        return colors[index]
    }

    /**
     * 解析出最终使用的颜色：用户自定义优先，否则按课名哈希。
     *
     * @param customArgb 用户自定义色，null 表示「自动」
     */
    fun resolve(customArgb: Int?, name: String): Int = customArgb ?: pickArgb(name)
}

/**
 * 判断某个颜色上应该用深色文字还是浅色文字。
 *
 * 用户自定义颜色后无法保证都是深色，白字在浅色块上会看不清，所以需要按亮度自动切换。
 */
object CourseTextColor {

    /**
     * @return true 表示这个背景色较亮，应该用深色文字
     */
    fun shouldUseDarkText(backgroundArgb: Int): Boolean {
        val r = (backgroundArgb shr 16) and 0xFF
        val g = (backgroundArgb shr 8) and 0xFF
        val b = backgroundArgb and 0xFF
        // 感知亮度（ITU-R BT.601）
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luminance > 0.6
    }
}
