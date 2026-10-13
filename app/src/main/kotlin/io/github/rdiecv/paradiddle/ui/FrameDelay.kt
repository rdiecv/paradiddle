// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui

import android.os.Build
import android.view.Choreographer
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND

/**
 * How long after its frame time (the `withFrameNanos` value) a frame is on screen. Views that
 * follow the audio draw the session time at frame time + this delay, so what you see matches
 * what you hear instead of lagging behind it.
 */
object FrameDelay {
    private const val MAX_NANOS = 200 * NANOS_PER_MS

    /** Typical delay when the system can't tell: two screen refreshes. */
    fun estimateNanos(refreshRateHz: Float): Long {
        val hz = if (refreshRateHz > 1f) refreshRateHz else 60f
        return (2.0 * NANOS_PER_SECOND / hz).toLong()
    }

    /** The [measured] delay when it is plausible, else the [estimate]. */
    fun pick(measured: Long, estimate: Long): Long = if (measured in 0..MAX_NANOS) measured else estimate
}

/**
 * The current [FrameDelay]. While [active], Android 13+ reports each frame's expected
 * presentation time; older versions use the estimate.
 */
@Composable
fun rememberFrameDelayNanos(active: Boolean): State<Long> {
    val view = LocalView.current
    val estimate = remember(view) { FrameDelay.estimateNanos(view.display?.refreshRate ?: 0f) }
    val delay = remember(view) { mutableLongStateOf(estimate) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DisposableEffect(active) {
            val watcher = if (active) PresentationWatcher { delay.longValue = FrameDelay.pick(it, estimate) } else null
            watcher?.start()
            onDispose { watcher?.stop() }
        }
    }
    return delay
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class PresentationWatcher(private val onDelay: (Long) -> Unit) : Choreographer.VsyncCallback {
    private val choreographer = Choreographer.getInstance()
    private var running = false

    fun start() {
        running = true
        choreographer.postVsyncCallback(this)
    }

    fun stop() {
        running = false
        choreographer.removeVsyncCallback(this)
    }

    override fun onVsync(data: Choreographer.FrameData) {
        onDelay(data.preferredFrameTimeline.expectedPresentationTimeNanos - data.frameTimeNanos)
        if (running) choreographer.postVsyncCallback(this)
    }
}
