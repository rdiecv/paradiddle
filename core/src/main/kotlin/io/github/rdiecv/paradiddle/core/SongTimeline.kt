// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

/** Where a song is at one 16th note. */
data class SongPlace(
    val section: Int,
    /** Bar within the section, 0-based. */
    val bar: Int,
    /** Step within the section's groove (it repeats through the section). */
    val grooveStep: Int,
    /** True on the first 16th of a section. */
    val sectionStart: Boolean,
)

/** The song's sections laid end to end, from section [from] onwards, on a grid of 16th notes. */
class SongTimeline(val sections: List<SongSection>, val from: Int = 0) {
    private val starts: IntArray
    val totalSteps: Int

    init {
        require(from in sections.indices) { "No section $from" }
        var step = 0
        starts = IntArray(sections.size) { i ->
            val start = step
            if (i >= from) step += sections[i].bars * Groove.STEPS_PER_BAR
            start
        }
        totalSteps = step
    }

    /** First step of [section] (which must be at or after [from]). */
    fun startOf(section: Int): Int = starts[section]

    /** Place of [step] (0 = first 16th of section [from]), or null outside the song. */
    fun placeAt(step: Int): SongPlace? {
        if (step < 0 || step >= totalSteps) return null
        var section = sections.lastIndex
        while (section > from && starts[section] > step) section--
        val inSection = step - starts[section]
        val grooveSteps = sections[section].groove?.steps ?: Groove.STEPS_PER_BAR
        return SongPlace(section, inSection / Groove.STEPS_PER_BAR, inSection % grooveSteps, inSection == 0)
    }
}
