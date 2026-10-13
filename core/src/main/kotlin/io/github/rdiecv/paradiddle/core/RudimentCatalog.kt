// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object RudimentCatalog {
    val allowedGrids = setOf(1, 2, 3, 4, 6, 8)

    @Serializable
    private data class Entry(
        val id: String,
        val pas: Int,
        val name: String,
        val category: String,
        val grid: Int,
        val strokes: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses and validates the rudiments file. Throws [IllegalArgumentException] on bad data. */
    fun parse(text: String): List<Rudiment> {
        val rudiments = json.decodeFromString<List<Entry>>(text).map { e ->
            val category = Category.entries.firstOrNull { it.name.equals(e.category, ignoreCase = true) }
                ?: throw IllegalArgumentException("${e.id}: unknown category '${e.category}'")
            require(e.grid in allowedGrids) { "${e.id}: bad grid ${e.grid}" }
            val strokes = try {
                StrokeNotation.parse(e.strokes)
            } catch (ex: IllegalArgumentException) {
                throw IllegalArgumentException("${e.id}: ${ex.message}", ex)
            }
            Rudiment(e.id, e.pas, e.name, category, e.grid, strokes).also {
                require(it.totalSlots % it.grid == 0) {
                    "${e.id}: ${it.totalSlots} slots is not a whole number of beats (grid ${it.grid})"
                }
                require(it.strokes.any { s -> !s.isRest }) { "${e.id}: no strokes" }
            }
        }
        val dupIds = rudiments.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupIds.isEmpty()) { "Duplicate ids: $dupIds" }
        val dupPas = rudiments.groupBy { it.pas }.filterValues { it.size > 1 }.keys
        require(dupPas.isEmpty()) { "Duplicate PAS numbers: $dupPas" }
        return rudiments.sortedBy { it.pas }
    }
}
