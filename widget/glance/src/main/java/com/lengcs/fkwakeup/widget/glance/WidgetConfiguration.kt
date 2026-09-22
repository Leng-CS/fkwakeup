package com.lengcs.fkwakeup.widget.glance

import android.appwidget.AppWidgetManager

/** 每一个桌面小组件实例独立保存的显示配置。 */
data class WidgetConfig(
    val range: com.lengcs.fkwakeup.core.common.WidgetLessonRange =
        com.lengcs.fkwakeup.core.common.WidgetLessonRange.TODAY_REMAINING,
    val barMode: com.lengcs.fkwakeup.core.common.WidgetLessonBarMode =
        com.lengcs.fkwakeup.core.common.WidgetLessonBarMode.COURSE_COLOR,
    val backgroundType: WidgetBackgroundType = WidgetBackgroundType.SOLID,
    val backgroundStartArgb: Int = 0xFFF6F1FA.toInt(),
    val backgroundEndArgb: Int = 0xFFE7E0EC.toInt(),
    val backgroundAlpha: Float = 0.92f,
    val textAlpha: Float = 1f,
    val imagePreset: WidgetImagePreset = WidgetImagePreset.SKY,
    val imageUri: String? = null,
    val darkPreset: Boolean = false,
)

enum class WidgetBackgroundType { TRANSPARENT, SOLID, GRADIENT, PRESET_IMAGE, PHOTO }
enum class WidgetImagePreset { SKY, SUNSET, PAPER }

enum class WidgetSizeMode(val label: String) {
    FOUR_BY_ONE("4×1"),
    FOUR_BY_TWO("4×2"),
    FOUR_BY_FOUR("4×4");

    companion object {
        fun fromHeightDp(height: Int): WidgetSizeMode = when {
            height < 112 -> FOUR_BY_ONE
            height < 224 -> FOUR_BY_TWO
            else -> FOUR_BY_FOUR
        }
    }
}

data class WidgetInstance(
    val appWidgetId: Int,
    val config: WidgetConfig,
    val sizeMode: WidgetSizeMode,
)

internal fun AppWidgetManager.widgetHeightDp(id: Int): Int =
    getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56)
