package com.lengcs.fkwakeup.widget.glance

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.ImageProvider
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import kotlin.math.roundToInt

internal fun WidgetConfig.backgroundModifier(): GlanceModifier = when (backgroundType) {
    WidgetBackgroundType.TRANSPARENT -> GlanceModifier.background(ColorProvider(Color.Transparent))
    WidgetBackgroundType.SOLID -> GlanceModifier.background(ColorProvider(backgroundColor(backgroundStartArgb)))
    WidgetBackgroundType.GRADIENT -> GlanceModifier.background(gradientProvider())
    WidgetBackgroundType.PRESET_IMAGE -> GlanceModifier.background(presetProvider())
    WidgetBackgroundType.PHOTO -> imageUri?.let { uri ->
        GlanceModifier.background(ImageProvider(Uri.parse(uri)))
    } ?: GlanceModifier.background(ColorProvider(backgroundColor(backgroundStartArgb)))
}

internal fun WidgetConfig.cardColor(): Color = backgroundColor(backgroundStartArgb).copy(
    alpha = (backgroundAlpha * 0.18f).coerceIn(0.03f, 0.28f),
)

internal fun WidgetConfig.primaryTextColor(): ColorProvider =
    ColorProvider(if (darkPreset) Color(0xFFF5F0F7).copy(alpha = textAlpha) else Color(0xFF1C1B1F).copy(alpha = textAlpha))

internal fun WidgetConfig.secondaryTextColor(): ColorProvider =
    ColorProvider(if (darkPreset) Color(0xFFD8D0DC).copy(alpha = textAlpha) else Color(0xFF49454F).copy(alpha = textAlpha))

private fun WidgetConfig.backgroundColor(argb: Int): Color {
    val base = Color(argb)
    val adjusted = if (darkPreset) base.copy(
        red = base.red * 0.38f,
        green = base.green * 0.38f,
        blue = base.blue * 0.38f,
    ) else base
    return adjusted.copy(alpha = backgroundAlpha)
}


private fun WidgetConfig.presetProvider(): ImageProvider = AndroidResourceImageProvider(
    when (imagePreset) {
        WidgetImagePreset.SKY -> R.drawable.widget_background_sky
        WidgetImagePreset.SUNSET -> R.drawable.widget_background_sunset
        WidgetImagePreset.PAPER -> R.drawable.widget_background_paper
    },
)

/** 用像素数组生成可配置渐变，避免在 Glance 小组件中使用 Canvas。 */
private fun WidgetConfig.gradientProvider(): ImageProvider {
    val width = 64
    val height = 64
    val pixels = IntArray(width * height)
    val from = backgroundColor(backgroundStartArgb).value.toInt()
    val to = backgroundColor(backgroundEndArgb).value.toInt()
    repeat(height) { y ->
        val ratio = y.toFloat() / (height - 1)
        val color = blendArgb(from, to, ratio)
        repeat(width) { x -> pixels[y * width + x] = color }
    }
    return BitmapImageProvider(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888))
}

private fun blendArgb(from: Int, to: Int, ratio: Float): Int {
    fun channel(shift: Int) = (((from ushr shift) and 0xFF) * (1f - ratio) +
        ((to ushr shift) and 0xFF) * ratio).roundToInt()
    return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
