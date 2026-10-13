// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.rdiecv.paradiddle.ui.screens.CalibrationScreen
import io.github.rdiecv.paradiddle.ui.screens.LogScreen
import io.github.rdiecv.paradiddle.ui.screens.PracticeScreen
import io.github.rdiecv.paradiddle.ui.screens.PracticeViewModel
import io.github.rdiecv.paradiddle.ui.screens.RudimentDetailScreen
import io.github.rdiecv.paradiddle.ui.screens.RudimentListScreen
import io.github.rdiecv.paradiddle.ui.screens.SettingsScreen
import io.github.rdiecv.paradiddle.ui.screens.app

@Composable
fun ParadiddleNavHost() {
    val nav = rememberNavController()
    val app = app()
    fun back() { nav.popBackStack() }

    NavHost(nav, startDestination = "list") {
        composable("list") {
            RudimentListScreen(
                onOpen = { nav.navigate("detail/$it") },
                onLog = { nav.navigate("log") },
                onSettings = { nav.navigate("settings") },
                onCalibrate = { nav.navigate("calibration") },
            )
        }
        composable("detail/{id}", listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val rudiment = app.rudiment(entry.arguments?.getString("id").orEmpty()) ?: return@composable back()
            RudimentDetailScreen(
                rudiment = rudiment,
                onBack = ::back,
                onStart = { bpm, reps, listen -> nav.navigate("practice/${rudiment.id}/$bpm/$reps/$listen") },
            )
        }
        composable(
            "practice/{id}/{bpm}/{reps}/{listen}",
            listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("bpm") { type = NavType.IntType },
                navArgument("reps") { type = NavType.IntType },
                navArgument("listen") { type = NavType.BoolType },
            ),
        ) { entry ->
            val args = entry.arguments ?: return@composable back()
            val rudiment = app.rudiment(args.getString("id").orEmpty()) ?: return@composable back()
            val vm = viewModel {
                PracticeViewModel(app, rudiment, args.getInt("bpm"), args.getInt("reps"), args.getBoolean("listen"))
            }
            PracticeScreen(vm, onBack = ::back)
        }
        composable("calibration") { CalibrationScreen(onBack = ::back) }
        composable("settings") { SettingsScreen(onBack = ::back, onCalibrate = { nav.navigate("calibration") }) }
        composable("log") { LogScreen(onBack = ::back) }
    }
}
