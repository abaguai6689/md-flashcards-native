package com.suishibeisong.flashcards.domain

data class ParsedCard(
    val id: String,
    val title: String,
    val question: String,
    val answer: String,
    val path: List<String>,
    val chapter: String,
)

data class ParsedDeck(
    val id: String,
    val name: String,
    val title: String,
    val mode: String,
    val qstyle: String,
    val cards: List<ParsedCard>,
)

data class ParseResult(
    val version: String,
    val title: String,
    val cards: List<ParsedCard>,
    val modeUsed: String,
    val headingCount: Int,
    val charCount: Int,
)

data class ParseOptions(
    val mode: String = "auto",
    val maxCardLen: Int = 1600,
    val useQA: Boolean = true,
    val qstyle: String = "prompt",
    val lang: String = "zh-Hans",
    val depth: Int = 0,
)
