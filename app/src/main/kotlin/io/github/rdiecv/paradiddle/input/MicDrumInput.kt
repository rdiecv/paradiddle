// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.input

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaRecorder
import android.os.Process
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND
import io.github.rdiecv.paradiddle.core.input.OnsetDetector

/** A practice pad or acoustic kit heard by the phone's microphone. */
class MicDrumInput(private val context: Context) : DrumInput {
    @Volatile private var running = false
    private var thread: Thread? = null

    @SuppressLint("MissingPermission") // checked just below; the caller asks for it
    override fun start(onHit: (nanoTime: Long, level: Float, pad: Instrument?) -> Unit) {
        check(DrumInputs.hasMicPermission(context)) { "No microphone permission" }
        val audio = context.getSystemService(AudioManager::class.java)
        val rate = audio.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48_000
        // Raw input when the phone has it, otherwise speech input, which also has no automatic gain.
        val sources = buildList {
            if (audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        }
        val record = sources.firstNotNullOfOrNull { open(it, rate) } ?: throw IllegalStateException("Microphone not available")
        try {
            record.startRecording()
        } catch (e: RuntimeException) {
            record.release()
            throw IllegalStateException("Microphone not available", e)
        }
        // Another app holding the microphone doesn't throw: recording just doesn't start.
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            throw IllegalStateException("Microphone busy")
        }
        running = true
        thread = Thread({ listen(record, rate, onHit) }, "paradiddle-mic").also { it.start() }
    }

    /** A recorder on [source], or null if this phone can't record that way. */
    @SuppressLint("MissingPermission") // checked by start()
    private fun open(source: Int, rate: Int): AudioRecord? {
        val minBytes = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        if (minBytes <= 0) return null
        val record = try {
            AudioRecord(
                source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                maxOf(minBytes, rate / 10 * 4), // at least 100 ms
            )
        } catch (e: RuntimeException) { // unsupported settings, or the permission was just revoked
            return null
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        return record
    }

    private fun listen(record: AudioRecord, rate: Int, onHit: (Long, Float, Instrument?) -> Unit) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val detector = OnsetDetector(rate)
        val buf = FloatArray(rate / 200) // 5 ms
        val ts = AudioTimestamp()
        var frames = 0L
        try {
            while (running) {
                val n = record.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                if (n <= 0) break
                val readAt = System.nanoTime()
                frames += n
                val onsets = detector.feed(buf, n)
                if (onsets.isEmpty()) continue
                val stamped = record.getTimestamp(ts, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS
                onsets.forEach { o ->
                    // The timestamp maps a captured frame to the clock; without one, assume the
                    // last frame read was captured just now.
                    val time = if (stamped) {
                        ts.nanoTime + (o.frame - ts.framePosition) * NANOS_PER_SECOND / rate
                    } else {
                        readAt - (frames - o.frame) * NANOS_PER_SECOND / rate
                    }
                    onHit(time, o.level, null)
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    override fun stop() {
        running = false
        thread = null
    }
}
