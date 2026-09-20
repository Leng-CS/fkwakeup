package com.lengcs.fkwakeup.core.common

import kotlin.math.roundToInt

/**
 * 已上完的课块配色（#29）。
 *
 * 与「降低不透明度」不同，这里是**去饱和变灰**：把颜色朝它自身的亮度收敛，
 * 只保留少量色相。效果是整体一眼看过去是灰的，但不同课程仍留有一点点明度差异，
 * 不至于所有过去的课糊成同一个色块。
 *
 * 纯整数运算，便于单测；Compose 层只做 `Color(argb)` 包装。
 */
object MutedBlockColor {

    /** 保留的色相比例：0 = 纯灰，1 = 原色 */
    const val KEEP_CHROMA: Float = 0.15f

    /** 额外的亮度衰减，让已上完的课退到背景里 */
    const val LUMINANCE_SCALE: Float = 0.94f

    /**
     * 把课块颜色转成「已上完」的灰调。
     *
     * alpha 原样保留（周视图里它可能已经被别处调过）。
     */
    fun mute(argb: Int): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        // Rec.601 亮度 —— 它就是「这个颜色对应的灰」的明度
        val lum = (0.299 * r + 0.587 * g + 0.114 * b) * LUMINANCE_SCALE

        fun channel(c: Int): Int =
            (lum + (c - lum) * KEEP_CHROMA).roundToInt().coerceIn(0, 255)

        return (a shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }

    /** 采样出的饱和度近似值（最大通道 − 最小通道），单测用它断言「确实变灰了」 */
    fun chromaSpread(argb: Int): Int {
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return maxOf(r, g, b) - minOf(r, g, b)
    }
}
