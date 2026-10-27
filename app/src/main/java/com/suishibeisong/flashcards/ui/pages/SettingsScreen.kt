package com.suishibeisong.flashcards.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.suishibeisong.flashcards.ui.components.OriginAlertDialog
import com.suishibeisong.flashcards.ui.components.SectionHeader
import com.suishibeisong.flashcards.ui.components.ThinDivider
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.fmt
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel
import com.suishibeisong.flashcards.viewmodel.UpdateStatus
import com.suishibeisong.flashcards.viewmodel.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    updateVm: UpdateViewModel,
    onOpenGeneral: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenData: () -> Unit,
) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val update by updateVm.update.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var changelogOpen by remember { mutableStateOf(false) }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    ActionRow(Icons.Outlined.Tune, s.settingsGeneral, s.settingsGeneralDesc) { onOpenGeneral() }
                    ThinDivider()
                    ActionRow(Icons.Outlined.MenuBook, s.settingsReading, s.settingsReadingDesc) { onOpenReading() }
                    ThinDivider()
                    ActionRow(Icons.Outlined.Storage, s.settingsData, s.settingsDataDesc) { onOpenData() }
                }
            }

            Spacer(Modifier.height(24.dp))
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
                            if (update.status != UpdateStatus.CHECKING) updateVm.check()
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
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.checkAtStartup, fontWeight = FontWeight.Medium)
                            Text(
                                s.checkAtStartupDesc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.checkUpdateAtStartup,
                            onCheckedChange = { vm.setCheckUpdateAtStartup(it) },
                        )
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

    if (changelogOpen) {
        OriginAlertDialog(
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
