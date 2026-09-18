package com.lengcs.fkwakeup.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

data class AppSettings(
    val currentTermId: Long = -1L,
    val showWeekend: Boolean = true,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val CURRENT_TERM_ID = longPreferencesKey("current_term_id")
        val SHOW_WEEKEND = booleanPreferencesKey("show_weekend")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            currentTermId = prefs[Keys.CURRENT_TERM_ID] ?: -1L,
            showWeekend = prefs[Keys.SHOW_WEEKEND] ?: true,
        )
    }

    suspend fun setCurrentTerm(termId: Long) {
        context.settingsStore.edit { it[Keys.CURRENT_TERM_ID] = termId }
    }

    suspend fun setShowWeekend(show: Boolean) {
        context.settingsStore.edit { it[Keys.SHOW_WEEKEND] = show }
    }
}
