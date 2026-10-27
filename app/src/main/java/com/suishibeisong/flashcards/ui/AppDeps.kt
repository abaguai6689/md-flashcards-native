package com.suishibeisong.flashcards.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.viewmodel.UpdateViewModel

data class AppDeps(
    val repo: FlashcardRepository,
    val updateVm: UpdateViewModel,
    val snackbar: SnackbarHostState,
)

val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("LocalAppDeps is only available inside MdFlashcardsApp")
}
