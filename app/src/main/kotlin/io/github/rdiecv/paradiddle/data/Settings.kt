// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the player plays on. */
enum class InputSource { SCREEN, MICROPHONE, MIDI }

data class Settings(
    /** Calibration of the on-screen pads; null until the user has calibrated once. */
    val calibrationOffsetMs: Int? = null,
    val clickVolume: Float = 0.8f,
    val guideSound: Boolean = false,
    /** Plays the right hand's strokes a little to the right and the left hand's to the left. */
    val separateHands: Boolean = true,
    val defaultRepetitions: Int = 8,
    val inputSource: InputSource = InputSource.SCREEN,
    val micOffsetMs: Int? = null,
    val midiOffsetMs: Int? = null,
) {
    /** Calibration of [source]; null until it has been calibrated. */
    fun offsetMs(source: InputSource = inputSource): Int? = when (source) {
        InputSource.SCREEN -> calibrationOffsetMs
        InputSource.MICROPHONE -> micOffsetMs
        InputSource.MIDI -> midiOffsetMs
    }
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val calibration = intPreferencesKey("calibration_offset_ms")
        val clickVolume = floatPreferencesKey("click_volume")
        val guideSound = booleanPreferencesKey("guide_sound")
        val separateHands = booleanPreferencesKey("separate_hands")
        val defaultRepetitions = intPreferencesKey("default_repetitions")
        val inputSource = stringPreferencesKey("input_source")
        val micOffset = intPreferencesKey("mic_offset_ms")
        val midiOffset = intPreferencesKey("midi_offset_ms")

        fun offset(source: InputSource) = when (source) {
            InputSource.SCREEN -> calibration
            InputSource.MICROPHONE -> micOffset
            InputSource.MIDI -> midiOffset
        }
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p: Preferences ->
        val defaults = Settings()
        Settings(
            calibrationOffsetMs = p[Keys.calibration],
            clickVolume = p[Keys.clickVolume] ?: defaults.clickVolume,
            guideSound = p[Keys.guideSound] ?: defaults.guideSound,
            separateHands = p[Keys.separateHands] ?: defaults.separateHands,
            defaultRepetitions = p[Keys.defaultRepetitions] ?: defaults.defaultRepetitions,
            inputSource = InputSource.entries.firstOrNull { it.name == p[Keys.inputSource] } ?: defaults.inputSource,
            micOffsetMs = p[Keys.micOffset],
            midiOffsetMs = p[Keys.midiOffset],
        )
    }

    suspend fun setCalibration(offsetMs: Int?, source: InputSource = InputSource.SCREEN) = context.dataStore.edit {
        val key = Keys.offset(source)
        if (offsetMs == null) it.remove(key) else it[key] = offsetMs
    }

    suspend fun setInputSource(source: InputSource) = context.dataStore.edit { it[Keys.inputSource] = source.name }

    suspend fun setClickVolume(volume: Float) = context.dataStore.edit { it[Keys.clickVolume] = volume.coerceIn(0f, 1f) }

    suspend fun setGuideSound(on: Boolean) = context.dataStore.edit { it[Keys.guideSound] = on }

    suspend fun setSeparateHands(on: Boolean) = context.dataStore.edit { it[Keys.separateHands] = on }

    suspend fun setDefaultRepetitions(reps: Int) = context.dataStore.edit { it[Keys.defaultRepetitions] = reps }
}
