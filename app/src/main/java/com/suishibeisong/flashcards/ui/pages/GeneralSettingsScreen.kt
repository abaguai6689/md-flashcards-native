package com.suishibeisong.flashcards.ui.pages

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel

@Composable
fun GeneralSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var systemBackAnimation by remember { mutableStateOf(systemBackAnimationEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemBackAnimation = systemBackAnimationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SettingsSubPage(title = s.settingsGeneral, onBack = onBack) {
        SettingCard {
            SettingLabel(s.settingsTheme)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(settings.theme == "system", { vm.setTheme("system") }, { Text(s.themeSystem) })
                FilterChip(settings.theme == "light", { vm.setTheme("light") }, { Text(s.themeLight) })
                FilterChip(settings.theme == "dark", { vm.setTheme("dark") }, { Text(s.themeDark) })
            }
            Spacer(Modifier.height(14.dp))
            SettingLabel(s.settingsLanguage)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(settings.lang == "zh", { vm.setLang("zh") }, { Text(s.langZh) })
                FilterChip(settings.lang == "zh-Hant", { vm.setLang("zh-Hant") }, { Text(s.langZhHant) })
                FilterChip(settings.lang == "en", { vm.setLang("en") }, { Text(s.langEn) })
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.predictiveBack, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    Text(
                        s.predictiveBackDesc,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.enablePredictiveBack,
                    onCheckedChange = { vm.setPredictiveBack(it) },
                )
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && settings.enablePredictiveBack && !systemBackAnimation) {
            Spacer(Modifier.height(12.dp))
            SettingCard {
                Text(
                    s.predictiveBackSystemOff,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    s.predictiveBackSystemOffDesc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(AndroidSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                runCatching { context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS)) }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(s.openDeveloperOptions) }
            }
        }
    }
}

private fun systemBackAnimationEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return runCatching {
        AndroidSettings.Global.getInt(context.contentResolver, "enable_back_animation", 1) != 0
    }.getOrDefault(true)
}
