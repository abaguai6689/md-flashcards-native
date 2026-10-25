package com.fest.mdflashcards

import android.app.Application
import com.fest.mdflashcards.data.FlashcardRepository

class MdFlashcardsApplication : Application() {
    lateinit var repository: FlashcardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FlashcardRepository(this)
    }
}
