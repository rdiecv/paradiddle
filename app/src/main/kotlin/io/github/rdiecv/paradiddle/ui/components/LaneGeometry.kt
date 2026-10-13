// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

/**
 * Vertical positions in the falling-note lanes: a time [lookaheadNanos] ahead is drawn at
 * [topY] and "now" on the hit line at [hitY].
 */
internal class LaneGeometry(val hitY: Float, topY: Float, lookaheadNanos: Long) {
    private val pxPerNano = (hitY - topY) / lookaheadNanos

    fun yOf(timeNanos: Long, nowNanos: Long): Float = hitY - (timeNanos - nowNanos) * pxPerNano

    /** Top edge of a note [height] tall. A note is centred on the hit line at its time, which is when to hit it. */
    fun noteTop(timeNanos: Long, nowNanos: Long, height: Float): Float = yOf(timeNanos, nowNanos) - height / 2
}
