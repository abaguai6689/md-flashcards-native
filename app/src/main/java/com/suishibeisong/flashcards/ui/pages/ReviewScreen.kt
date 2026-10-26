package com.suishibeisong.flashcards.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suishibeisong.flashcards.data.Card
import com.suishibeisong.flashcards.domain.Rating
import com.suishibeisong.flashcards.ui.components.MarkdownText
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.fmt
import com.suishibeisong.flashcards.ui.theme.LocalReading
import com.suishibeisong.flashcards.viewmodel.ReviewUiState
import com.suishibeisong.flashcards.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    vm: ReviewViewModel,
    deckId: String,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    val ui by vm.ui.collectAsStateWithLifecycle()

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
            if (card.path.isNotEmpty()) {
                Text(
                    card.path.joinToString("  /  "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 22.dp).padding(top = 12.dp),
                )
            }
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val cardWidth = minOf(maxWidth, 380.dp, maxHeight * 0.8f)
                FlipSwipeCard(
                    card = card,
                    revealed = ui.revealed,
                    onReveal = vm::reveal,
                    onHide = vm::hideAnswer,
                    onRate = vm::rate,
                    modifier = Modifier.width(cardWidth).height(cardWidth / 0.8f),
                )
            }
            AnimatedVisibility(
                visible = ui.revealed,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
private fun FlipSwipeCard(
    card: Card,
    revealed: Boolean,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val reading = LocalReading.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(500, easing = CubicBezierEasing(0.34f, 1.25f, 0.64f, 1f)),
        label = "flip",
    )
    val offset = remember(card.id) { Animatable(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    val swipeColor = when {
        offset.value > 24f -> MaterialTheme.colorScheme.primary
        offset.value < -24f -> MaterialTheme.colorScheme.error
        else -> Color.Transparent
    }

    LaunchedEffect(card.id) { offset.snapTo(0f) }

    Box(
        modifier
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer {
                translationX = offset.value
                rotationZ = if (width > 0f) (offset.value / width) * 9f else 0f
                alpha = 1f - (abs(offset.value) / (width.coerceAtLeast(1f) * 2.2f)).coerceIn(0f, 0.55f)
            }
            .border(2.dp, swipeColor.copy(alpha = 0.75f), RoundedCornerShape(28.dp))
            .pointerInput(revealed, card.id) {
                if (!revealed) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                    onDragEnd = {
                        val threshold = (width * 0.28f).coerceAtLeast(120f)
                        when {
                            offset.value > threshold -> scope.launch {
                                offset.animateTo(width * 1.5f, tween(220, easing = FastOutSlowInEasing))
                                onRate(Rating.GOT)
                            }
                            offset.value < -threshold -> scope.launch {
                                offset.animateTo(-width * 1.5f, tween(220, easing = FastOutSlowInEasing))
                                onRate(Rating.FORGOT)
                            }
                            else -> scope.launch {
                                offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                    },
                )
            }
            .clickable(
                onClick = { if (revealed) onHide() else onReveal() },
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density.density
                },
        ) {
            val frontAlpha = (1f - rotation / 90f).coerceIn(0f, 1f)
            val backAlpha = ((rotation - 90f) / 90f).coerceIn(0f, 1f)

            if (rotation <= 90f) {
                CardFace(
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = frontAlpha },
                    background = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ),
                    footer = s.flipHint,
                ) {
                    val tag = card.chapter.ifBlank { card.path.lastOrNull().orEmpty() }
                    if (tag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(
                        card.title.ifBlank { "知识点" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (card.question.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        MarkdownText(
                            markdown = card.question,
                            baseFontSize = 16f * reading.fontScale,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                CardFace(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        rotationY = 180f
                        alpha = backAlpha
                    },
                    background = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                    footer = s.swipeHint2,
                ) {
                    Text(
                        s.answerLabel,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    MarkdownText(
                        markdown = card.answer,
                        baseFontSize = 15.5f * reading.fontScale,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFace(
    modifier: Modifier,
    background: Brush,
    footer: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(background)
                .padding(22.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                content = content,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                footer,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun RatingButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: Color,
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
    ui: ReviewUiState,
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
