// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import io.github.rdiecv.paradiddle.core.Instrument

/** Default pad notes of the Alesis Nitro module (Nitro Drum Module User Guide v1.2, appendix). */
object NitroMidi {
    private const val HI_HAT_PEDAL = 44

    private val NOTES: Map<Int, Instrument> = buildMap {
        put(36, Instrument.KICK)
        put(38, Instrument.SNARE)
        put(40, Instrument.CROSS_STICK) // snare rim
        listOf(48, 50, 45, 47, 43, 58, 41, 39).forEach { put(it, Instrument.TOM) } // toms 1–4 and their rims
        put(51, Instrument.RIDE)
        listOf(49, 57, 21).forEach { put(it, Instrument.CRASH) } // crash 1, crash 2, splash
        listOf(46, 23, 42).forEach { put(it, Instrument.HIHAT) } // open, half-open, closed
    }

    /** The pad a note comes from, or null for a note the module was set to send that isn't known. */
    fun instrument(note: Int): Instrument? = NOTES[note]

    /** Notes that aren't strokes: the hi-hat pedal closing. */
    fun ignored(note: Int): Boolean = note == HI_HAT_PEDAL
}

data class MidiNote(val note: Int, val velocity: Int)

/** Reads note-on messages from a raw MIDI byte stream, with running status, across calls. */
class MidiParser {
    private var status = 0
    private var data1 = -1
    private var inSysEx = false

    fun feed(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size - offset): List<MidiNote> {
        val out = ArrayList<MidiNote>(1)
        for (i in offset until offset + count) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b >= 0xF8 -> Unit // real-time bytes can appear anywhere
                b == 0xF0 -> { inSysEx = true; status = 0 }
                b == 0xF7 -> inSysEx = false
                inSysEx -> Unit
                b >= 0x80 -> { status = if (b < 0xF0) b else 0; data1 = -1 }
                status == 0 -> Unit
                data1 < 0 && dataBytes(status) == 2 -> data1 = b
                else -> {
                    if (status and 0xF0 == 0x90 && b > 0) out += MidiNote(data1, b)
                    data1 = -1
                }
            }
        }
        return out
    }

    private fun dataBytes(status: Int): Int = when (status and 0xF0) {
        0xC0, 0xD0 -> 1
        else -> 2
    }
}
