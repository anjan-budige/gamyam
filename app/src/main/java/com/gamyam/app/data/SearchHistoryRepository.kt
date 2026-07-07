package com.gamyam.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamyam.app.ui.main.SearchHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Separate DataStore from auth_prefs (which is already claimed by AuthManager)
private val Context.searchHistoryStore by preferencesDataStore(name = "search_history")

class SearchHistoryRepository(private val context: Context) {

    private val KEY_ENTRIES = stringPreferencesKey("entries")

    private val json = Json { ignoreUnknownKeys = true }

    /** Emits the list of recent searches, newest first. Never throws. */
    val flow: Flow<List<SearchHistoryItem>> = context.searchHistoryStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            runCatching {
                json.decodeFromString<List<SearchHistoryItem>>(prefs[KEY_ENTRIES] ?: "[]")
            }.getOrElse { emptyList() }
        }

    /**
     * Prepend [entry] to the shared history, deduplicate by search type,
     * and persist. Only the most recent [MAX_HISTORY] entries are kept.
     */
    suspend fun save(entry: SearchHistoryItem) {
        context.searchHistoryStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<SearchHistoryItem>>(prefs[KEY_ENTRIES] ?: "[]")
            }.getOrElse { emptyList() }

            val updated = (listOf(entry) + current.filterNot { it.sameHistoryKey(entry) })
                .take(MAX_HISTORY)

            prefs[KEY_ENTRIES] = json.encodeToString(updated)
        }
    }

    private fun SearchHistoryItem.sameHistoryKey(other: SearchHistoryItem): Boolean {
        if (isServiceIdSearch || other.isServiceIdSearch) {
            return isServiceIdSearch == other.isServiceIdSearch &&
                (serviceId.takeIf { it > 0 } ?: toName.toIntOrNull()) ==
                (other.serviceId.takeIf { it > 0 } ?: other.toName.toIntOrNull()) &&
                tab == other.tab
        }

        return fromKey() == other.fromKey() &&
            toKey() == other.toKey() &&
            tab == other.tab &&
            operationType == other.operationType &&
            cityId == other.cityId &&
            isAirport == other.isAirport
    }

    private fun SearchHistoryItem.fromKey(): String =
        fromId.ifBlank { fromName.trim().lowercase() }

    private fun SearchHistoryItem.toKey(): String =
        toId.ifBlank { toName.trim().lowercase() }

    companion object {
        const val MAX_HISTORY = 5
    }
}
