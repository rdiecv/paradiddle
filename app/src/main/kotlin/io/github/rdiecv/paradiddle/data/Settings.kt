// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    /** Null until the user has calibrated once. */
    val calibrationOffsetMs: Int? = null,
    val clickVolume: Float = 0.8f,
    val guideSound: Boolean = false,
    val defaultRepetitions: Int = 8,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val calibration = intPreferencesKey("calibration_offset_ms")
        val clickVolume = floatPreferencesKey("click_volume")
        val guideSound = booleanPreferencesKey("guide_sound")
        val defaultRepetitions = intPreferencesKey("default_repetitions")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p: Preferences ->
        val defaults = Settings()
        Settings(
            calibrationOffsetMs = p[Keys.calibration],
            clickVolume = p[Keys.clickVolume] ?: defaults.clickVolume,
            guideSound = p[Keys.guideSound] ?: defaults.guideSound,
            defaultRepetitions = p[Keys.defaultRepetitions] ?: defaults.defaultRepetitions,
        )
    }

    suspend fun setCalibration(offsetMs: Int?) = context.dataStore.edit {
        if (offsetMs == null) it.remove(Keys.calibration) else it[Keys.calibration] = offsetMs
    }

    suspend fun setClickVolume(volume: Float) = context.dataStore.edit { it[Keys.clickVolume] = volume.coerceIn(0f, 1f) }

    suspend fun setGuideSound(on: Boolean) = context.dataStore.edit { it[Keys.guideSound] = on }

    suspend fun setDefaultRepetitions(reps: Int) = context.dataStore.edit { it[Keys.defaultRepetitions] = reps }
}
