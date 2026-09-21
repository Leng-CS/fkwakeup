package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import kotlin.math.roundToInt

internal fun WidgetConfig.backgroundModifier(imageBackground: ImageProvider?): GlanceModifier = when (backgroundType) {
    WidgetBackgroundType.TRANSPARENT -> GlanceModifier.background(ColorProvider(Color.Transparent))
    WidgetBackgroundType.SOLID -> GlanceModifier.background(ColorProvider(backgroundColor(backgroundStartArgb)))
    WidgetBackgroundType.GRADIENT -> GlanceModifier.background(gradientProvider(backgroundStartArgb, backgroundEndArgb))
    WidgetBackgroundType.PRESET_IMAGE, WidgetBackgroundType.PHOTO -> imageBackground?.let(GlanceModifier::background)
        ?: GlanceModifier.background(ColorProvider(backgroundColor(backgroundStartArgb)))
}

/** 在 Glance 组合前读取图片并压缩，避免 Composable 内部 I/O。 */
internal fun WidgetConfig.imageBackground(context: Context): ImageProvider? = when (backgroundType) {
    WidgetBackgroundType.PRESET_IMAGE -> presetProvider()
    WidgetBackgroundType.PHOTO -> imageUri?.let { uri ->
        context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream)
            ?.let(::scaledWithOpacity)?.let(::BitmapImageProvider)
    }
    else -> null
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
    val adjusted = if (darkPreset) base.copy(red = base.red * .38f, green = base.green * .38f, blue = base.blue * .38f) else base
    return adjusted.copy(alpha = backgroundAlpha)
}

private fun WidgetConfig.presetProvider(): ImageProvider = when (imagePreset) {
    WidgetImagePreset.SKY -> gradientProvider(0xFFCFE8FF.toInt(), 0xFF7398CE.toInt())
    WidgetImagePreset.SUNSET -> gradientProvider(0xFFFFD3B3.toInt(), 0xFFC87492.toInt())
    WidgetImagePreset.PAPER -> gradientProvider(0xFFFFFCF2.toInt(), 0xFFE1D7B9.toInt())
}

/** 用像素数组生成渐变，避免在 Glance 小组件中使用 Canvas。 */
private fun WidgetConfig.gradientProvider(from: Int, to: Int): ImageProvider =
    BitmapImageProvider(createGradient(from, to, backgroundAlpha))

private fun createGradient(from: Int, to: Int, opacity: Float): Bitmap {
    val width = 64
    val height = 64
    val pixels = IntArray(width * height)
    repeat(height) { y ->
        val color = blendArgb(from, to, y.toFloat() / (height - 1), opacity)
        repeat(width) { x -> pixels[y * width + x] = color }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private fun WidgetConfig.scaledWithOpacity(source: Bitmap): Bitmap {
    val scaled = Bitmap.createScaledBitmap(source, 96, 96, true)
    val pixels = IntArray(96 * 96)
    scaled.getPixels(pixels, 0, 96, 0, 0, 96, 96)
    val alpha = backgroundAlpha.coerceIn(0f, 1f)
    pixels.indices.forEach { index ->
        val originalAlpha = pixels[index] ushr 24
        pixels[index] = (originalAlpha * alpha).roundToInt().shl(24) or (pixels[index] and 0x00FFFFFF)
    }
    return Bitmap.createBitmap(pixels, 96, 96, Bitmap.Config.ARGB_8888)
}

private fun blendArgb(from: Int, to: Int, ratio: Float, opacity: Float): Int {
    fun channel(shift: Int) = (((from ushr shift) and 0xFF) * (1f - ratio) + ((to ushr shift) and 0xFF) * ratio).roundToInt()
    return ((255 * opacity).roundToInt() shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
