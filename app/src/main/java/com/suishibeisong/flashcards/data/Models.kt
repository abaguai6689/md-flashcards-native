package com.suishibeisong.flashcards.data

import com.suishibeisong.flashcards.domain.CardParser
import com.suishibeisong.flashcards.domain.CardStatLike
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class Card(
    val id: String = "",
    val title: String = "",
    val question: String = "",
    val answer: String = "",
    val path: List<String> = emptyList(),
    val chapter: String = "",
    val tags: List<String> = emptyList(),
    val modified: Boolean = false,
)

@Serializable
data class Deck(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val createdAt: Long = 0L,
    val mode: String = "auto",
    val qstyle: String = "prompt",
    val raw: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "",
    val bundled: Boolean = false,
    val cloudId: String = "",
    val cloudVersion: Int = 0,
    val cloudDescription: String = "",
    val cloudUploader: String = "",
    val cloudUploadedAt: String = "",
    val cloudSize: Long = 0L,
    val cloudFileName: String = "",
    val cards: List<Card> = emptyList(),
)

@Serializable
data class CardStat(
    @SerialName("s") var status: String = "new",
    @SerialName("n") var reviews: Int = 0,
    @SerialName("ok") var okCount: Int = 0,
    @SerialName("bad") var badCount: Int = 0,
    @SerialName("last") var lastReviewedAt: Long = 0L,
    @SerialName("box") override val box: Int = 0,
    @SerialName("dueAt") override val dueAt: Long = 0L,
    @SerialName("ivl") var intervalDays: Int = 0,
) : CardStatLike

@Serializable
data class ReadingSettings(
    val fs: String = "m",
    val lh: Int = 2,
    val cw: Int = 2,
)

@Serializable
data class Settings(
    val mode: String = "auto",
    val gran: String = "mid",
    val qstyle: String = "prompt",
    val filter: String = "all",
    val theme: String = "system",
    val lang: String = "zh",
    val statsView: String = "heat",
    val checkUpdateAtStartup: Boolean = true,
    val reading: ReadingSettings = ReadingSettings(),
    val tagFilter: String = "",
    val deckSort: String = "recent",
)

@Serializable
data class ReviewSession(
    val deckIds: List<String> = emptyList(),
    val filter: String = "all",
    val deckName: String = "",
    val phase: Int = 1,
    val total: Int = 0,
    val rated: Int = 0,
    val okCount: Int = 0,
    val fuzzyCount: Int = 0,
    val badCount: Int = 0,
    val queueIds: List<String> = emptyList(),
    val round2Ids: List<String> = emptyList(),
    val currentId: String? = null,
    val savedAt: Long = 0L,
)

@Serializable
data class AppState(
    val version: Int = 1,
    val decks: List<Deck> = emptyList(),
    val stats: Map<String, CardStat> = emptyMap(),
    val daily: Map<String, Int> = emptyMap(),
    val reviewSession: ReviewSession? = null,
    val lastBackupAt: Long = 0L,
    val lastChangeAt: Long = 0L,
    val backupRemindDismissedAt: Long = 0L,
    val settings: Settings = Settings(),
)

object ProgressRules {
    fun endOfTodayMs(now: Long = System.currentTimeMillis()): Long =
        LocalDate.now().atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun isDue(stat: CardStat?, now: Long = System.currentTimeMillis()): Boolean {
        if (stat == null || stat.box == 0) return true
        return stat.dueAt <= endOfTodayMs(now)
    }

    fun isKnown(stat: CardStat?): Boolean = stat?.status == "known"

    /** Category used by the deck header list: all / known / new / marked. */
    fun matchesCategory(stat: CardStat?, category: String): Boolean = when (category) {
        "known" -> isKnown(stat)
        "new" -> stat == null || stat.reviews == 0
        "marked" -> stat != null && stat.reviews > 0 && !isKnown(stat)
        else -> true
    }

    fun isWeak(stat: CardStat?): Boolean {
        if (stat == null || stat.reviews <= 0) return false
        if (stat.badCount > 0) return true
        return stat.box == 1
    }

    fun isTodo(stat: CardStat?, now: Long = System.currentTimeMillis()): Boolean {
        if (!isKnown(stat)) return true
        return isDue(stat, now)
    }

    fun deckKnown(deck: Deck, stats: Map<String, CardStat>): Int =
        deck.cards.count { isKnown(stats[it.id]) }

    fun deckDue(deck: Deck, stats: Map<String, CardStat>, now: Long = System.currentTimeMillis()): Int =
        deck.cards.count { isDue(stats[it.id], now) }

    fun chapterOf(card: Card): String {
        val parts = ArrayList<String>()
        card.path.forEach {
            val p = it.trim()
            if (p.isNotEmpty() && !parts.contains(p)) parts += p
        }
        val ch = card.chapter.trim()
        if (ch.isNotEmpty() && !parts.contains(ch)) parts += ch
        return if (parts.isEmpty()) "未分类" else parts.joinToString(" / ")
    }
}

fun parsedCardToCard(parsed: com.suishibeisong.flashcards.domain.ParsedCard): Card = Card(
    id = parsed.id,
    title = parsed.title,
    question = parsed.question,
    answer = parsed.answer,
    path = parsed.path,
    chapter = parsed.chapter,
)

fun normalizeDeckNames(name: String): String =
    name.replace(Regex("\\.(md|markdown|txt)$", RegexOption.IGNORE_CASE), "").trim().ifEmpty { "未命名笔记" }

fun deckIdFor(name: String, text: String, cardCount: Int): String =
    "d" + CardParser.hash32("${normalizeDeckNames(name)}|${text.length}|$cardCount")
