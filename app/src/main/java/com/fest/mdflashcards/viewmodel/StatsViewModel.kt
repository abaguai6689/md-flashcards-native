package com.fest.mdflashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fest.mdflashcards.data.FlashcardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUiState(
    val daily: Map<String, Int> = emptyMap(),
    val today: Int = 0,
    val total: Int = 0,
    val streak: Int = 0,
)

class StatsViewModel(repo: FlashcardRepository) : ViewModel() {
    val ui: StateFlow<StatsUiState> = repo.state.map { state ->
        val daily = state.daily
        val todayKey = LocalDate.now().toString()
        var streak = 0
        var d = LocalDate.now()
        while ((daily[d.toString()] ?: 0) > 0) {
            streak++
            d = d.minusDays(1)
        }
        StatsUiState(
            daily = daily,
            today = daily[todayKey] ?: 0,
            total = daily.values.sum(),
            streak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
