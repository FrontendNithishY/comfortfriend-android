package com.comfortfriend.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cf_settings")

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val model: String = DEFAULT_MODEL,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 700,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val memoryEnabled: Boolean = true,
    val mindLensEnabled: Boolean = true,
    val userName: String = "",
    val onboarded: Boolean = false,
) {
    companion object {
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    }
}

class SettingsStore(private val context: Context) {
    private object K {
        val model = stringPreferencesKey("model")
        val temperature = floatPreferencesKey("temperature")
        val maxTokens = intPreferencesKey("max_tokens")
        val theme = stringPreferencesKey("theme")
        val memory = booleanPreferencesKey("memory_enabled")
        val mindLens = booleanPreferencesKey("mindlens_enabled")
        val name = stringPreferencesKey("user_name")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            model = p[K.model] ?: d.model,
            temperature = p[K.temperature] ?: d.temperature,
            maxTokens = p[K.maxTokens] ?: d.maxTokens,
            theme = p[K.theme]?.let { runCatching { ThemeChoice.valueOf(it) }.getOrNull() } ?: d.theme,
            memoryEnabled = p[K.memory] ?: d.memoryEnabled,
            mindLensEnabled = p[K.mindLens] ?: d.mindLensEnabled,
            userName = p[K.name] ?: d.userName,
            onboarded = p[K.onboarded] ?: d.onboarded,
        )
    }

    suspend fun setModel(v: String) = context.dataStore.edit { it[K.model] = v }
    suspend fun setTemperature(v: Float) = context.dataStore.edit { it[K.temperature] = v.coerceIn(0f, 1.5f) }
    suspend fun setMaxTokens(v: Int) = context.dataStore.edit { it[K.maxTokens] = v.coerceIn(100, 2000) }
    suspend fun setTheme(v: ThemeChoice) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setMemoryEnabled(v: Boolean) = context.dataStore.edit { it[K.memory] = v }
    suspend fun setMindLensEnabled(v: Boolean) = context.dataStore.edit { it[K.mindLens] = v }
    suspend fun setUserName(v: String) = context.dataStore.edit { it[K.name] = v.trim().take(40) }
    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[K.onboarded] = v }
}
