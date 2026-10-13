// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

fun rudiment(strokes: String, grid: Int = 4, id: String = "test") =
    Rudiment(id, 1, "Test", Category.ROLL, grid, StrokeNotation.parse(strokes))

const val MS = NANOS_PER_MS
