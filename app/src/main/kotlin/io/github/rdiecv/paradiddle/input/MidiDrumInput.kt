// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.input

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.input.MidiParser
import io.github.rdiecv.paradiddle.core.input.NitroMidi

/** An electronic kit (the Alesis Nitro module or any USB-MIDI drum module) plugged into the phone. */
class MidiDrumInput(context: Context) : DrumInput {
    private val manager = midiManager(context)
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    @Volatile private var running = false

    override fun start(onHit: (nanoTime: Long, level: Float, pad: Instrument?) -> Unit) {
        val m = manager ?: throw IllegalStateException("MIDI not supported")
        val info = firstDrumSource(m) ?: throw IllegalStateException("No MIDI device")
        running = true
        val parser = MidiParser()
        val receiver = object : MidiReceiver() {
            override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                if (!running) return
                val time = if (timestamp > 0) timestamp else System.nanoTime()
                parser.feed(msg, offset, count).forEach { n ->
                    if (!NitroMidi.ignored(n.note)) onHit(time, n.velocity / 127f, NitroMidi.instrument(n.note))
                }
            }
        }
        m.openDevice(info, { opened ->
            if (opened == null) return@openDevice
            if (!running) {
                opened.close()
                return@openDevice
            }
            device = opened
            port = opened.openOutputPort(firstOutputPort(info))?.also { it.connect(receiver) }
        }, Handler(Looper.getMainLooper()))
    }

    override fun stop() {
        running = false
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
    }

    companion object {
        private fun midiManager(context: Context): MidiManager? = context.getSystemService(MidiManager::class.java)

        /** Devices that send MIDI (have an output port). */
        @Suppress("DEPRECATION") // getDevicesForTransport needs API 33; this covers every version.
        private fun firstDrumSource(m: MidiManager): MidiDeviceInfo? = m.devices.firstOrNull { it.outputPortCount > 0 }

        private fun firstOutputPort(info: MidiDeviceInfo): Int =
            info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }?.portNumber ?: 0

        /** Name of the connected kit, or null if none. */
        fun deviceName(context: Context): String? {
            val info = midiManager(context)?.let(::firstDrumSource) ?: return null
            val p: Bundle = info.properties
            return p.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: p.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: p.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
                ?: "MIDI"
        }

        /** Calls [onChange] whenever a MIDI device is plugged in or out; returns the unregister action. */
        fun watch(context: Context, onChange: () -> Unit): () -> Unit {
            val m = midiManager(context) ?: return {}
            val callback = object : MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) = onChange()
                override fun onDeviceRemoved(device: MidiDeviceInfo) = onChange()
            }
            @Suppress("DEPRECATION") // the Executor overload needs API 33
            m.registerDeviceCallback(callback, Handler(Looper.getMainLooper()))
            return { m.unregisterDeviceCallback(callback) }
        }
    }
}
