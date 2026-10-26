package com.suishibeisong.flashcards.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suishibeisong.flashcards.ui.components.ActionRow
import com.suishibeisong.flashcards.ui.components.ConfirmDialog
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun DataSettingsScreen(
    vm: SettingsViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var clearOpen by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = context.contentResolver.openOutputStream(uri)?.let { vm.export(it) }
                snackbar.showSnackbar(if (result?.isSuccess == true) s.exportDone else s.error)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    SettingsSubPage(title = s.settingsData, onBack = onBack, snackbar = snackbar) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                ActionRow(Icons.Outlined.Download, s.exportBackup, s.backupInfo) {
                    exportLauncher.launch(vm.backupFileName())
                }
                ActionRow(Icons.Outlined.Upload, s.importBackup, s.merge + " / " + s.replace) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
                ActionRow(Icons.Outlined.DeleteForever, s.clearAll) { clearOpen = true }
            }
        }
    }

    if (clearOpen) {
        ConfirmDialog(
            title = s.clearAll,
            text = s.clearConfirm,
            confirmLabel = s.delete,
            destructive = true,
            onConfirm = { vm.clearAll() },
            onDismiss = { clearOpen = false },
        )
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(s.importBackup) },
            text = { Text(s.mergeHint) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val r = vm.importBackup(uri, merge = true)
                        snackbar.showSnackbar(r.messages.firstOrNull() ?: s.importDone)
                    }
                }) { Text(s.merge) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        pendingImport = null
                        scope.launch {
                            val r = vm.importBackup(uri, merge = false)
                            snackbar.showSnackbar(r.messages.firstOrNull() ?: s.importDone)
                        }
                    }) { Text(s.replace, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { pendingImport = null }) { Text(s.cancel) }
                }
            },
        )
    }
}
