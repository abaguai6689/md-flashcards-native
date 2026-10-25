package com.fest.mdflashcards.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class DeckRoute(val deckId: String)
@Serializable data class ReviewRoute(val deckId: String, val resume: Boolean = false)
@Serializable data class QuickRoute(val deckId: String)
@Serializable data class BrowseRoute(val deckId: String)
@Serializable data object StatsRoute
@Serializable data object SettingsRoute
