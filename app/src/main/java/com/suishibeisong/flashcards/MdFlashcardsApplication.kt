package com.suishibeisong.flashcards

import android.app.Application
import com.suishibeisong.flashcards.data.FlashcardRepository

class MdFlashcardsApplication : Application() {
    lateinit var repository: FlashcardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FlashcardRepository(this)
    }
}
