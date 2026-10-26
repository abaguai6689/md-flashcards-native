package com.suishibeisong.flashcards.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.data.AppState
import com.suishibeisong.flashcards.data.CardStat
import com.suishibeisong.flashcards.data.Deck
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.data.ImportSummary
import com.suishibeisong.flashcards.data.ProgressRules
import com.suishibeisong.flashcards.data.ReviewSession
import com.suishibeisong.flashcards.domain.ParseOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DeckItem(
    val deck: Deck,
    val cards: Int,
    val known: Int,
    val due: Int,
)

data class HomeUiState(
    val decks: List<DeckItem> = emptyList(),
    val allDecks: List<DeckItem> = emptyList(),
    val totalCards: Int = 0,
    val totalDue: Int = 0,
    val totalKnown: Int = 0,
    val today: Int = 0,
    val session: ReviewSession? = null,
    val query: String = "",
    val sort: String = "recent",
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: FlashcardRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow("recent")

    val uiState: StateFlow<HomeUiState> = combine(repo.state, query, sort) { state, q, s ->
        build(state, q, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        sort.value = repo.currentState.settings.deckSort.ifBlank { "recent" }
    }

    private fun build(state: AppState, query: String, sort: String): HomeUiState {
        val items = state.decks.map { d ->
            DeckItem(
                deck = d,
                cards = d.cards.size,
                known = ProgressRules.deckKnown(d, state.stats),
                due = ProgressRules.deckDue(d, state.stats),
            )
        }
        val filtered = items.filter { it.deck.name.contains(query, ignoreCase = true) || it.deck.title.contains(query, ignoreCase = true) }
        val sorted = when (sort) {
            "name" -> filtered.sortedBy { it.deck.name.lowercase() }
            "cards" -> filtered.sortedByDescending { it.cards }
            "due" -> filtered.sortedByDescending { it.due }
            else -> filtered.sortedByDescending { it.deck.createdAt }
        }
        val todayKey = LocalDate.now().toString()
        return HomeUiState(
            decks = sorted,
            allDecks = items,
            totalCards = items.sumOf { it.cards },
            totalDue = items.sumOf { it.due },
            totalKnown = items.sumOf { it.known },
            today = state.daily[todayKey] ?: 0,
            session = state.reviewSession,
            query = query,
            sort = sort,
            loading = false,
        )
    }

    fun setQuery(value: String) { query.value = value }
    fun setSort(value: String) {
        sort.value = value
        repo.setDeckSort(value)
    }
    fun deck(deckId: String): Deck? = repo.currentState.decks.find { it.id == deckId }
    fun stats(deckId: String): Map<String, CardStat> = repo.currentState.stats
    fun session(): ReviewSession? = repo.currentState.reviewSession

    fun renameDeck(deckId: String, name: String) = repo.renameDeck(deckId, name)
    fun deleteDeck(deckId: String) = repo.deleteDeck(deckId)
    fun resetDeck(deckId: String) = repo.resetDeckProgress(deckId)

    fun startReview(deckId: String, count: Int, weakOnly: Boolean, chapter: String, statusFilter: String = "all"): Boolean =
        repo.startReview(deckId, count, weakOnly, chapter, statusFilter)

    suspend fun importUri(uri: Uri, options: ParseOptions, imageRenames: Map<String, String> = emptyMap()): ImportSummary =
        repo.importUri(uri, options, imageRenames = imageRenames)

    suspend fun importText(name: String, text: String, options: ParseOptions, imageRenames: Map<String, String> = emptyMap()): ImportSummary =
        repo.importMarkdown(name, text, options, imageRenames = imageRenames)

    suspend fun importImages(uris: List<Uri>) = repo.importImages(uris)

    fun error(): StateFlow<String?> = repo.lastError
    fun clearError() = repo.clearError()
}
