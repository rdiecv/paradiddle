// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.input

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.data.InputSource

/** Hits on real drums: a practice pad or kit heard by the microphone, or an electronic kit over USB-MIDI. */
interface DrumInput {
    /**
     * Starts listening. [onHit] runs on a background thread with the hit's time
     * ([System.nanoTime] base), its loudness (0–1) and the pad, when known.
     * Throws [IllegalStateException] if the input can't be opened.
     */
    fun start(onHit: (nanoTime: Long, level: Float, pad: Instrument?) -> Unit)

    fun stop()
}

/** Why real drums can't be used right now. */
enum class InputProblem {
    /** The on-screen pads are chosen, but this exercise needs real drums. */
    NO_REAL_DRUMS,
    NO_MIDI_DEVICE,
    NO_MIC_PERMISSION,
    MIC_UNAVAILABLE,
}

object DrumInputs {
    fun hasMicPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** What stops [source] from working, or null if it is ready. */
    fun problem(context: Context, source: InputSource): InputProblem? = when (source) {
        InputSource.SCREEN -> null
        InputSource.MICROPHONE -> if (hasMicPermission(context)) null else InputProblem.NO_MIC_PERMISSION
        InputSource.MIDI -> if (MidiDrumInput.deviceName(context) == null) InputProblem.NO_MIDI_DEVICE else null
    }

    /** The input for [source]; null for the on-screen pads. */
    fun create(context: Context, source: InputSource): DrumInput? = when (source) {
        InputSource.SCREEN -> null
        InputSource.MICROPHONE -> MicDrumInput(context)
        InputSource.MIDI -> MidiDrumInput(context)
    }
}
