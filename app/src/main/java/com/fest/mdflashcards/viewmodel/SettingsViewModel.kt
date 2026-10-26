package com.fest.mdflashcards.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fest.mdflashcards.BuildConfig
import com.fest.mdflashcards.data.FlashcardRepository
import com.fest.mdflashcards.data.ImportSummary
import com.fest.mdflashcards.data.Settings
import com.fest.mdflashcards.data.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream

enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, FAILED }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String = "",
    val releaseUrl: String = "",
    val notes: String = "",
    val error: String = "",
)

class SettingsViewModel(private val repo: FlashcardRepository) : ViewModel() {
    val settings: StateFlow<Settings> = repo.state.map { it.settings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    fun setTheme(value: String) = repo.setTheme(value)
    fun setLang(value: String) = repo.setLang(value)
    fun setReading(fs: String? = null, lh: Int? = null, cw: Int? = null) = repo.setReading(fs, lh, cw)
    fun clearAll() = repo.clearAll()

    suspend fun export(output: OutputStream): Result<Int> = repo.exportJson(output)
    suspend fun importBackup(uri: Uri, merge: Boolean): ImportSummary = repo.importBackup(uri, merge)
    fun backupFileName(): String = repo.backupFileName()

    fun checkUpdate() {
        if (_update.value.status == UpdateStatus.CHECKING) return
        _update.value = _update.value.copy(status = UpdateStatus.CHECKING, error = "")
        viewModelScope.launch {
            UpdateChecker.fetchLatest()
                .onSuccess { info ->
                    val newer = UpdateChecker.compareVersions(info.latestVersion, BuildConfig.VERSION_NAME) > 0
                    _update.value = UpdateUiState(
                        status = if (newer) UpdateStatus.AVAILABLE else UpdateStatus.UP_TO_DATE,
                        currentVersion = BuildConfig.VERSION_NAME,
                        latestVersion = info.latestVersion,
                        releaseUrl = info.releaseUrl,
                        notes = info.notes,
                    )
                }
                .onFailure {
                    _update.value = _update.value.copy(
                        status = UpdateStatus.FAILED,
                        error = it.message ?: "network error",
                    )
                }
        }
    }


}
