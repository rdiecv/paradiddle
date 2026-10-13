// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import io.github.rdiecv.paradiddle.core.audio.KitSound

enum class GrooveFamily { ROCK, LATIN }

/** Drum kit pieces a groove can use, in the order they are shown (top to bottom). */
enum class Instrument { CRASH, HIHAT, RIDE, COWBELL, SNARE, CROSS_STICK, TOM, KICK }

enum class Hit { NORMAL, ACCENT, GHOST, OPEN }

/** Text in several languages, keyed by language code; English is always present. */
data class LocalizedText(val byLanguage: Map<String, String>) {
    fun get(language: String): String = byLanguage[language] ?: byLanguage.getValue("en")
}

/** A music style and its beats, with the recommended Alesis Nitro Mesh setup. */
data class Style(
    val id: String,
    val family: GrooveFamily,
    val name: String,
    /** Recommended Alesis Nitro Mesh preset kit, 1-based (see [NitroMesh.presetKits]). */
    val kit: Int,
    /** How the app's drums sound for this style (see [io.github.rdiecv.paradiddle.core.audio.KitSound.PRESETS]). */
    val sound: KitSound,
    val setup: LocalizedText,
    val grooves: List<Groove>,
)

/**
 * One named beat of a style. Each track has one entry per 16th note ([steps] in total, a
 * whole number of 4/4 bars); null is a rest.
 */
data class Groove(
    val id: String,
    val name: LocalizedText,
    val bpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val how: LocalizedText,
    val tracks: Map<Instrument, List<Hit?>>,
    /** Well-known songs that use this beat, as listening examples. */
    val songs: List<Song> = emptyList(),
) {
    val steps: Int get() = tracks.values.first().size
    val bars: Int get() = steps / STEPS_PER_BAR

    companion object {
        const val STEPS_PER_BAR = 16
        const val MIN_BPM = 30
        const val MAX_BPM = 280
    }
}

/**
 * A recorded song that uses a beat; [note] says where to listen, if needed. [lesson] teaches the
 * whole song with the app's beats, when its structure has been checked against drum guides.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val note: LocalizedText? = null,
    val lesson: SongLesson? = null,
)

/** How to play a whole song: its parts in order, at [bpm], and the steps to learn it. */
data class SongLesson(val bpm: Int, val sections: List<SongSection>, val steps: List<LocalizedText>)

/** One part of a song played with [groove] for [bars] bars; no groove = the drums rest (count along). */
data class SongSection(val name: LocalizedText, val groove: Groove?, val bars: Int, val note: LocalizedText? = null)
