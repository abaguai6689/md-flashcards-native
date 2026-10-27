package com.suishibeisong.flashcards.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class DeckRoute(val deckId: String)
@Serializable data class DeckCardsRoute(val deckId: String, val category: String)
@Serializable data class ReviewRoute(val deckId: String, val resume: Boolean = false)
@Serializable data class QuickRoute(val deckId: String)
@Serializable data class BrowseRoute(val deckId: String)
@Serializable data object StatsRoute
@Serializable data object SettingsRoute
@Serializable data object GeneralSettingsRoute
@Serializable data object ReadingSettingsRoute
@Serializable data object DataSettingsRoute

@Serializable data object RootRoute
