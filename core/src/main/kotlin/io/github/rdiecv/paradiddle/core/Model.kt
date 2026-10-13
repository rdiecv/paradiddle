// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

enum class Hand { LEFT, RIGHT;

    val letter: Char get() = if (this == LEFT) 'L' else 'R'
}

enum class Category { ROLL, DIDDLE, FLAM, DRAG }

/**
 * One position of a sticking pattern. [hand] is null for a rest.
 * [graces] are the grace notes played just before the stroke (flams, drags), in order.
 */
data class Stroke(
    val hand: Hand?,
    val slots: Int = 1,
    val accent: Boolean = false,
    val buzz: Boolean = false,
    val graces: List<Hand> = emptyList(),
) {
    val isRest: Boolean get() = hand == null
}

data class Rudiment(
    val id: String,
    val pas: Int,
    val name: String,
    val category: Category,
    /** Grid slots per beat: 1, 2, 3, 4, 6 or 8. */
    val grid: Int,
    val strokes: List<Stroke>,
) {
    val totalSlots: Int get() = strokes.sumOf { it.slots }
    val beats: Int get() = totalSlots / grid

    /** Sticking as letters, e.g. "lR> R rL> L". */
    val sticking: String get() = StrokeNotation.format(strokes)
}
