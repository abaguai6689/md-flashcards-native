package com.suishibeisong.flashcards.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suishibeisong.flashcards.BuildConfig
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.data.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, FAILED }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String = "",
    val releaseUrl: String = "",
    val notes: String = "",
    val error: String = "",
    val showDialog: Boolean = false,
)

class UpdateViewModel(private val repo: FlashcardRepository) : ViewModel() {
    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()
    private var startupChecked = false

    fun checkAtStartup() {
        if (startupChecked) return
        startupChecked = true
        if (repo.currentState.settings.checkUpdateAtStartup) check(showDialog = true)
    }

    fun check(showDialog: Boolean = false) {
        if (_update.value.status == UpdateStatus.CHECKING) return
        _update.value = _update.value.copy(status = UpdateStatus.CHECKING, error = "", showDialog = false)
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
                        showDialog = newer && showDialog,
                    )
                }
                .onFailure {
                    _update.value = _update.value.copy(
                        status = UpdateStatus.FAILED,
                        error = it.message ?: "network error",
                        showDialog = false,
                    )
                }
        }
    }

    fun dismissDialog() {
        _update.value = _update.value.copy(showDialog = false)
    }
}
