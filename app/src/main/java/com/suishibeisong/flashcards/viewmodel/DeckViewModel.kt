package com.suishibeisong.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.data.CardStat
import com.suishibeisong.flashcards.data.Deck
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.data.ProgressRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DeckUiState(
    val deck: Deck? = null,
    val stats: Map<String, CardStat> = emptyMap(),
    val known: Int = 0,
    val due: Int = 0,
    val chapters: List<String> = emptyList(),
)

class DeckViewModel(private val repo: FlashcardRepository, private val deckId: String) : ViewModel() {
    val ui: StateFlow<DeckUiState> = repo.state.map { state ->
        val deck = state.decks.find { it.id == deckId }
        val chapters = deck?.cards?.map { ProgressRules.chapterOf(it) }?.distinct()?.sorted().orEmpty()
        DeckUiState(
            deck = deck,
            stats = state.stats,
            known = deck?.let { ProgressRules.deckKnown(it, state.stats) } ?: 0,
            due = deck?.let { ProgressRules.deckDue(it, state.stats) } ?: 0,
            chapters = chapters,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckUiState())

    fun startReview(count: Int, weakOnly: Boolean, chapter: String, statusFilter: String = "all"): Boolean =
        repo.startReview(deckId, count, weakOnly, chapter, statusFilter)

    fun rename(name: String) = repo.renameDeck(deckId, name)
    fun reset() = repo.resetDeckProgress(deckId)
    fun delete() = repo.deleteDeck(deckId)
    fun cardById(id: String): com.suishibeisong.flashcards.data.Card? = repo.currentState.decks.find { it.id == deckId }?.cards?.find { it.id == id }
}
