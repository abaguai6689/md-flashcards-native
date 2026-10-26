package com.fest.mdflashcards

import com.fest.mdflashcards.data.AppState
import com.fest.mdflashcards.data.Card
import com.fest.mdflashcards.data.CardStat
import com.fest.mdflashcards.data.Deck
import com.fest.mdflashcards.data.Settings
import com.fest.mdflashcards.data.UpdateChecker
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateSerializationTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    @Test
    fun roundTripKeepsProgressAndSettings() {
        val state = AppState(
            decks = listOf(
                Deck(
                    id = "d1", name = "网络", title = "计算机网络", createdAt = 1L,
                    cards = listOf(Card(id = "c1", title = "TCP", question = "三次握手？", answer = "SYN/ACK")),
                ),
            ),
            stats = mapOf("c1" to CardStat(status = "known", reviews = 3, okCount = 2, badCount = 1, box = 2, dueAt = 99L, intervalDays = 3)),
            daily = mapOf("2026-10-25" to 5),
            settings = Settings(lang = "en", theme = "dark"),
        )
        val text = json.encodeToString(AppState.serializer(), state)
        val back = json.decodeFromString(AppState.serializer(), text)
        assertEquals("网络", back.decks.first().name)
        assertEquals("known", back.stats["c1"]!!.status)
        assertEquals(2, back.stats["c1"]!!.box)
        assertEquals(99L, back.stats["c1"]!!.dueAt)
        assertEquals(5, back.daily["2026-10-25"])
        assertEquals("en", back.settings.lang)
    }

    @Test
    fun comparesReleaseVersionsNumerically() {
        assertTrue(UpdateChecker.compareVersions("1.1.0", "1.0.9") > 0)
        assertTrue(UpdateChecker.compareVersions("v1.2.0", "1.1.9") > 0)
        assertTrue(UpdateChecker.compareVersions("1.0.0", "1.0.0") == 0)
        assertTrue(UpdateChecker.compareVersions("1.0.0", "1.1.0") < 0)
    }

    @Test
    fun decodesWebBackupShapeWithUnknownKeys() {
        val web = """
            {
              "version": 1,
              "decks": [{"id":"d1","name":"笔记","title":"笔记","createdAt":1,"mode":"auto","qstyle":"prompt","raw":"x","tags":[],"source":"","bundled":false,
                "cards":[{"id":"c1","title":"题","question":"问？","answer":"答","path":[],"chapter":"","tags":[],"modified":false}]}],
              "stats": {"c1": {"s":"known","n":2,"ok":2,"bad":0,"last":1,"box":1,"dueAt":99,"ivl":1}},
              "daily": {"2026-10-25": 2},
              "settings": {"theme":"dark","lang":"zh-Hans","ai":{"providers":[]}},
              "reviewSession": null
            }
        """.trimIndent()
        val state = json.decodeFromString(AppState.serializer(), web)
        assertEquals(1, state.decks.size)
        assertEquals("known", state.stats["c1"]!!.status)
        assertTrue(state.settings.theme == "dark")
    }
}
