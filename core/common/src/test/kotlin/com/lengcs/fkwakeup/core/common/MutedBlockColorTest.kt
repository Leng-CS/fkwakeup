package com.lengcs.fkwakeup.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 已上完课块的灰化（#29）。
 *
 * 目标是「去饱和」而不是「降透明度」：颜色变成灰调，但仍保留一点点明度层次，
 * 让不同的课不至于糊成同一个色块。
 */
class MutedBlockColorTest {

    private fun luminance(argb: Int): Double {
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return 0.299 * r + 0.587 * g + 0.114 * b
    }

    @Test
    fun `alpha 原样保留`() {
        val muted = MutedBlockColor.mute(0x80FF0000.toInt())
        assertEquals(0x80, (muted ushr 24) and 0xFF)
    }

    @Test
    fun `高饱和红色灰化后色度大幅下降`() {
        val red = 0xFFFF0000.toInt()
        val muted = MutedBlockColor.mute(red)
        assertTrue(
            "原色色度 ${MutedBlockColor.chromaSpread(red)}，灰化后 ${MutedBlockColor.chromaSpread(muted)}",
            MutedBlockColor.chromaSpread(muted) < 50,
        )
    }

    @Test
    fun `蓝色与黄色灰化后都接近中性`() {
        listOf(0xFF0000FF.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt()).forEach { c ->
            assertTrue(
                "颜色 ${Integer.toHexString(c)} 灰化后色度仍为 ${MutedBlockColor.chromaSpread(MutedBlockColor.mute(c))}",
                MutedBlockColor.chromaSpread(MutedBlockColor.mute(c)) < 50,
            )
        }
    }

    @Test
    fun `保留明度层次_亮的原色灰化后仍然更亮`() {
        val green = 0xFF00FF00.toInt()   // 亮度高
        val blue = 0xFF0000FF.toInt()    // 亮度低
        val mutedGreen = MutedBlockColor.mute(green)
        val mutedBlue = MutedBlockColor.mute(blue)
        assertTrue(
            "绿→%.1f 应亮于 蓝→%.1f".format(luminance(mutedGreen), luminance(mutedBlue)),
            luminance(mutedGreen) > luminance(mutedBlue),
        )
    }

    @Test
    fun `已经是灰色时灰化结果几乎不变`() {
        val gray = 0xFF808080.toInt()
        val muted = MutedBlockColor.mute(gray)
        // 允许 due to LUMINANCE_SCALE 的轻微变暗，但不应该发生跳变
        assertTrue(
            "灰 → ${Integer.toHexString(muted)}",
            kotlin.math.abs(luminance(muted) - luminance(gray)) < 12,
        )
    }

    @Test
    fun `灰化结果比原色暗一点_让它退到背景`() {
        val c = 0xFF3366CC.toInt()
        assertTrue(luminance(MutedBlockColor.mute(c)) < luminance(c))
    }

    @Test
    fun `黑色与白色不会溢出`() {
        assertEquals(0xFF000000.toInt(), MutedBlockColor.mute(0xFF000000.toInt()))
        val white = MutedBlockColor.mute(0xFFFFFFFF.toInt())
        listOf(
            (white ushr 16) and 0xFF,
            (white ushr 8) and 0xFF,
            white and 0xFF,
        ).forEach { assertTrue("通道 $it 应在 0..255", it in 0..255) }
    }
}
