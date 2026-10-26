package com.suishibeisong.flashcards.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel

@Composable
fun GeneralSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
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
    }
}
