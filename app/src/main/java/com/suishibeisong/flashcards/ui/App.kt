package com.suishibeisong.flashcards.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.ui.components.LocalTapOrigin
import com.suishibeisong.flashcards.ui.components.MarkdownText
import com.suishibeisong.flashcards.ui.components.OriginAlertDialog
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.stringsFor
import com.suishibeisong.flashcards.ui.theme.MdFlashcardsTheme
import com.suishibeisong.flashcards.ui.theme.ReadingStyle
import com.suishibeisong.flashcards.viewmodel.UpdateStatus
import com.suishibeisong.flashcards.viewmodel.UpdateViewModel
import com.xah.navigation.component.SharedNavHost
import com.xah.navigation.component.rememberNavController

@Composable
fun MdFlashcardsApp() {
    val repo = repository()
    val state by repo.state.collectAsStateWithLifecycle()
    val strings = stringsFor(state.settings.lang)
    val reading = ReadingStyle(
        fontScale = when (state.settings.reading.fs) {
            "s" -> 0.92f
            "l" -> 1.12f
            else -> 1f
        },
        lineHeightScale = when (state.settings.reading.lh) {
            1 -> 0.86f
            3 -> 1.2f
            else -> 1f
        },
        widthFactor = when (state.settings.reading.cw) {
            1 -> 0.82f
            3 -> 1f
            else -> 0.92f
        },
    )
    CompositionLocalProvider(LocalStrings provides strings) {
        MdFlashcardsTheme(themeMode = state.settings.theme, reading = reading) {
            AppContent(repo)
        }
    }
}

@Composable
private fun AppContent(repo: FlashcardRepository) {
    val nav = rememberNavController(startDestination = RootDestination)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val s = LocalStrings.current
    val appState by repo.state.collectAsStateWithLifecycle()
    val updateVm = appViewModel("update") { UpdateViewModel(repo) }
    val update by updateVm.update.collectAsStateWithLifecycle()
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val tapOrigin = remember { mutableStateOf(TransformOrigin.Center) }

    LaunchedEffect(Unit) { updateVm.checkAtStartup() }
    LaunchedEffect(appState.settings.enablePredictiveBack) {
        nav.enablePredictiveBack = appState.settings.enablePredictiveBack
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            val p = event.changes.firstOrNull()?.position ?: continue
                            if (rootSize.width > 0 && rootSize.height > 0) {
                                tapOrigin.value = TransformOrigin(
                                    pivotFractionX = (p.x / rootSize.width).coerceIn(0f, 1f),
                                    pivotFractionY = (p.y / rootSize.height).coerceIn(0f, 1f),
                                )
                            }
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(
            LocalAppDeps provides AppDeps(repo, updateVm, snackbar),
            LocalTapOrigin provides tapOrigin.value,
        ) {
            if (update.showDialog && update.status == UpdateStatus.AVAILABLE) {
                OriginAlertDialog(
                    onDismissRequest = { updateVm.dismissDialog() },
                    title = { Text("${s.updateAvailableTitle} · v${update.latestVersion}") },
                    text = {
                        Column(Modifier.height(340.dp).verticalScroll(rememberScrollState())) {
                            MarkdownText(
                                markdown = update.notes.ifBlank { s.updateNotesFallback },
                                baseFontSize = 13f,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(update.apkUrl.ifBlank { update.releaseUrl }),
                                    ),
                                )
                            }
                            updateVm.dismissDialog()
                        }) { Text(s.updateNow) }
                    },
                    dismissButton = {
                        TextButton(onClick = { updateVm.dismissDialog() }) { Text(s.later) }
                    },
                )
            }

            SharedNavHost(
                navController = nav,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
