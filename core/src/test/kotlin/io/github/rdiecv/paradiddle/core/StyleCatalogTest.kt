// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleCatalogTest {
    private fun realFile(): String = File(System.getProperty("stylesJson")).readText()

    private fun beat(
        tracks: String = """{"snare":"....X.......X...","kick":"x.......x......."}""",
        bpm: Int = 100,
        minBpm: Int = 80,
        maxBpm: Int = 120,
        id: String = "b",
        how: String = """{"en":"How","es":"Cómo"}""",
    ) = """{"id":"$id","name":{"en":"B"},"bpm":$bpm,"minBpm":$minBpm,"maxBpm":$maxBpm,"how":$how,"tracks":$tracks}"""

    private fun style(
        vararg beats: String = arrayOf(beat()),
        family: String = "rock",
        kit: Int = 10,
        id: String = "s",
        setup: String = """{"en":"Setup"}""",
    ) = """{"id":"$id","family":"$family","name":"S","kit":$kit,"setup":$setup,"beats":${beats.joinToString(",", "[", "]")}}"""

    private fun parse(vararg styles: String) = StyleCatalog.parse(styles.joinToString(",", "[", "]"))

    @Test
    fun realFileHasTheRockAndLatinStyles() {
        val all = StyleCatalog.parse(realFile())
        assertEquals(listOf("rock", "pop", "punk", "metal", "bossa_nova", "songo", "reggae", "cumbia"), all.map { it.id })
        assertEquals(4, all.count { it.family == GrooveFamily.ROCK })
        assertEquals(4, all.count { it.family == GrooveFamily.LATIN })
        all.forEach { assertTrue(it.id, it.kit in 1..NitroMesh.presetKits.size) }
    }

    @Test
    fun realFileHasSeveralBeatsForMostStyles() {
        val all = StyleCatalog.parse(realFile())
        assertEquals(18, all.sumOf { it.grooves.size })
        assertTrue(all.count { it.grooves.size >= 2 } >= 6)
    }

    @Test
    fun realFileTextsAreInEnglishAndSpanish() {
        StyleCatalog.parse(realFile()).forEach { s ->
            assertEquals(s.id, setOf("en", "es"), s.setup.byLanguage.keys)
            s.grooves.forEach { g ->
                assertEquals(g.id, setOf("en", "es"), g.name.byLanguage.keys)
                assertEquals(g.id, setOf("en", "es"), g.how.byLanguage.keys)
            }
        }
    }

    @Test
    fun parsesTracksInInstrumentOrder() {
        val g = parse(style(beat("""{"kick":"x.......x.......","hihat":"x.o.x.x.x.x.x.x.","snare":"..g.X.......X..."}"""))).single().grooves.single()
        assertEquals(listOf(Instrument.HIHAT, Instrument.SNARE, Instrument.KICK), g.tracks.keys.toList())
        assertEquals(16, g.steps)
        assertEquals(1, g.bars)
        assertEquals(Hit.OPEN, g.tracks.getValue(Instrument.HIHAT)[2])
        assertEquals(Hit.GHOST, g.tracks.getValue(Instrument.SNARE)[2])
        assertEquals(Hit.ACCENT, g.tracks.getValue(Instrument.SNARE)[4])
        assertEquals(Hit.NORMAL, g.tracks.getValue(Instrument.KICK)[0])
        assertNull(g.tracks.getValue(Instrument.KICK)[1])
    }

    @Test
    fun twoBarGroove() {
        val g = parse(style(beat("""{"cross_stick":"x.....x.....x.......x.....x.....","kick":"x.....x.x.....x.x.....x.x.....x."}""")))
            .single().grooves.single()
        assertEquals(32, g.steps)
        assertEquals(2, g.bars)
    }

    @Test
    fun textFallsBackToEnglish() {
        val s = parse(style()).single()
        assertEquals("Setup", s.setup.get("es"))
        assertEquals("Cómo", s.grooves.single().how.get("es"))
        assertEquals("How", s.grooves.single().how.get("fr"))
    }

    private fun assertRejected(vararg styles: String) {
        try {
            parse(*styles)
        } catch (e: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }

    @Test fun rejectsUnknownFamily() = assertRejected(style(family = "polka"))
    @Test fun rejectsUnknownKit() = assertRejected(style(kit = 25))
    @Test fun rejectsStyleWithoutBeats() = assertRejected(style(beats = arrayOf()))
    @Test fun rejectsTextWithoutEnglish() = assertRejected(style(setup = """{"es":"Config"}"""))
    @Test fun rejectsEmptyText() = assertRejected(style(beat(how = """{"en":" "}""")))
    @Test fun rejectsUnknownInstrument() = assertRejected(style(beat("""{"cowbel":"x..............."}""")))
    @Test fun rejectsUnknownSymbol() = assertRejected(style(beat("""{"snare":"....X...y...X..."}""")))
    @Test fun rejectsOpenOutsideHiHat() = assertRejected(style(beat("""{"snare":"....o..........."}""")))
    @Test fun rejectsPartialBar() = assertRejected(style(beat("""{"snare":"....X......."}""")))
    @Test fun rejectsTracksOfDifferentLength() =
        assertRejected(style(beat("""{"snare":"....X.......X...","kick":"x.......x.......x.......x......."}""")))
    @Test fun rejectsEmptyGroove() = assertRejected(style(beat("""{"snare":"................"}""")))
    @Test fun rejectsNoTracks() = assertRejected(style(beat("""{}""")))
    @Test fun rejectsTempoOutsideRange() = assertRejected(style(beat(bpm = 130)))
    @Test fun rejectsTempoOutsideLimits() = assertRejected(style(beat(bpm = 300, minBpm = 290, maxBpm = 320)))
    @Test fun rejectsDuplicateBeatIds() = assertRejected(style(beat(), beat()))
    @Test fun rejectsBeatIdEqualToAStyleId() = assertRejected(style(beat(id = "s")))
    @Test fun rejectsDuplicateStyleIds() = assertRejected(style(), style(beat(id = "c")))
}
