// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

/** Facts about the Alesis Nitro Mesh module, from its official Kit & Voice List (v1.0). */
object NitroMesh {
    /** Preset kits 1-24 (kits 25-40 are user kits). */
    val presetKits = listOf(
        "Acoustic", "Standard 1", "Standard 2", "Funk 1", "Funk 2", "Fusion", "Camco", "Brush",
        "Room", "Rock", "Pop Rock", "Jazz", "Jazz Brush", "Timpani", "Songo", "1971’s",
        "Latin", "Disco", "Tr606", "909", "HipHop", "Electron 1", "Electron 2", "Techno 1",
    )

    fun kitName(number: Int): String = presetKits[number - 1]
}
