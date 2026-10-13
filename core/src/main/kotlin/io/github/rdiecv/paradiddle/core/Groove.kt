// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

enum class GrooveFamily { ROCK, LATIN }

/** Drum kit pieces a groove can use, in the order they are shown (top to bottom). */
enum class Instrument { HIHAT, RIDE, COWBELL, SNARE, CROSS_STICK, TOM, KICK }

enum class Hit { NORMAL, ACCENT, GHOST, OPEN }

/**
 * The basic groove of a music style. Each track has one entry per 16th note ([steps] in
 * total, a whole number of 4/4 bars); null is a rest.
 */
data class Groove(
    val id: String,
    val family: GrooveFamily,
    val name: String,
    val bpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    /** Recommended Alesis Nitro Mesh preset kit, 1-based (see [NitroMesh.presetKits]). */
    val kit: Int,
    val tracks: Map<Instrument, List<Hit?>>,
) {
    val steps: Int get() = tracks.values.first().size
    val bars: Int get() = steps / STEPS_PER_BAR

    companion object {
        const val STEPS_PER_BAR = 16
        const val MIN_BPM = 30
        const val MAX_BPM = 280
    }
}
