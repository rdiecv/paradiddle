// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.ui.screens.CalibrationScreen
import io.github.rdiecv.paradiddle.ui.screens.GrooveDetailScreen
import io.github.rdiecv.paradiddle.ui.screens.GrooveViewModel
import io.github.rdiecv.paradiddle.ui.screens.KitGuideScreen
import io.github.rdiecv.paradiddle.ui.screens.LogScreen
import io.github.rdiecv.paradiddle.ui.screens.PracticeScreen
import io.github.rdiecv.paradiddle.ui.screens.PracticeViewModel
import io.github.rdiecv.paradiddle.ui.screens.RudimentDetailScreen
import io.github.rdiecv.paradiddle.ui.screens.RudimentListScreen
import io.github.rdiecv.paradiddle.ui.screens.SettingsScreen
import io.github.rdiecv.paradiddle.ui.screens.StylesScreen
import io.github.rdiecv.paradiddle.ui.screens.app

/** Top-level screens, shown as tabs in the bottom bar. */
private enum class Tab(val route: String, @StringRes val label: Int) {
    RUDIMENTS("list", R.string.tab_rudiments),
    STYLES("styles", R.string.tab_styles),
    KIT("kit", R.string.tab_kit),
}

@Composable
fun ParadiddleNavHost() {
    val nav = rememberNavController()
    val app = app()
    fun back() { nav.popBackStack() }
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route

    Scaffold(
        bottomBar = { if (Tab.entries.any { it.route == route }) TabBar(nav, route) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        NavHost(nav, startDestination = Tab.RUDIMENTS.route, Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Tab.RUDIMENTS.route) {
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
            composable(Tab.STYLES.route) { StylesScreen(onOpen = { nav.navigate("groove/$it") }) }
            composable("groove/{id}", listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val groove = app.groove(entry.arguments?.getString("id").orEmpty()) ?: return@composable back()
                val vm = viewModel { GrooveViewModel(groove) }
                GrooveDetailScreen(vm, onBack = ::back)
            }
            composable(Tab.KIT.route) { KitGuideScreen() }
            composable("calibration") { CalibrationScreen(onBack = ::back) }
            composable("settings") { SettingsScreen(onBack = ::back, onCalibrate = { nav.navigate("calibration") }) }
            composable("log") { LogScreen(onBack = ::back) }
        }
    }
}

@Composable
private fun TabBar(nav: NavHostController, route: String?) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (tab) {
                        Tab.RUDIMENTS -> Icon(Icons.AutoMirrored.Filled.List, null)
                        Tab.STYLES -> Icon(painterResource(R.drawable.ic_music_note), null)
                        Tab.KIT -> Icon(Icons.Filled.Build, null)
                    }
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
