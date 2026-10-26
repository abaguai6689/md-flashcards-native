package com.suishibeisong.flashcards.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.ui.components.FloatingNavBar
import com.suishibeisong.flashcards.ui.components.MarkdownText
import com.suishibeisong.flashcards.ui.components.MainTab
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.stringsFor
import com.suishibeisong.flashcards.ui.navigation.BrowseRoute
import com.suishibeisong.flashcards.ui.navigation.DataSettingsRoute
import com.suishibeisong.flashcards.ui.navigation.DeckRoute
import com.suishibeisong.flashcards.ui.navigation.GeneralSettingsRoute
import com.suishibeisong.flashcards.ui.navigation.ReadingSettingsRoute
import com.suishibeisong.flashcards.ui.navigation.QuickRoute
import com.suishibeisong.flashcards.ui.navigation.ReviewRoute
import com.suishibeisong.flashcards.ui.navigation.RootRoute
import com.suishibeisong.flashcards.ui.pages.BrowseScreen
import com.suishibeisong.flashcards.ui.pages.DataSettingsScreen
import com.suishibeisong.flashcards.ui.pages.DeckScreen
import com.suishibeisong.flashcards.ui.pages.GeneralSettingsScreen
import com.suishibeisong.flashcards.ui.pages.ReadingSettingsScreen
import com.suishibeisong.flashcards.ui.pages.HomeScreen
import com.suishibeisong.flashcards.ui.pages.QuickScreen
import com.suishibeisong.flashcards.ui.pages.ReviewScreen
import com.suishibeisong.flashcards.ui.pages.SettingsScreen
import com.suishibeisong.flashcards.ui.pages.StatsScreen
import com.suishibeisong.flashcards.ui.theme.MdFlashcardsTheme
import com.suishibeisong.flashcards.ui.theme.ReadingStyle
import com.suishibeisong.flashcards.viewmodel.BrowseViewModel
import com.suishibeisong.flashcards.viewmodel.DeckViewModel
import com.suishibeisong.flashcards.viewmodel.HomeViewModel
import com.suishibeisong.flashcards.viewmodel.QuickViewModel
import com.suishibeisong.flashcards.viewmodel.ReviewViewModel
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel
import com.suishibeisong.flashcards.viewmodel.StatsViewModel
import com.suishibeisong.flashcards.viewmodel.UpdateStatus
import com.suishibeisong.flashcards.viewmodel.UpdateViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val s = LocalStrings.current
    val appState by repo.state.collectAsStateWithLifecycle()
    val updateVm = appViewModel("update") { UpdateViewModel(repo) }
    val update by updateVm.update.collectAsStateWithLifecycle()
    val backEntry by nav.currentBackStackEntryAsState()
    val atRoot = backEntry?.destination?.route?.contains("RootRoute") == true

    LaunchedEffect(Unit) { updateVm.checkAtStartup() }

    if (update.showDialog && update.status == UpdateStatus.AVAILABLE) {
        AlertDialog(
            onDismissRequest = { updateVm.dismissDialog() },
            title = { Text("${s.updateAvailableTitle} · v${update.latestVersion}") },
            text = {
                Column(Modifier.height(340.dp).verticalScroll(rememberScrollState())) {
                    MarkdownText(
                        markdown = update.notes.ifBlank { s.updateNotesFallback },
                        baseFontSize = 13f,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                    }
                    updateVm.dismissDialog()
                }) { Text(s.updateNow) }
            },
            dismissButton = {
                TextButton(onClick = { updateVm.dismissDialog() }) { Text(s.later) }
            },
        )
    }

    NavHost(navController = nav, startDestination = RootRoute) {
        composable<RootRoute> {
            val pager = rememberPagerState(pageCount = { 3 })
            val hazeState = remember { HazeState() }
            val scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pager,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                ) { page ->
                    when (page) {
                        0 -> {
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
                        1 -> {
                            val vm = appViewModel("stats") { StatsViewModel(repo) }
                            StatsScreen(vm = vm)
                        }
                        else -> {
                            val vm = appViewModel("settings") { SettingsViewModel(repo) }
                            SettingsScreen(
                                vm = vm,
                                updateVm = updateVm,
                                onOpenGeneral = { nav.navigate(GeneralSettingsRoute) },
                                onOpenReading = { nav.navigate(ReadingSettingsRoute) },
                                onOpenData = { nav.navigate(DataSettingsRoute) },
                            )
                        }
                    }
                }
                FloatingNavBar(
                    current = MainTab.entries[pager.currentPage],
                    onSelect = { tab ->
                        scope.launch { pager.animateScrollToPage(tab.ordinal) }
                    },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
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
        composable<GeneralSettingsRoute> {
            val vm = appViewModel("settings-general") { SettingsViewModel(repo) }
            GeneralSettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable<ReadingSettingsRoute> {
            val vm = appViewModel("settings-reading") { SettingsViewModel(repo) }
            ReadingSettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable<DataSettingsRoute> {
            val vm = appViewModel("settings-data") { SettingsViewModel(repo) }
            DataSettingsScreen(vm = vm, snackbar = snackbar, onBack = { nav.popBackStack() })
        }
    }

    if (!appState.settings.enablePredictiveBack && !atRoot) {
        BackHandler { nav.popBackStack() }
    }
}
