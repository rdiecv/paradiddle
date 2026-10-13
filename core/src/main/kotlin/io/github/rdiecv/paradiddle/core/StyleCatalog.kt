// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object StyleCatalog {
    @Serializable
    private data class StyleEntry(
        val id: String,
        val family: String,
        val name: String,
        val kit: Int,
        val setup: Map<String, String>,
        val beats: List<BeatEntry>,
    )

    @Serializable
    private data class BeatEntry(
        val id: String,
        val name: Map<String, String>,
        val bpm: Int,
        val minBpm: Int,
        val maxBpm: Int,
        val how: Map<String, String>,
        val tracks: Map<String, String>,
        val songs: List<SongEntry> = emptyList(),
    )

    @Serializable
    private data class SongEntry(val title: String, val artist: String, val note: Map<String, String>? = null)

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and validates the styles file, keeping its order. Throws [IllegalArgumentException] on bad data. */
    fun parse(text: String): List<Style> {
        val styles = json.decodeFromString<List<StyleEntry>>(text).map { e ->
            fun fail(message: String): Nothing = throw IllegalArgumentException("${e.id}: $message")

            val family = GrooveFamily.entries.firstOrNull { it.name.equals(e.family, ignoreCase = true) }
                ?: fail("unknown family '${e.family}'")
            if (e.kit !in 1..NitroMesh.presetKits.size) fail("unknown kit ${e.kit}")
            if (e.beats.isEmpty()) fail("no beats")
            Style(e.id, family, e.name, e.kit, text(e.setup) { fail("setup: $it") }, e.beats.map(::groove))
        }
        val ids = styles.map { it.id } + styles.flatMap { s -> s.grooves.map { it.id } }
        val dupIds = ids.groupBy { it }.filterValues { it.size > 1 }.keys
        require(dupIds.isEmpty()) { "Duplicate ids: $dupIds" }
        return styles
    }

    private inline fun text(map: Map<String, String>, fail: (String) -> Nothing): LocalizedText {
        if ("en" !in map) fail("no English text")
        if (map.values.any { it.isBlank() }) fail("empty text")
        return LocalizedText(map)
    }

    private fun groove(e: BeatEntry): Groove {
        fun fail(message: String): Nothing = throw IllegalArgumentException("${e.id}: $message")

        if (e.bpm !in e.minBpm..e.maxBpm) fail("bpm ${e.bpm} outside ${e.minBpm}-${e.maxBpm}")
        if (e.minBpm < Groove.MIN_BPM || e.maxBpm > Groove.MAX_BPM) {
            fail("tempo range outside ${Groove.MIN_BPM}-${Groove.MAX_BPM}")
        }
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

        val songs = e.songs.map { song ->
            if (song.title.isBlank() || song.artist.isBlank()) fail("song without title or artist")
            Song(song.title, song.artist, song.note?.let { note -> text(note) { fail("note of ${song.title}: $it") } })
        }
        return Groove(
            e.id, text(e.name) { fail("name: $it") }, e.bpm, e.minBpm, e.maxBpm, text(e.how) { fail("how: $it") }, tracks, songs,
        )
    }
}
