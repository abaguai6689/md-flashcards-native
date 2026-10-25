package com.fest.mdflashcards.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoodBad
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.domain.Rating
import com.fest.mdflashcards.ui.components.MarkdownText
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.fmt
import com.fest.mdflashcards.ui.theme.LocalReading
import com.fest.mdflashcards.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    vm: ReviewViewModel,
    deckId: String,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val reading = LocalReading.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ui.deckName.ifBlank { s.deckReview }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (ui.card != null) {
                            Text(
                                s.reviewProgress.fmt(ui.rated, ui.total) + if (ui.phase > 1) " · ${s.round2}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.Outlined.ArrowBack, contentDescription = s.back) }
                },
                actions = {
                    if (ui.canUndo) {
                        IconButton(onClick = { vm.undo() }) {
                            Icon(Icons.Outlined.Undo, contentDescription = s.undo)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (ui.finished) {
            FinishedContent(
                ui = ui,
                modifier = Modifier.padding(padding),
                onAgain = { vm.restart(deckId, 0, false, ""); },
                onBack = onExit,
            )
            return@Scaffold
        }
        if (ui.empty || ui.card == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(s.reviewEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onExit) { Text(s.back) }
            }
            return@Scaffold
        }

        val card = ui.card ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { if (ui.total == 0) 0f else ui.rated.toFloat() / ui.total },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                if (card.path.isNotEmpty()) {
                    Text(
                        card.path.joinToString("  /  "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(card.title.ifBlank { card.question }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (card.question.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    MarkdownText(
                        markdown = card.question,
                        baseFontSize = 16f * reading.fontScale,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (ui.revealed) Modifier.pointerInput(ui.card?.id) {
                                var drag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { drag = 0f },
                                    onHorizontalDrag = { _, delta -> drag += delta },
                                    onDragEnd = {
                                        if (drag > 90) vm.rate(Rating.GOT)
                                        else if (drag < -90) vm.rate(Rating.FORGOT)
                                    },
                                )
                            } else Modifier,
                        ),
                ) {
                    Box(Modifier.padding(20.dp)) {
                        if (!ui.revealed) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 42.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(s.tapReveal, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Spacer(Modifier.height(18.dp))
                                FilledTonalButton(onClick = { vm.reveal() }) { Text(s.showAnswer) }
                            }
                        } else {
                            Column {
                                Text(s.answerLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                MarkdownText(
                                    markdown = card.answer,
                                    baseFontSize = 15.5f * reading.fontScale,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(s.swipeHint, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            if (ui.revealed) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RatingButton(
                        text = s.rateForgot,
                        icon = Icons.Outlined.MoodBad,
                        color = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    ) { vm.rate(Rating.FORGOT) }
                    RatingButton(
                        text = s.rateFuzzy,
                        icon = Icons.Outlined.SentimentSatisfied,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    ) { vm.rate(Rating.FUZZY) }
                    RatingButton(
                        text = s.rateGot,
                        icon = Icons.Outlined.SentimentVerySatisfied,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    ) { vm.rate(Rating.GOT) }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = content),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FinishedContent(
    ui: com.fest.mdflashcards.viewmodel.ReviewUiState,
    modifier: Modifier = Modifier,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    Column(
        modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(78.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 34.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(18.dp))
        Text(s.reviewDone, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            s.reviewSummary.fmt(ui.okCount, ui.fuzzyCount, ui.badCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Text(s.reviewAgain)
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text(s.reviewBack)
        }
    }
}
