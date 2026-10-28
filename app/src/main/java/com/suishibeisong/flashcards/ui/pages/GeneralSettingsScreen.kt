package com.suishibeisong.flashcards.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    }
}
