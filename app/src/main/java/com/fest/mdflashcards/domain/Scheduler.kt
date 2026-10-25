package com.fest.mdflashcards.domain

object Scheduler {
    val BOX_INTERVALS = intArrayOf(1, 3, 7)
    private const val DAY_MS = 86_400_000L

    data class Result(val box: Int, val intervalDays: Int, val dueAt: Long)

    fun schedule(stat: CardStatLike?, kind: Rating, now: Long): Result {
        val base = stat?.box ?: 0
        return when (kind) {
            Rating.GOT -> {
                val box = minOf(3, base + 1)
                val interval = BOX_INTERVALS[box - 1]
                Result(box, interval, now + interval * DAY_MS)
            }
            Rating.FUZZY -> Result(1, 1, now + DAY_MS)
            Rating.FORGOT -> Result(1, 0, now)
        }
    }

    fun isDue(stat: CardStatLike?, endOfTodayMs: Long): Boolean {
        if (stat == null || stat.box == 0) return true
        return stat.dueAt <= endOfTodayMs
    }
}

enum class Rating { FORGOT, FUZZY, GOT }

interface CardStatLike {
    val box: Int
    val dueAt: Long
}
