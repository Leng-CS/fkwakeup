package com.lengcs.fkwakeup.widget.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lengcs.fkwakeup.core.common.WidgetLessonBarMode
import com.lengcs.fkwakeup.core.common.WidgetLessonRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_settings",
)

/**
 * AppWidgetId 是 launcher 为每一份桌面实例分配的稳定标识；所有外观和范围均以它为键。
 * 删除组件后设置会延迟清理，不会影响后续新实例的默认值。
 */
@Singleton
class WidgetSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun config(appWidgetId: Int): WidgetConfig = configFrom(
        prefs = context.widgetSettingsStore.data.first(),
        id = appWidgetId,
    )

    suspend fun save(appWidgetId: Int, config: WidgetConfig) {
        context.widgetSettingsStore.edit { prefs ->
            prefs[rangeKey(appWidgetId)] = config.range.name
            prefs[barModeKey(appWidgetId)] = config.barMode.name
            prefs[backgroundTypeKey(appWidgetId)] = config.backgroundType.name
            prefs[startColorKey(appWidgetId)] = config.backgroundStartArgb
            prefs[endColorKey(appWidgetId)] = config.backgroundEndArgb
            prefs[backgroundAlphaKey(appWidgetId)] = config.backgroundAlpha
            prefs[textAlphaKey(appWidgetId)] = config.textAlpha
            prefs[imagePresetKey(appWidgetId)] = config.imagePreset.name
            config.imageUri?.let { prefs[imageUriKey(appWidgetId)] = it }
                ?: prefs.remove(imageUriKey(appWidgetId))
            prefs[darkPresetKey(appWidgetId)] = if (config.darkPreset) 1 else 0
        }
    }

    suspend fun instances(): List<WidgetInstance> {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, FkwakeupWidgetReceiver::class.java))
        val prefs = context.widgetSettingsStore.data.first()
        return ids.sorted().map { id ->
            WidgetInstance(
                appWidgetId = id,
                config = configFrom(prefs, id),
                sizeMode = WidgetSizeMode.fromHeightDp(manager.widgetHeightDp(id)),
            )
        }
    }

    private fun configFrom(prefs: Preferences, id: Int): WidgetConfig = WidgetConfig(
        range = enumValue(prefs[rangeKey(id)], WidgetLessonRange.TODAY_REMAINING),
        barMode = enumValue(prefs[barModeKey(id)], WidgetLessonBarMode.COURSE_COLOR),
        backgroundType = enumValue(prefs[backgroundTypeKey(id)], WidgetBackgroundType.SOLID),
        backgroundStartArgb = prefs[startColorKey(id)] ?: 0xFFF6F1FA.toInt(),
        backgroundEndArgb = prefs[endColorKey(id)] ?: 0xFFE7E0EC.toInt(),
        backgroundAlpha = prefs[backgroundAlphaKey(id)] ?: 0.92f,
        textAlpha = prefs[textAlphaKey(id)] ?: 1f,
        imagePreset = enumValue(prefs[imagePresetKey(id)], WidgetImagePreset.SKY),
        imageUri = prefs[imageUriKey(id)],
        darkPreset = (prefs[darkPresetKey(id)] ?: 0) == 1,
    )

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, default: T): T =
        raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private fun rangeKey(id: Int) = stringPreferencesKey("widget_${id}_range")
    private fun barModeKey(id: Int) = stringPreferencesKey("widget_${id}_bar_mode")
    private fun backgroundTypeKey(id: Int) = stringPreferencesKey("widget_${id}_background_type")
    private fun startColorKey(id: Int) = intPreferencesKey("widget_${id}_start_color")
    private fun endColorKey(id: Int) = intPreferencesKey("widget_${id}_end_color")
    private fun backgroundAlphaKey(id: Int) = floatPreferencesKey("widget_${id}_background_alpha")
    private fun textAlphaKey(id: Int) = floatPreferencesKey("widget_${id}_text_alpha")
    private fun imagePresetKey(id: Int) = stringPreferencesKey("widget_${id}_image_preset")
    private fun imageUriKey(id: Int) = stringPreferencesKey("widget_${id}_image_uri")
    private fun darkPresetKey(id: Int) = intPreferencesKey("widget_${id}_dark_preset")
}
