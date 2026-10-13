// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import io.github.rdiecv.paradiddle.core.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiTest {
    @Test
    fun nitroNotesFromTheManual() {
        assertEquals(Instrument.KICK, NitroMidi.instrument(36))
        assertEquals(Instrument.SNARE, NitroMidi.instrument(38))
        assertEquals(Instrument.CROSS_STICK, NitroMidi.instrument(40))
        listOf(48, 50, 45, 47, 43, 58, 41, 39).forEach { assertEquals(Instrument.TOM, NitroMidi.instrument(it)) }
        assertEquals(Instrument.RIDE, NitroMidi.instrument(51))
        listOf(49, 57, 21).forEach { assertEquals(Instrument.CRASH, NitroMidi.instrument(it)) }
        listOf(46, 23, 42).forEach { assertEquals(Instrument.HIHAT, NitroMidi.instrument(it)) }
    }

    @Test
    fun pedalIsIgnoredAndUnknownNotesAreAnyPad() {
        assertTrue(NitroMidi.ignored(44))
        assertNull(NitroMidi.instrument(60))
    }

    @Test
    fun parsesNoteOnsWithRunningStatus() {
        val p = MidiParser()
        // Note on ch10 kick, then running status snare, then note-off by velocity 0.
        val notes = p.feed(byteArrayOf(0x99.toByte(), 36, 100, 38, 64, 38, 0))
        assertEquals(listOf(MidiNote(36, 100), MidiNote(38, 64)), notes)
    }

    @Test
    fun skipsRealTimeBytesAndOtherMessagesAndKeepsStateAcrossCalls() {
        val p = MidiParser()
        val a = p.feed(byteArrayOf(0xF8.toByte(), 0xB9.toByte(), 4, 90, 0x99.toByte(), 42))
        val b = p.feed(byteArrayOf(0xFE.toByte(), 80, 0x89.toByte(), 42, 0))
        assertEquals(emptyList<MidiNote>(), a)
        assertEquals(listOf(MidiNote(42, 80)), b)
    }

    @Test
    fun sysExIsSkipped() {
        val p = MidiParser()
        val notes = p.feed(byteArrayOf(0xF0.toByte(), 1, 2, 3, 0xF7.toByte(), 0x99.toByte(), 36, 127))
        assertEquals(listOf(MidiNote(36, 127)), notes)
    }
}
