package com.suishibeisong.flashcards.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.viewmodel.SettingsViewModel

@Composable
fun ReadingSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    SettingsSubPage(title = s.settingsReading, onBack = onBack) {
        SettingCard {
            SettingLabel(s.fontSize)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(settings.reading.fs == "s", { vm.setReading(fs = "s") }, { Text(s.sizeSmall) })
                FilterChip(settings.reading.fs == "m", { vm.setReading(fs = "m") }, { Text(s.sizeMedium) })
                FilterChip(settings.reading.fs == "l", { vm.setReading(fs = "l") }, { Text(s.sizeLarge) })
            }
            Spacer(Modifier.height(12.dp))
            SettingLabel(s.lineHeight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(settings.reading.lh == 1, { vm.setReading(lh = 1) }, { Text(s.tight) })
                FilterChip(settings.reading.lh == 2, { vm.setReading(lh = 2) }, { Text(s.normal) })
                FilterChip(settings.reading.lh == 3, { vm.setReading(lh = 3) }, { Text(s.loose) })
            }
            Spacer(Modifier.height(12.dp))
            SettingLabel(s.cardWidth)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(settings.reading.cw == 1, { vm.setReading(cw = 1) }, { Text(s.narrow) })
                FilterChip(settings.reading.cw == 2, { vm.setReading(cw = 2) }, { Text(s.medium) })
                FilterChip(settings.reading.cw == 3, { vm.setReading(cw = 3) }, { Text(s.wide) })
            }
        }
    }
}
