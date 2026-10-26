package com.suishibeisong.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.data.Deck
import com.suishibeisong.flashcards.data.FlashcardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

class QuickViewModel(repo: FlashcardRepository, private val deckId: String) : ViewModel() {
    val deck: StateFlow<Deck?> = repo.state.map { state -> state.decks.find { it.id == deckId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun filter(deck: Deck?, query: String): List<com.suishibeisong.flashcards.data.Card> {
        if (deck == null) return emptyList()
        if (query.isBlank()) return deck.cards
        val q = query.lowercase(Locale.getDefault())
        return deck.cards.filter {
            it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.question.lowercase(Locale.getDefault()).contains(q) ||
                it.answer.lowercase(Locale.getDefault()).contains(q)
        }
    }
}
