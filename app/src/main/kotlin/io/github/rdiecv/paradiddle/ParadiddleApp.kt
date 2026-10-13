// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle

import android.app.Application
import androidx.room.Room
import io.github.rdiecv.paradiddle.core.Rudiment
import io.github.rdiecv.paradiddle.core.RudimentCatalog
import io.github.rdiecv.paradiddle.core.Style
import io.github.rdiecv.paradiddle.core.StyleCatalog
import io.github.rdiecv.paradiddle.data.AppDatabase
import io.github.rdiecv.paradiddle.data.SettingsRepository

class ParadiddleApp : Application() {
    val rudiments: List<Rudiment> by lazy {
        RudimentCatalog.parse(assets.open("rudiments.json").bufferedReader().use { it.readText() })
    }

    val styles: List<Style> by lazy {
        StyleCatalog.parse(assets.open("styles.json").bufferedReader().use { it.readText() })
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "paradiddle.db").build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    fun rudiment(id: String): Rudiment? = rudiments.firstOrNull { it.id == id }

    fun style(id: String): Style? = styles.firstOrNull { it.id == id }
}
