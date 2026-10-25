package com.fest.mdflashcards.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.ui.components.ConfirmDialog
import com.fest.mdflashcards.ui.components.SectionHeader
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.fmt
import com.fest.mdflashcards.viewmodel.DeckViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    vm: DeckViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onStartReview: () -> Unit,
    onQuick: () -> Unit,
    onBrowse: () -> Unit,
) {
    val s = LocalStrings.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var deleteOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    var weak by remember { mutableStateOf(false) }
    var chapter by remember { mutableStateOf("") }
    var chapterMenu by remember { mutableStateOf(false) }

    val deck = ui.deck
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: s.deckReview, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = s.back) }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = s.more) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(s.deckRename) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = { menu = false; renameText = deck?.name.orEmpty(); renameOpen = true },
                            )
                            DropdownMenuItem(
                                text = { Text(s.deckReset) },
                                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                                onClick = { menu = false; resetOpen = true },
                            )
                            DropdownMenuItem(
                                text = { Text(s.deckDelete, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; deleteOpen = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (deck == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(s.emptyTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(deck.title.ifBlank { deck.name }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Metric(deck.cards.size.toString(), s.count)
                        Metric(ui.known.toString(), s.statKnown)
                        Metric(ui.due.toString(), s.statDue)
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { if (deck.cards.isEmpty()) 0f else ui.known.toFloat() / deck.cards.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(Icons.Outlined.Shuffle, s.deckReview, Modifier.weight(1f), onStartReview)
                QuickAction(Icons.Outlined.MenuBook, s.deckQuick, Modifier.weight(1f), onQuick)
                QuickAction(Icons.Outlined.ImportContacts, s.deckBrowse, Modifier.weight(1f), onBrowse)
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(s.reviewSetup)
            Spacer(Modifier.height(10.dp))
            Text(s.count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = count == 20, onClick = { count = if (count == 20) 0 else 20 }, label = { Text(s.count20) })
                FilterChip(selected = count == 50, onClick = { count = if (count == 50) 0 else 50 }, label = { Text(s.count50) })
                FilterChip(selected = count == 0, onClick = { count = 0 }, label = { Text(s.countAll) })
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.weakOnly, fontWeight = FontWeight.Medium)
                        Text(s.weakDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = weak, onCheckedChange = { weak = it })
                }
            }

            Spacer(Modifier.height(10.dp))
            Box {
                OutlinedButton(onClick = { chapterMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("${s.chapter}: ${chapter.ifBlank { s.all }}", modifier = Modifier.weight(1f))
                    Text("▾")
                }
                DropdownMenu(expanded = chapterMenu, onDismissRequest = { chapterMenu = false }) {
                    DropdownMenuItem(text = { Text(s.all) }, onClick = { chapter = ""; chapterMenu = false })
                    ui.chapters.forEach { ch ->
                        DropdownMenuItem(text = { Text(ch, maxLines = 1) }, onClick = { chapter = ch; chapterMenu = false })
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (vm.startReview(count, weak, chapter)) onStartReview()
                    else scope.launch { snackbar.showSnackbar(s.reviewEmpty) }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(s.startReview, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (renameOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(s.deckRename) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text(s.renameHint) },
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { vm.rename(renameText); renameOpen = false }) { Text(s.save) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { renameOpen = false }) { Text(s.cancel) }
            },
        )
    }
    if (deleteOpen) {
        ConfirmDialog(
            title = s.deckDelete,
            text = s.deleteDeckConfirm.fmt(deck?.name.orEmpty()),
            confirmLabel = s.delete,
            destructive = true,
            onConfirm = { vm.delete(); onBack() },
            onDismiss = { deleteOpen = false },
        )
    }
    if (resetOpen) {
        ConfirmDialog(
            title = s.deckReset,
            text = s.resetDeckConfirm.fmt(deck?.name.orEmpty()),
            confirmLabel = s.deckReset,
            destructive = true,
            onConfirm = { vm.reset() },
            onDismiss = { resetOpen = false },
        )
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(7.dp))
            Text(label, fontSize = 11.sp, maxLines = 2, fontWeight = FontWeight.Medium)
        }
    }
}
