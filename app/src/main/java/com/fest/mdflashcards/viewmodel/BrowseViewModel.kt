package com.fest.mdflashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fest.mdflashcards.data.Card
import com.fest.mdflashcards.data.Deck
import com.fest.mdflashcards.data.FlashcardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BrowseViewModel(private val repo: FlashcardRepository, private val deckId: String) : ViewModel() {
    val deck: StateFlow<Deck?> = repo.state.map { state -> state.decks.find { it.id == deckId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(card: Card) = repo.updateCard(deckId, card)
    fun delete(cardId: String) = repo.deleteCard(deckId, cardId)
    fun reset(cardId: String) = repo.resetCardProgress(cardId)
}
