package com.fest.mdflashcards.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fest.mdflashcards.data.FlashcardRepository
import com.fest.mdflashcards.ui.components.FloatingNavBar
import com.fest.mdflashcards.ui.components.MainTab
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.stringsFor
import com.fest.mdflashcards.ui.navigation.BrowseRoute
import com.fest.mdflashcards.ui.navigation.DeckRoute
import com.fest.mdflashcards.ui.navigation.HomeRoute
import com.fest.mdflashcards.ui.navigation.QuickRoute
import com.fest.mdflashcards.ui.navigation.ReviewRoute
import com.fest.mdflashcards.ui.navigation.SettingsRoute
import com.fest.mdflashcards.ui.navigation.StatsRoute
import com.fest.mdflashcards.ui.pages.BrowseScreen
import com.fest.mdflashcards.ui.pages.DeckScreen
import com.fest.mdflashcards.ui.pages.HomeScreen
import com.fest.mdflashcards.ui.pages.QuickScreen
import com.fest.mdflashcards.ui.pages.ReviewScreen
import com.fest.mdflashcards.ui.pages.SettingsScreen
import com.fest.mdflashcards.ui.pages.StatsScreen
import com.fest.mdflashcards.ui.theme.MdFlashcardsTheme
import com.fest.mdflashcards.ui.theme.ReadingStyle
import com.fest.mdflashcards.viewmodel.BrowseViewModel
import com.fest.mdflashcards.viewmodel.DeckViewModel
import com.fest.mdflashcards.viewmodel.HomeViewModel
import com.fest.mdflashcards.viewmodel.QuickViewModel
import com.fest.mdflashcards.viewmodel.ReviewViewModel
import com.fest.mdflashcards.viewmodel.SettingsViewModel
import com.fest.mdflashcards.viewmodel.StatsViewModel

@Composable
fun MdFlashcardsApp() {
    val repo = repository()
    val state by repo.state.collectAsStateWithLifecycle()
    val strings = stringsFor(state.settings.lang)
    val reading = ReadingStyle(
        fontScale = when (state.settings.reading.fs) {
            "s" -> 0.92f
            "l" -> 1.12f
            else -> 1f
        },
        lineHeightScale = when (state.settings.reading.lh) {
            1 -> 0.86f
            3 -> 1.2f
            else -> 1f
        },
        widthFactor = when (state.settings.reading.cw) {
            1 -> 0.82f
            3 -> 1f
            else -> 0.92f
        },
    )
    CompositionLocalProvider(LocalStrings provides strings) {
        MdFlashcardsTheme(themeMode = state.settings.theme, reading = reading) {
            AppContent(repo)
        }
    }
}

@Composable
private fun AppContent(repo: FlashcardRepository) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val topLevel = route.contains("HomeRoute") || route.contains("StatsRoute") || route.contains("SettingsRoute")
    val currentTab = when {
        route.contains("StatsRoute") -> MainTab.STATS
        route.contains("SettingsRoute") -> MainTab.SETTINGS
        else -> MainTab.HOME
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                val vm = appViewModel("home") { HomeViewModel(repo) }
                HomeScreen(
                    vm = vm,
                    snackbar = snackbar,
                    onOpenDeck = { nav.navigate(DeckRoute(it)) },
                    onReview = {
                        val deckId = vm.session()?.deckIds?.firstOrNull()
                        if (deckId != null) nav.navigate(ReviewRoute(deckId, resume = true))
                    },
                    onQuick = { nav.navigate(QuickRoute(it)) },
                    onBrowse = { nav.navigate(BrowseRoute(it)) },
                )
            }
            composable<DeckRoute> { entry ->
                val deckId = entry.toRoute<DeckRoute>().deckId
                val vm = appViewModel("deck-$deckId") { DeckViewModel(repo, deckId) }
                DeckScreen(
                    vm = vm,
                    snackbar = snackbar,
                    onBack = { nav.popBackStack() },
                    onStartReview = { nav.navigate(ReviewRoute(deckId)) },
                    onQuick = { nav.navigate(QuickRoute(deckId)) },
                    onBrowse = { nav.navigate(BrowseRoute(deckId)) },
                )
            }
            composable<ReviewRoute> { entry ->
                val deckId = entry.toRoute<ReviewRoute>().deckId
                val vm = appViewModel("review-$deckId") { ReviewViewModel(repo) }
                ReviewScreen(vm = vm, deckId = deckId, onExit = { nav.popBackStack() })
            }
            composable<QuickRoute> { entry ->
                val deckId = entry.toRoute<QuickRoute>().deckId
                val vm = appViewModel("quick-$deckId") { QuickViewModel(repo, deckId) }
                QuickScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable<BrowseRoute> { entry ->
                val deckId = entry.toRoute<BrowseRoute>().deckId
                val vm = appViewModel("browse-$deckId") { BrowseViewModel(repo, deckId) }
                BrowseScreen(vm = vm, snackbar = snackbar, onBack = { nav.popBackStack() })
            }
            composable<StatsRoute> {
                val vm = appViewModel("stats") { StatsViewModel(repo) }
                StatsScreen(vm = vm)
            }
            composable<SettingsRoute> {
                val vm = appViewModel("settings") { SettingsViewModel(repo) }
                SettingsScreen(vm = vm, snackbar = snackbar)
            }
        }

        if (topLevel) {
            FloatingNavBar(current = currentTab, onSelect = { tab ->
                val target = when (tab) {
                    MainTab.HOME -> HomeRoute
                    MainTab.STATS -> StatsRoute
                    MainTab.SETTINGS -> SettingsRoute
                }
                nav.navigate(target) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
