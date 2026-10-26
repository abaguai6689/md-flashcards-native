package com.fest.mdflashcards.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import com.fest.mdflashcards.ui.i18n.LocalStrings

enum class MainTab { HOME, STATS, SETTINGS }

@Composable
fun FloatingNavBar(current: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val tabs = listOf(
        Triple(MainTab.HOME, s.tabHome, Icons.Outlined.Home),
        Triple(MainTab.STATS, s.tabStats, Icons.Outlined.BarChart),
        Triple(MainTab.SETTINGS, s.tabSettings, Icons.Outlined.Settings),
    )
    val shape = RoundedCornerShape(34.dp)
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val topGlass = Color.White.copy(alpha = if (light) 0.66f else 0.14f)
    val bottomGlass = Color.White.copy(alpha = if (light) 0.26f else 0.045f)
    val borderStart = Color.White.copy(alpha = if (light) 0.95f else 0.48f)
    val borderEnd = Color.White.copy(alpha = if (light) 0.24f else 0.06f)
    val highlight by animateFloatAsState(
        targetValue = current.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        label = "liquidHighlight",
    )

    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .shadow(
                    elevation = 26.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topGlass, bottomGlass),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderStart, borderEnd, borderStart.copy(alpha = 0.35f)),
                    ),
                    shape = shape,
                )
                .drawBehind {
                    val cell = size.width / 3f
                    val centerX = (highlight + 0.5f) * cell
                    val radius = cell * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (light) 0.55f else 0.20f),
                                Color.White.copy(alpha = 0.0f),
                            ),
                            center = Offset(centerX, size.height * 0.12f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(centerX, size.height * 0.12f),
                    )
                    drawOval(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.0f),
                                Color.White.copy(alpha = if (light) 0.45f else 0.16f),
                                Color.White.copy(alpha = 0.0f),
                            ),
                        ),
                        topLeft = Offset(centerX - cell * 0.55f, -size.height * 0.35f),
                        size = androidx.compose.ui.geometry.Size(cell * 1.1f, size.height * 0.9f),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = if (light) 0.65f else 0.18f),
                        start = Offset(18.dp.toPx(), 1.dp.toPx()),
                        end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (tab, label, icon) ->
                    val selected = tab == current
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (pressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                        label = "pressScale",
                    )
                    val bg by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (light) 0.16f else 0.28f)
                        } else Color.Transparent,
                        label = "navBg",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(24.dp))
                            .background(bg)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab)
                            }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            label,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val bg by animateColorAsState(
        if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chipBg",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable { onSelect(key) },
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
}

@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}
