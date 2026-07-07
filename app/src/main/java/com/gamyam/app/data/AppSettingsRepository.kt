package com.gamyam.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.appSettingsStore by preferencesDataStore(name = "app_settings")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class SavedTabSearchState(
    val fromName: String = "",
    val fromId: String = "",
    val fromGeozoneType: String = "",
    val fromSearchGeozoneType: String = "",
    val toName: String = "",
    val toId: String = "",
    val toGeozoneType: String = "",
    val toSearchGeozoneType: String = ""
)

@Serializable
data class SavedHomeSearchState(
    val tab: String = "DISTRICT",
    val cityId: String = "",
    val cityName: String = "",
    val tabStates: Map<String, SavedTabSearchState> = emptyMap(),
    val fromName: String = "",
    val fromId: String = "",
    val fromGeozoneType: String = "",
    val fromSearchGeozoneType: String = "",
    val toName: String = "",
    val toId: String = "",
    val toGeozoneType: String = "",
    val toSearchGeozoneType: String = ""
)

class AppSettingsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val keyTheme = stringPreferencesKey("theme")
    private val keyHomeState = stringPreferencesKey("home_state")

    val themeMode: Flow<AppThemeMode> = context.appSettingsStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            runCatching { AppThemeMode.valueOf(prefs[keyTheme] ?: AppThemeMode.SYSTEM.name) }
                .getOrDefault(AppThemeMode.SYSTEM)
        }

    val homeSearchState: Flow<SavedHomeSearchState> = context.appSettingsStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            runCatching {
                json.decodeFromString<SavedHomeSearchState>(prefs[keyHomeState] ?: "{}")
            }.getOrDefault(SavedHomeSearchState())
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appSettingsStore.edit { prefs -> prefs[keyTheme] = mode.name }
    }

    suspend fun saveHomeSearchState(state: SavedHomeSearchState) {
        context.appSettingsStore.edit { prefs -> prefs[keyHomeState] = json.encodeToString(state) }
    }
}
