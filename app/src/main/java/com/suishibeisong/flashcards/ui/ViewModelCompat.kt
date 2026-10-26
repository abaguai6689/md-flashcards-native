package com.suishibeisong.flashcards.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suishibeisong.flashcards.MdFlashcardsApplication
import com.suishibeisong.flashcards.data.FlashcardRepository

@Composable
fun repository(): FlashcardRepository {
    val app = LocalContext.current.applicationContext as MdFlashcardsApplication
    return app.repository
}

class VmFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

@Composable
inline fun <reified T : ViewModel> appViewModel(key: String? = null, crossinline create: () -> T): T {
    return viewModel(key = key, factory = VmFactory { create() })
}
