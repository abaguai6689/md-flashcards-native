package com.suishibeisong.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.data.Card
import com.suishibeisong.flashcards.data.CardStat
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.data.ReviewSession
import com.suishibeisong.flashcards.domain.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReviewUiState(
    val deckName: String = "",
    val phase: Int = 1,
    val total: Int = 0,
    val rated: Int = 0,
    val okCount: Int = 0,
    val fuzzyCount: Int = 0,
    val badCount: Int = 0,
    val card: Card? = null,
    val revealed: Boolean = false,
    val finished: Boolean = false,
    val empty: Boolean = false,
    val canUndo: Boolean = false,
) {
    val left: Int get() = (total - rated).coerceAtLeast(0)
}

private data class UndoSnapshot(
    val cardId: String,
    val previousStat: CardStat?,
    val queue: List<String>,
    val round2: List<String>,
    val phase: Int,
    val total: Int,
    val rated: Int,
    val okCount: Int,
    val fuzzyCount: Int,
    val badCount: Int,
    val dayKey: String,
)

class ReviewViewModel(private val repo: FlashcardRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ReviewUiState())
    val ui: StateFlow<ReviewUiState> = _ui.asStateFlow()

    private var queue = ArrayDeque<String>()
    private var round2 = ArrayDeque<String>()
    private var current: Card? = null
    private var phase = 1
    private var total = 0
    private var rated = 0
    private var okCount = 0
    private var fuzzyCount = 0
    private var badCount = 0
    private var finished = false
    private var undoSnapshot: UndoSnapshot? = null

    init {
        val session = repo.currentState.reviewSession
        if (session == null) {
            _ui.value = ReviewUiState(empty = true)
        } else {
            queue = ArrayDeque(session.queueIds)
            round2 = ArrayDeque(session.round2Ids)
            phase = session.phase
            total = session.total
            rated = session.rated
            okCount = session.okCount
            fuzzyCount = session.fuzzyCount
            badCount = session.badCount
            current = session.currentId?.let(::resolveCard)
            if (current == null) current = nextFromQueue()
            if (current == null) {
                _ui.value = ReviewUiState(empty = true, deckName = session.deckName)
            } else {
                publish()
            }
        }
    }

    private fun resolveCard(id: String): Card? {
        val state = repo.currentState
        for (deck in state.decks) {
            deck.cards.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    private fun nextFromQueue(): Card? {
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            resolveCard(id)?.let { return it }
        }
        if (round2.isNotEmpty()) {
            phase = 2
            queue = ArrayDeque(round2)
            round2.clear()
            total = queue.size
            rated = 0
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                resolveCard(id)?.let { return it }
            }
        }
        return null
    }

    private fun publish() {
        val card = current
        _ui.value = ReviewUiState(
            deckName = repo.currentState.reviewSession?.deckName ?: _ui.value.deckName,
            phase = phase,
            total = total,
            rated = rated,
            okCount = okCount,
            fuzzyCount = fuzzyCount,
            badCount = badCount,
            card = card,
            revealed = _ui.value.revealed && card?.id == _ui.value.card?.id,
            finished = finished,
            empty = !finished && card == null,
            canUndo = undoSnapshot != null,
        )
        if (card == null && !finished) {
            // Nothing resolvable left: end the session.
            finish()
        }
    }

    fun reveal() {
        val card = current ?: return
        _ui.value = _ui.value.copy(card = card, revealed = true)
    }

    fun hideAnswer() {
        _ui.value = _ui.value.copy(revealed = false)
    }

    fun rate(rating: Rating) {
        val card = current ?: return
        if (!_ui.value.revealed) return
        val previous = repo.currentState.stats[card.id]
        val dayKey = LocalDate.now().toString()
        undoSnapshot = UndoSnapshot(
            cardId = card.id,
            previousStat = previous,
            queue = queue.toList(),
            round2 = round2.toList(),
            phase = phase,
            total = total,
            rated = rated,
            okCount = okCount,
            fuzzyCount = fuzzyCount,
            badCount = badCount,
            dayKey = dayKey,
        )
        repo.recordRating(card.id, rating)
        when (rating) {
            Rating.GOT -> okCount++
            Rating.FUZZY -> { fuzzyCount++; if (phase == 1) round2.addLast(card.id) }
            Rating.FORGOT -> { badCount++; if (phase == 1) round2.addLast(card.id) }
        }
        rated++
        current = nextFromQueue()
        saveSession()
        if (current == null) {
            finish()
        } else {
            _ui.value = _ui.value.copy(revealed = false)
            publish()
        }
    }

    fun undo(): Boolean {
        val snapshot = undoSnapshot ?: return false
        repo.restoreRating(snapshot.cardId, snapshot.previousStat, snapshot.dayKey)
        queue = ArrayDeque(snapshot.queue)
        round2 = ArrayDeque(snapshot.round2)
        phase = snapshot.phase
        total = snapshot.total
        rated = snapshot.rated
        okCount = snapshot.okCount
        fuzzyCount = snapshot.fuzzyCount
        badCount = snapshot.badCount
        current = resolveCard(snapshot.cardId)
        undoSnapshot = null
        finished = false
        saveSession()
        _ui.value = _ui.value.copy(revealed = false, finished = false)
        publish()
        return true
    }

    private fun saveSession() {
        if (finished) return
        val card = current ?: return
        repo.saveReviewSession(
            ReviewSession(
                deckIds = repo.currentState.reviewSession?.deckIds ?: emptyList(),
                deckName = _ui.value.deckName,
                phase = phase,
                total = total,
                rated = rated,
                okCount = okCount,
                fuzzyCount = fuzzyCount,
                badCount = badCount,
                queueIds = queue.toList(),
                round2Ids = round2.toList(),
                currentId = card.id,
            ),
        )
    }

    private fun finish() {
        finished = true
        repo.clearReviewSession()
        _ui.value = _ui.value.copy(
            card = null,
            revealed = false,
            finished = true,
            empty = false,
            canUndo = undoSnapshot != null,
            total = total,
            rated = rated,
            okCount = okCount,
            fuzzyCount = fuzzyCount,
            badCount = badCount,
        )
    }

    fun restart(deckId: String, count: Int, weakOnly: Boolean, chapter: String) {
        if (repo.startReview(deckId, count, weakOnly, chapter)) {
            resetFromSession()
        } else {
            _ui.value = ReviewUiState(empty = true)
        }
    }

    private fun resetFromSession() {
        val session = repo.currentState.reviewSession ?: return
        queue = ArrayDeque(session.queueIds)
        round2 = ArrayDeque(session.round2Ids)
        phase = session.phase
        total = session.total
        rated = session.rated
        okCount = session.okCount
        fuzzyCount = session.fuzzyCount
        badCount = session.badCount
        undoSnapshot = null
        finished = false
        current = session.currentId?.let(::resolveCard) ?: nextFromQueue()
        _ui.value = _ui.value.copy(revealed = false)
        publish()
    }

    override fun onCleared() {
        // Session is saved after every action; nothing else to flush synchronously.
        super.onCleared()
    }
}
