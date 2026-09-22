package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.dao.UsageStatDao
import com.example.data.local.entity.UsageStatEntity
import com.example.domain.model.AccuracyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsRepository(
    private val context: Context,
    private val usageStatDao: UsageStatDao
) {
    private val ACCURACY_MODE = stringPreferencesKey("accuracy_mode")
    private val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
    private val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
    private val KEEP_RECORDINGS = booleanPreferencesKey("keep_recordings")

    val accuracyMode: Flow<AccuracyMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[ACCURACY_MODE] ?: AccuracyMode.MAXIMUM_ACCURACY.name
        try {
            AccuracyMode.valueOf(modeStr)
        } catch (e: Exception) {
            AccuracyMode.MAXIMUM_ACCURACY
        }
    }

    val preferredLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_LANGUAGE] ?: "Egyptian Arabic + English"
    }

    val audioQuality: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_QUALITY] ?: "High"
    }

    val keepRecordings: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEEP_RECORDINGS] ?: true
    }

    suspend fun setAccuracyMode(mode: AccuracyMode) {
        context.dataStore.edit { preferences ->
            preferences[ACCURACY_MODE] = mode.name
        }
    }

    suspend fun setPreferredLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_LANGUAGE] = language
        }
    }

    suspend fun setAudioQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY] = quality
        }
    }

    suspend fun setKeepRecordings(keep: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_RECORDINGS] = keep
        }
    }

    suspend fun recordUsage(providerName: String, durationMs: Long) {
        val todayStr = getTodayDateString()
        usageStatDao.recordUsage(
            UsageStatEntity(
                dateString = todayStr,
                providerName = providerName,
                durationMs = durationMs
            )
        )
    }

    suspend fun getTodayMinutesForProvider(providerName: String): Double {
        val todayStr = getTodayDateString()
        val totalMs = usageStatDao.getDurationForProviderToday(todayStr, providerName) ?: 0L
        return totalMs / (1000.0 * 60.0)
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
