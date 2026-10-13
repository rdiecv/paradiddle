// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import android.os.SystemClock
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND
import io.github.rdiecv.paradiddle.core.audio.Mixer
import io.github.rdiecv.paradiddle.core.audio.Voice

/** Low-latency playback of mixer voices, with a map from clock time to session time. */
object AudioEngine {
    val sampleRate: Int by lazy { AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) }

    /**
     * Starts playing [voices] right away. [lengthFrames] is how long the session lasts.
     * [onFinished] runs on the audio thread once everything has been heard.
     * Throws [IllegalStateException] if the audio output can't be opened.
     */
    fun play(voices: List<Voice>, lengthFrames: Long, onFinished: () -> Unit): Playback =
        Playback(sampleRate, Mixer(voices), lengthFrames, onFinished).also { it.start() }

    /** Converts a Compose/MotionEvent uptime in ms to the [System.nanoTime] time base. */
    fun uptimeMillisToNanoTime(uptimeMillis: Long): Long {
        val delta = System.nanoTime() - SystemClock.uptimeMillis() * 1_000_000L
        return uptimeMillis * 1_000_000L + delta
    }
}

class Playback internal constructor(
    private val sampleRate: Int,
    private val mixer: Mixer,
    private val lengthFrames: Long,
    private val onFinished: () -> Unit,
) {
    private val chunkFrames = 192
    private val track: AudioTrack
    private val thread: Thread

    @Volatile private var running = true
    @Volatile private var anchorFrame = 0L
    @Volatile private var anchorNanos = 0L
    @Volatile private var hasTimestamp = false
    @Volatile private var startNanos = 0L
    private val bufferFrames: Int

    init {
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        check(minBytes > 0) { "Audio output not available" }
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(minBytes)
            .build()
        check(track.state == AudioTrack.STATE_INITIALIZED) { "Audio output not available" }
        bufferFrames = track.bufferSizeInFrames
        thread = Thread(::renderLoop, "paradiddle-audio")
    }

    internal fun start() {
        thread.start()
    }

    private fun renderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val buf = FloatArray(chunkFrames)
        val ts = AudioTimestamp()
        var written = 0L
        var lastTimestampCheck = 0L
        try {
            // Prime the buffer so playback starts with session frame 0.
            mixer.render(written, buf)
            written += track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
            startNanos = System.nanoTime()
            track.play()
            while (running && written < lengthFrames) {
                mixer.render(written, buf)
                val n = track.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
                if (n <= 0) break
                written += n
                val now = System.nanoTime()
                if (now - lastTimestampCheck > 50_000_000L && track.getTimestamp(ts) && ts.framePosition > 0) {
                    anchorNanos = ts.nanoTime
                    anchorFrame = ts.framePosition
                    hasTimestamp = true
                    lastTimestampCheck = now
                }
            }
            // Let the tail be heard.
            while (running && track.playbackHeadPosition.toLong() < lengthFrames) {
                Thread.sleep(10)
            }
        } finally {
            val completed = running
            running = false
            runCatching { track.pause(); track.flush() }
            track.release()
            if (completed) onFinished()
        }
    }

    /** Session time (ns since session frame 0 was heard) at clock time [nanoTime] (System.nanoTime base). */
    fun sessionTimeAt(nanoTime: Long): Long {
        if (hasTimestamp) {
            return anchorFrame * NANOS_PER_SECOND / sampleRate + (nanoTime - anchorNanos)
        }
        // Before the first timestamp: assume the primed buffer adds its own length of latency.
        if (startNanos == 0L) return Long.MIN_VALUE / 4
        return nanoTime - startNanos - bufferFrames.toLong() * NANOS_PER_SECOND / sampleRate
    }

    val isRunning: Boolean get() = running

    /** Stops playback without calling onFinished. */
    fun stop() {
        running = false
    }
}
