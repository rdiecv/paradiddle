// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import io.github.rdiecv.paradiddle.core.audio.KitSound
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object StyleCatalog {
    @Serializable
    private data class StyleEntry(
        val id: String,
        val family: String,
        val name: String,
        val kit: Int,
        val sound: String,
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
    private data class SongEntry(
        val id: String,
        val title: String,
        val artist: String,
        val note: Map<String, String>? = null,
        val lesson: LessonEntry? = null,
    )

    @Serializable
    private data class LessonEntry(val bpm: Int, val sections: List<SectionEntry>, val steps: List<Map<String, String>>)

    @Serializable
    private data class SectionEntry(
        val name: Map<String, String>,
        /** Id of any beat in the file; or [tracks] for a short pattern of this section only; neither = the drums rest. */
        val beat: String? = null,
        val tracks: Map<String, String>? = null,
        val bars: Int,
        val note: Map<String, String>? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and validates the styles file, keeping its order. Throws [IllegalArgumentException] on bad data. */
    fun parse(text: String): List<Style> {
        val styleEntries = json.decodeFromString<List<StyleEntry>>(text)
        val styles = styleEntries.map { e ->
            fun fail(message: String): Nothing = throw IllegalArgumentException("${e.id}: $message")

            val family = GrooveFamily.entries.firstOrNull { it.name.equals(e.family, ignoreCase = true) }
                ?: fail("unknown family '${e.family}'")
            if (e.kit !in 1..NitroMesh.presetKits.size) fail("unknown kit ${e.kit}")
            val sound = KitSound.PRESETS[e.sound] ?: fail("unknown sound '${e.sound}'")
            if (e.beats.isEmpty()) fail("no beats")
            Style(e.id, family, e.name, e.kit, sound, text(e.setup) { fail("setup: $it") }, e.beats.map(::groove))
        }
        val beats = styles.flatMap { it.grooves }.associateBy { it.id }
        val entries = styleEntries.flatMap { it.beats }.associateBy { it.id }
        val withSongs = styles.map { style ->
            style.copy(grooves = style.grooves.map { g -> g.copy(songs = entries.getValue(g.id).songs.map { song(g.id, it, beats) }) })
        }
        val ids = withSongs.map { it.id } + withSongs.flatMap { s -> s.grooves.flatMap { g -> listOf(g.id) + g.songs.map { it.id } } }
        val dupIds = ids.groupBy { it }.filterValues { it.size > 1 }.keys
        require(dupIds.isEmpty()) { "Duplicate ids: $dupIds" }
        return withSongs
    }

    private fun song(beatId: String, e: SongEntry, beats: Map<String, Groove>): Song {
        fun fail(message: String): Nothing = throw IllegalArgumentException("$beatId/${e.id}: $message")

        if (e.id.isBlank() || e.title.isBlank() || e.artist.isBlank()) fail("song without id, title or artist")
        val lesson = e.lesson?.let { l ->
            if (l.bpm !in Groove.MIN_BPM..Groove.MAX_BPM) fail("lesson bpm ${l.bpm}")
            if (l.sections.isEmpty()) fail("lesson without sections")
            if (l.steps.isEmpty()) fail("lesson without steps")
            val sections = l.sections.mapIndexed { i, sec ->
                if (sec.bars < 1) fail("section with ${sec.bars} bars")
                if (sec.beat != null && sec.tracks != null) fail("section with both a beat and tracks")
                val name = text(sec.name) { fail("section name: $it") }
                val note = sec.note?.let { n -> text(n) { fail("section note: $it") } }
                val groove = when {
                    sec.beat != null -> beats[sec.beat] ?: fail("unknown beat '${sec.beat}'")
                    sec.tracks != null -> Groove(
                        "${e.id}_$i", name, l.bpm, l.bpm, l.bpm, note ?: name, tracks(sec.tracks) { fail("section ${i + 1}: $it") },
                    )
                    else -> null
                }
                SongSection(name, groove, sec.bars, note)
            }
            if (sections.none { it.groove != null }) fail("lesson without drums")
            SongLesson(l.bpm, sections, l.steps.map { step -> text(step) { fail("step: $it") } })
        }
        return Song(e.id, e.title, e.artist, e.note?.let { n -> text(n) { fail("note: $it") } }, lesson)
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

        val tracks = tracks(e.tracks, ::fail)
        return Groove(e.id, text(e.name) { fail("name: $it") }, e.bpm, e.minBpm, e.maxBpm, text(e.how) { fail("how: $it") }, tracks)
    }

    private fun tracks(source: Map<String, String>, fail: (String) -> Nothing): Map<Instrument, List<Hit?>> {
        if (source.isEmpty()) fail("no tracks")
        val tracks = source.entries
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
        return tracks
    }
}
