// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrooveCatalogTest {
    private fun realFile(): String = File(System.getProperty("groovesJson")).readText()

    private fun one(
        tracks: String = """{"snare":"....X.......X...","kick":"x.......x......."}""",
        family: String = "rock",
        bpm: Int = 100,
        minBpm: Int = 80,
        maxBpm: Int = 120,
        kit: Int = 10,
        id: String = "x",
    ) = """{"id":"$id","family":"$family","name":"X","bpm":$bpm,"minBpm":$minBpm,"maxBpm":$maxBpm,"kit":$kit,"tracks":$tracks}"""

    private fun parse(vararg grooves: String) = GrooveCatalog.parse(grooves.joinToString(",", "[", "]"))

    @Test
    fun realFileHasTheRockAndLatinStyles() {
        val all = GrooveCatalog.parse(realFile())
        assertEquals(
            listOf("rock", "pop", "punk", "metal", "bossa_nova", "songo", "reggae", "cumbia"),
            all.map { it.id },
        )
        assertEquals(4, all.count { it.family == GrooveFamily.ROCK })
        assertEquals(4, all.count { it.family == GrooveFamily.LATIN })
        all.forEach { assertTrue(it.id, it.kit in 1..NitroMesh.presetKits.size) }
    }

    @Test
    fun parsesTracksInInstrumentOrder() {
        val g = parse(one("""{"kick":"x.......x.......","hihat":"x.o.x.x.x.x.x.x.","snare":"..g.X.......X..."}""")).single()
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
        val g = parse(one("""{"cross_stick":"x.....x.....x.......x.....x.....","kick":"x.....x.x.....x.x.....x.x.....x."}""")).single()
        assertEquals(32, g.steps)
        assertEquals(2, g.bars)
    }

    private fun assertRejected(vararg grooves: String) {
        try {
            parse(*grooves)
        } catch (e: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }

    @Test fun rejectsUnknownFamily() = assertRejected(one(family = "polka"))
    @Test fun rejectsUnknownInstrument() = assertRejected(one("""{"cowbel":"x..............."}"""))
    @Test fun rejectsUnknownSymbol() = assertRejected(one("""{"snare":"....X...y...X..."}"""))
    @Test fun rejectsOpenOutsideHiHat() = assertRejected(one("""{"snare":"....o..........."}"""))
    @Test fun rejectsPartialBar() = assertRejected(one("""{"snare":"....X......."}"""))
    @Test fun rejectsTracksOfDifferentLength() =
        assertRejected(one("""{"snare":"....X.......X...","kick":"x.......x.......x.......x......."}"""))
    @Test fun rejectsEmptyGroove() = assertRejected(one("""{"snare":"................"}"""))
    @Test fun rejectsNoTracks() = assertRejected(one("""{}"""))
    @Test fun rejectsTempoOutsideRange() = assertRejected(one(bpm = 130))
    @Test fun rejectsTempoOutsideLimits() = assertRejected(one(bpm = 300, minBpm = 290, maxBpm = 320))
    @Test fun rejectsUnknownKit() = assertRejected(one(kit = 25))
    @Test fun rejectsDuplicateIds() = assertRejected(one(), one())
}
