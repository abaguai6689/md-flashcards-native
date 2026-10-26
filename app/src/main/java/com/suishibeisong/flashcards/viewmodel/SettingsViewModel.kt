package com.suishibeisong.flashcards.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.data.ImportSummary
import com.suishibeisong.flashcards.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.OutputStream

class SettingsViewModel(private val repo: FlashcardRepository) : ViewModel() {
    val settings: StateFlow<Settings> = repo.state.map { it.settings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setTheme(value: String) = repo.setTheme(value)
    fun setLang(value: String) = repo.setLang(value)
    fun setReading(fs: String? = null, lh: Int? = null, cw: Int? = null) = repo.setReading(fs, lh, cw)
    fun setCheckUpdateAtStartup(value: Boolean) = repo.setCheckUpdateAtStartup(value)
    fun setPredictiveBack(value: Boolean) = repo.setEnablePredictiveBack(value)
    fun clearAll() = repo.clearAll()

    suspend fun export(output: OutputStream): Result<Int> = repo.exportJson(output)
    suspend fun importBackup(uri: Uri, merge: Boolean): ImportSummary = repo.importBackup(uri, merge)
    fun backupFileName(): String = repo.backupFileName()

}
