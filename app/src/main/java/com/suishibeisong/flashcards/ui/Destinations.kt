package com.suishibeisong.flashcards.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.suishibeisong.flashcards.ui.components.FloatingNavBar
import com.suishibeisong.flashcards.ui.components.MainTab
import com.suishibeisong.flashcards.ui.pages.BrowseScreen
import com.suishibeisong.flashcards.ui.pages.DataSettingsScreen
import com.suishibeisong.flashcards.ui.pages.DeckCardsScreen
import com.suishibeisong.flashcards.ui.pages.DeckScreen
import com.suishibeisong.flashcards.ui.pages.GeneralSettingsScreen
import com.suishibeisong.flashcards.ui.pages.HomeScreen
import com.suishibeisong.flashcards.ui.pages.QuickScreen
import com.suishibeisong.flashcards.ui.pages.ReadingSettingsScreen
import com.suishibeisong.flashcards.ui.pages.ReviewScreen
import com.suishibeisong.flashcards.ui.pages.SettingsScreen
import com.suishibeisong.flashcards.ui.pages.StatsScreen
import com.suishibeisong.flashcards.viewmodel.BrowseViewModel
import com.suishibeisong.flashcards.viewmodel.DeckViewModel
import com.suishibeisong.flashcards.viewmodel.HomeViewModel
import com.suishibeisong.flashcards.viewmodel.QuickViewModel
import com.suishibeisong.flashcards.viewmodel.ReviewViewModel
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel
import com.suishibeisong.flashcards.viewmodel.StatsViewModel
import com.xah.navigation.model.dest.Destination
import com.xah.navigation.util.LocalNavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

fun deckKey(deckId: String) = "deck_$deckId"
fun deckCardsKey(deckId: String, category: String) = "deck_cards_${deckId}_$category"
fun reviewKey(deckId: String, resume: Boolean = false) = "review_${deckId}_$resume"
fun quickKey(deckId: String) = "quick_$deckId"
fun browseKey(deckId: String) = "browse_$deckId"

object RootDestination : Destination() {
    override val key = "root"

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val pager = rememberPagerState(pageCount = { 3 })
        val hazeState = androidx.compose.runtime.remember { HazeState() }
        val scope = rememberCoroutineScope()

        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 2,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            ) { page ->
                when (page) {
                    0 -> {
                        val vm = appViewModel("home") { HomeViewModel(deps.repo) }
                        HomeScreen(
                            vm = vm,
                            snackbar = deps.snackbar,
                            onOpenDeck = { nav.push(DeckDestination(it)) },
                            onReview = {
                                val deckId = vm.session()?.deckIds?.firstOrNull()
                                if (deckId != null) nav.push(ReviewDestination(deckId, resume = true))
                            },
                            onQuick = { nav.push(QuickDestination(it)) },
                            onBrowse = { nav.push(BrowseDestination(it)) },
                        )
                    }
                    1 -> {
                        val vm = appViewModel("stats") { StatsViewModel(deps.repo) }
                        StatsScreen(vm = vm)
                    }
                    else -> {
                        val vm = appViewModel("settings") { SettingsViewModel(deps.repo) }
                        SettingsScreen(
                            vm = vm,
                            updateVm = deps.updateVm,
                            onOpenGeneral = { nav.push(GeneralSettingsDestination) },
                            onOpenReading = { nav.push(ReadingSettingsDestination) },
                            onOpenData = { nav.push(DataSettingsDestination) },
                        )
                    }
                }
            }
            FloatingNavBar(
                current = MainTab.entries[pager.currentPage],
                onSelect = { tab -> scope.launch { pager.animateScrollToPage(tab.ordinal) } },
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

data class DeckDestination(val deckId: String) : Destination() {
    override val key = deckKey(deckId)

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("deck-$deckId") { DeckViewModel(deps.repo, deckId) }
        DeckScreen(
            vm = vm,
            deckId = deckId,
            snackbar = deps.snackbar,
            onBack = { nav.pop() },
            onStartReview = { nav.push(ReviewDestination(deckId)) },
            onQuick = { nav.push(QuickDestination(deckId)) },
            onBrowse = { nav.push(BrowseDestination(deckId)) },
            onOpenCategory = { nav.push(DeckCardsDestination(deckId, it)) },
        )
    }
}

data class DeckCardsDestination(val deckId: String, val category: String) : Destination() {
    override val key = deckCardsKey(deckId, category)

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("deck-cards-$deckId") { DeckViewModel(deps.repo, deckId) }
        DeckCardsScreen(vm = vm, category = category, onBack = { nav.pop() })
    }
}

data class ReviewDestination(val deckId: String, val resume: Boolean = false) : Destination() {
    override val key = reviewKey(deckId, resume)

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("review-$deckId") { ReviewViewModel(deps.repo) }
        ReviewScreen(vm = vm, deckId = deckId, onExit = { nav.pop() })
    }
}

data class QuickDestination(val deckId: String) : Destination() {
    override val key = quickKey(deckId)

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("quick-$deckId") { QuickViewModel(deps.repo, deckId) }
        QuickScreen(vm = vm, onBack = { nav.pop() })
    }
}

data class BrowseDestination(val deckId: String) : Destination() {
    override val key = browseKey(deckId)

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("browse-$deckId") { BrowseViewModel(deps.repo, deckId) }
        BrowseScreen(vm = vm, snackbar = deps.snackbar, onBack = { nav.pop() })
    }
}

object GeneralSettingsDestination : Destination() {
    override val key = "settings_general"

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("settings-general") { SettingsViewModel(deps.repo) }
        GeneralSettingsScreen(vm = vm, onBack = { nav.pop() })
    }
}

object ReadingSettingsDestination : Destination() {
    override val key = "settings_reading"

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("settings-reading") { SettingsViewModel(deps.repo) }
        ReadingSettingsScreen(vm = vm, onBack = { nav.pop() })
    }
}

object DataSettingsDestination : Destination() {
    override val key = "settings_data"

    @Composable
    override fun Content() {
        val deps = LocalAppDeps.current
        val nav = LocalNavController.current
        val vm = appViewModel("settings-data") { SettingsViewModel(deps.repo) }
        DataSettingsScreen(vm = vm, snackbar = deps.snackbar, onBack = { nav.pop() })
    }
}
