// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object GrooveCatalog {
    @Serializable
    private data class Entry(
        val id: String,
        val family: String,
        val name: String,
        val bpm: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val kit: Int,
        val tracks: Map<String, String>,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and validates the grooves file, keeping its order. Throws [IllegalArgumentException] on bad data. */
    fun parse(text: String): List<Groove> {
        val grooves = json.decodeFromString<List<Entry>>(text).map { e ->
            fun fail(message: String): Nothing = throw IllegalArgumentException("${e.id}: $message")

            val family = GrooveFamily.entries.firstOrNull { it.name.equals(e.family, ignoreCase = true) }
                ?: fail("unknown family '${e.family}'")
            if (e.bpm !in e.minBpm..e.maxBpm) fail("bpm ${e.bpm} outside ${e.minBpm}-${e.maxBpm}")
            if (e.minBpm < Groove.MIN_BPM || e.maxBpm > Groove.MAX_BPM) {
                fail("tempo range outside ${Groove.MIN_BPM}-${Groove.MAX_BPM}")
            }
            if (e.kit !in 1..NitroMesh.presetKits.size) fail("unknown kit ${e.kit}")
            if (e.tracks.isEmpty()) fail("no tracks")

            val tracks = e.tracks.entries
                .map { (name, pattern) ->
                    val instrument = Instrument.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?: fail("unknown instrument '$name'")
                    instrument to pattern.map { c ->
                        when (c) {
                            '.' -> null
                            'x' -> Hit.NORMAL
                            'X' -> Hit.ACCENT
                            'g' -> Hit.GHOST
                            'o' -> if (instrument == Instrument.HIHAT) Hit.OPEN else fail("open hit 'o' on $name")
                            else -> fail("unknown symbol '$c' on $name")
                        }
                    }
                }
                .sortedBy { it.first.ordinal }
                .toMap()
            val lengths = tracks.values.map { it.size }.toSet()
            if (lengths.size != 1) fail("tracks have different lengths $lengths")
            val steps = lengths.single()
            if (steps == 0 || steps % Groove.STEPS_PER_BAR != 0) fail("$steps steps is not a whole number of bars")
            if (tracks.values.none { t -> t.any { it != null } }) fail("no hits")

            Groove(e.id, family, e.name, e.bpm, e.minBpm, e.maxBpm, e.kit, tracks)
        }
        val dupIds = grooves.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupIds.isEmpty()) { "Duplicate ids: $dupIds" }
        return grooves
    }
}
