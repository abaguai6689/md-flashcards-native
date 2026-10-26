package com.suishibeisong.flashcards.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.BuildConfig
import com.suishibeisong.flashcards.data.Changelog
import com.suishibeisong.flashcards.data.UpdateChecker
import com.suishibeisong.flashcards.ui.components.ActionRow
import com.suishibeisong.flashcards.ui.components.ConfirmDialog
import com.suishibeisong.flashcards.ui.components.SectionHeader
import com.suishibeisong.flashcards.ui.components.ThinDivider
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.fmt
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel
import com.suishibeisong.flashcards.viewmodel.UpdateStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, snackbar: SnackbarHostState) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clearOpen by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var changelogOpen by remember { mutableStateOf(false) }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(s.settingsTitle, fontWeight = FontWeight.SemiBold) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = navInset + 96.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionHeader(s.settingsGeneral)
            Spacer(Modifier.height(8.dp))
            SettingCard {
                Label(s.settingsTheme)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.theme == "system", { vm.setTheme("system") }, { Text(s.themeSystem) })
                    FilterChip(settings.theme == "light", { vm.setTheme("light") }, { Text(s.themeLight) })
                    FilterChip(settings.theme == "dark", { vm.setTheme("dark") }, { Text(s.themeDark) })
                }
                Spacer(Modifier.height(14.dp))
                Label(s.settingsLanguage)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.lang == "zh", { vm.setLang("zh") }, { Text(s.langZh) })
                    FilterChip(settings.lang == "zh-Hant", { vm.setLang("zh-Hant") }, { Text(s.langZhHant) })
                    FilterChip(settings.lang == "en", { vm.setLang("en") }, { Text(s.langEn) })
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(s.settingsReading)
            Spacer(Modifier.height(8.dp))
            SettingCard {
                Label(s.fontSize)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.reading.fs == "s", { vm.setReading(fs = "s") }, { Text(s.sizeSmall) })
                    FilterChip(settings.reading.fs == "m", { vm.setReading(fs = "m") }, { Text(s.sizeMedium) })
                    FilterChip(settings.reading.fs == "l", { vm.setReading(fs = "l") }, { Text(s.sizeLarge) })
                }
                Spacer(Modifier.height(12.dp))
                Label(s.lineHeight)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.reading.lh == 1, { vm.setReading(lh = 1) }, { Text(s.tight) })
                    FilterChip(settings.reading.lh == 2, { vm.setReading(lh = 2) }, { Text(s.normal) })
                    FilterChip(settings.reading.lh == 3, { vm.setReading(lh = 3) }, { Text(s.loose) })
                }
                Spacer(Modifier.height(12.dp))
                Label(s.cardWidth)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.reading.cw == 1, { vm.setReading(cw = 1) }, { Text(s.narrow) })
                    FilterChip(settings.reading.cw == 2, { vm.setReading(cw = 2) }, { Text(s.medium) })
                    FilterChip(settings.reading.cw == 3, { vm.setReading(cw = 3) }, { Text(s.wide) })
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(s.settingsData)
            Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(20.dp))
            SectionHeader(s.about)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.appName, fontWeight = FontWeight.SemiBold)
                            Text("${s.currentVersion} ${BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        UpdateChecker.REPO_URL,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.REPO_URL))) }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    ThinDivider()
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (update.status != UpdateStatus.CHECKING) vm.checkUpdate()
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.checkUpdate, fontWeight = FontWeight.Medium)
                            val status = when (update.status) {
                                UpdateStatus.IDLE -> ""
                                UpdateStatus.CHECKING -> s.checking
                                UpdateStatus.UP_TO_DATE -> "${s.upToDate} · v${update.latestVersion}"
                                UpdateStatus.AVAILABLE -> s.newVersion.fmt(update.latestVersion)
                                UpdateStatus.FAILED -> s.updateFailed.fmt(update.error)
                            }
                            if (status.isNotBlank()) {
                                Text(
                                    status,
                                    fontSize = 11.sp,
                                    color = if (update.status == UpdateStatus.FAILED) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (update.status == UpdateStatus.AVAILABLE) {
                            TextButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                                }
                            }) { Text(s.goDownload) }
                        } else {
                            Text(
                                if (update.status == UpdateStatus.CHECKING) "…" else "›",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                            )
                        }
                    }
                    ThinDivider()
                    Row(
                        Modifier.fillMaxWidth().clickable { changelogOpen = true }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.changelog, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
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

    if (changelogOpen) {
        AlertDialog(
            onDismissRequest = { changelogOpen = false },
            title = { Text(s.changelog) },
            text = {
                Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                    Changelog.entries.forEach { entry ->
                        Text(
                            "v${entry.version}  ·  ${entry.date}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        entry.changes.forEach { change ->
                            Text("• $change", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { changelogOpen = false }) { Text(s.close) } },
        )
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
}
