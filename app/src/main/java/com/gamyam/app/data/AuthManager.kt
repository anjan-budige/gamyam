package com.gamyam.app.data

import android.content.Context
import com.gamyam.app.BuildConfig
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthManager(private val context: Context) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val TOKEN_TIMESTAMP_KEY = longPreferencesKey("token_timestamp")
        const val EXPIRY_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val REFRESH_EARLY_MS = 10 * 60 * 1000L
    }

    private val refreshMutex = Mutex()

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val token = preferences[TOKEN_KEY]
        val timestamp = preferences[TOKEN_TIMESTAMP_KEY] ?: 0L
        if (token != null && (System.currentTimeMillis() - timestamp) < (EXPIRY_DURATION_MS - REFRESH_EARLY_MS)) {
            token
        } else {
            null
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[TOKEN_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }
    
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(TOKEN_TIMESTAMP_KEY)
        }
    }

    private suspend fun requestNewToken(): String {
        val response = ApiClient.oauthApiService.getAccessToken(
            clientId = BuildConfig.CLIENT_ID,
            clientSecret = BuildConfig.CLIENT_SECRET
        )
        saveToken(response.access_token)
        return response.access_token
    }

    suspend fun freshToken(forceRefresh: Boolean = false): String =
        refreshMutex.withLock {
            if (!forceRefresh) {
                authTokenFlow.firstOrNull()?.let { return@withLock it }
            }

            requestNewToken()
        }

    suspend fun bearer(forceRefresh: Boolean = false): String =
        "Bearer ${freshToken(forceRefresh)}"

    private suspend fun retryBearerAfterUnauthorized(failedAuth: String): String =
        refreshMutex.withLock {
            authTokenFlow.firstOrNull()
                ?.let { "Bearer $it" }
                ?.takeIf { it != failedAuth }
                ?: "Bearer ${requestNewToken()}"
        }

    suspend fun <T> authenticatedCall(block: suspend (auth: String) -> T): T {
        val auth = bearer()
        return try {
            block(auth)
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
            block(retryBearerAfterUnauthorized(auth))
        }
    }
}
