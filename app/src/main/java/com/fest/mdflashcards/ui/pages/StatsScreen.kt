package com.fest.mdflashcards.ui.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.ui.components.StatChip
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.fmt
import com.fest.mdflashcards.viewmodel.StatsViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: StatsViewModel) {
    val s = LocalStrings.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf("heat") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(s.statsTitle, fontWeight = FontWeight.SemiBold) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .padding(bottom = 96.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                StatChip(ui.today.toString(), s.statsToday, Modifier.weight(1f), accent = true)
                StatChip(ui.total.toString(), s.statsTotal, Modifier.weight(1f))
                StatChip(ui.streak.toString(), s.statsStreak, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "heat", onClick = { mode = "heat" }, label = { Text(s.statsHeat) })
                FilterChip(selected = mode == "bars", onClick = { mode = "bars" }, label = { Text(s.statsBars) })
            }
            Spacer(Modifier.height(14.dp))
            if (mode == "heat") {
                Heatmap(daily = ui.daily, selected = selectedDate, onSelect = { selectedDate = it })
            } else {
                Bars(daily = ui.daily)
            }
            Spacer(Modifier.height(12.dp))
            val selected = selectedDate
            if (selected != null) {
                val n = ui.daily[selected.toString()] ?: 0
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.toString(), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${n} ${s.reviewsUnit}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(
                    s.statsHeat + " · " + s.statsBars + " · " + s.weeks26,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Heatmap(daily: Map<String, Int>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val s = LocalStrings.current
    val today = LocalDate.now()
    val end = today
    val start = remember(today) {
        var d = today.minusDays((26 * 7 - 1).toLong())
        d = d.minusDays((d.dayOfWeek.value % 7).toLong())
        d
    }
    val weeks = remember(start, end) {
        val cols = ArrayList<List<LocalDate?>>()
        var d = start
        while (!d.isAfter(end)) {
            val col = ArrayList<LocalDate?>()
            repeat(7) {
                col += if (d.isAfter(end)) null else d
                d = d.plusDays(1)
            }
            cols += col
        }
        cols
    }
    val scroll = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { col ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        col.forEach { date ->
                            if (date == null) {
                                Box(Modifier.size(13.dp))
                            } else {
                                val n = daily[date.toString()] ?: 0
                                val level = when {
                                    n == 0 -> 0
                                    n < 3 -> 1
                                    n < 6 -> 2
                                    n < 10 -> 3
                                    else -> 4
                                }
                                val color = when (level) {
                                    0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    Modifier
                                        .size(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                        .then(
                                            if (date == selected) Modifier.background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                                RoundedCornerShape(3.dp),
                                            ) else Modifier,
                                        )
                                        .clickable { onSelect(date) },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.heatLess, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                (0..4).forEach { level ->
                    val color = when (level) {
                        0 -> MaterialTheme.colorScheme.surfaceVariant
                        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(Modifier.padding(horizontal = 2.dp).size(10.dp).background(color, RoundedCornerShape(2.dp)))
                }
                Spacer(Modifier.width(4.dp))
                Text(s.heatMore, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Bars(daily: Map<String, Int>) {
    val s = LocalStrings.current
    val days = remember { (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
    val max = (days.maxOfOrNull { daily[it.toString()] ?: 0 } ?: 0).coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { date ->
                val n = daily[date.toString()] ?: 0
                val fraction by animateFloatAsState(
                    if (n == 0) 0.02f else (n.toFloat() / max).coerceIn(0.08f, 1f),
                    label = "bar",
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (n > 0) n.toString() else "",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (date == LocalDate.now()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                ),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
