package com.fest.mdflashcards.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.data.Deck
import com.fest.mdflashcards.data.FlashcardRepository
import com.fest.mdflashcards.domain.ParseOptions
import com.fest.mdflashcards.ui.components.ChipRow
import com.fest.mdflashcards.ui.components.ConfirmDialog
import com.fest.mdflashcards.ui.components.EmptyState
import com.fest.mdflashcards.ui.components.SectionHeader
import com.fest.mdflashcards.ui.components.StatChip
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.fmt
import com.fest.mdflashcards.viewmodel.DeckItem
import com.fest.mdflashcards.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    snackbar: SnackbarHostState,
    onOpenDeck: (String) -> Unit,
    onReview: () -> Unit,
    onQuick: (String) -> Unit,
    onBrowse: (String) -> Unit,
) {
    val s = LocalStrings.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var renameDeck by remember { mutableStateOf<Deck?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteDeck by remember { mutableStateOf<Deck?>(null) }
    var resetDeck by remember { mutableStateOf<Deck?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(s.homeTitle, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { searching = !searching }) {
                        Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, contentDescription = s.searchDecks)
                    }
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = s.importButton)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showImport = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(s.importButton) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(s.homeSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    StatChip(state.totalDue.toString(), s.statDue, Modifier.weight(1f), accent = true)
                    StatChip(state.totalKnown.toString(), s.statKnown, Modifier.weight(1f))
                    StatChip(state.today.toString(), s.statToday, Modifier.weight(1f))
                }
            }

            val session = state.session
            if (session != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth().clickable { onReview() },
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.resumeTitle, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${session.deckName} · " + s.resumeLeft.fmt(session.queueIds.size + session.round2Ids.size + if (session.currentId != null) 1 else 0),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                            }
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }

            if (searching) {
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text(s.searchDecks) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(
                        s.notes,
                        state.allDecks.size.toString(),
                        Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = null)
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("recent" to s.sortRecent, "name" to s.sortName, "cards" to s.sortCards, "due" to s.sortDue)
                                .forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(if (state.sort == key) "✓  $label" else label) },
                                        onClick = { vm.setSort(key); sortMenu = false },
                                    )
                                }
                        }
                    }
                }
            }

            if (state.decks.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = if (state.query.isBlank()) s.emptyTitle else s.quickEmpty,
                        body = if (state.query.isBlank()) s.emptyBody else s.searchDecks,
                        icon = Icons.Outlined.Description,
                        action = if (state.query.isBlank()) {
                            { Button(onClick = { showImport = true }) { Text(s.importButton) } }
                        } else null,
                    )
                }
            } else {
                items(state.decks, key = { it.deck.id }) { item ->
                    DeckCard(
                        item = item,
                        onClick = { onOpenDeck(item.deck.id) },
                        onReview = {
                            if (vm.startReview(item.deck.id, 0, false, "")) onReview()
                        },
                        onQuick = { onQuick(item.deck.id) },
                        onBrowse = { onBrowse(item.deck.id) },
                        onRename = { renameDeck = item.deck; renameText = item.deck.name },
                        onReset = { resetDeck = item.deck },
                        onDelete = { deleteDeck = item.deck },
                    )
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    if (showImport) {
        ImportSheet(
            vm = vm,
            onDismiss = { showImport = false },
            onDone = { summary ->
                showImport = false
                snackbar.currentSnackbarData?.dismiss()
                scope.launch { snackbar.showSnackbar(summary.messages.firstOrNull() ?: s.imported) }
            },
        )
    }

    renameDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { renameDeck = null },
            title = { Text(s.deckRename) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text(s.renameHint) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameDeck(deck.id, renameText)
                    renameDeck = null
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { renameDeck = null }) { Text(s.cancel) } },
        )
    }
    deleteDeck?.let { deck ->
        ConfirmDialog(
            title = s.deckDelete,
            text = s.deleteDeckConfirm.fmt(deck.name),
            confirmLabel = s.delete,
            destructive = true,
            onConfirm = { vm.deleteDeck(deck.id) },
            onDismiss = { deleteDeck = null },
        )
    }
    resetDeck?.let { deck ->
        ConfirmDialog(
            title = s.deckReset,
            text = s.resetDeckConfirm.fmt(deck.name),
            confirmLabel = s.deckReset,
            destructive = true,
            onConfirm = { vm.resetDeck(deck.id) },
            onDismiss = { resetDeck = null },
        )
    }
}

@Composable
private fun DeckCard(
    item: DeckItem,
    onClick: () -> Unit,
    onReview: () -> Unit,
    onQuick: () -> Unit,
    onBrowse: () -> Unit,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalStrings.current
    var menu by remember { mutableStateOf(false) }
    val fraction = if (item.cards == 0) 0f else item.known.toFloat() / item.cards
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        listOf(
                            s.deckCards.fmt(item.cards),
                            s.deckKnown.fmt(item.known),
                            s.deckDue.fmt(item.due),
                        ).joinToString("  ·  "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = s.more)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(s.deckReview) }, onClick = { menu = false; onReview() })
                        DropdownMenuItem(text = { Text(s.deckQuick) }, onClick = { menu = false; onQuick() })
                        DropdownMenuItem(text = { Text(s.deckBrowse) }, onClick = { menu = false; onBrowse() })
                        DropdownMenuItem(text = { Text(s.deckRename) }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text(s.deckReset) }, onClick = { menu = false; onReset() })
                        DropdownMenuItem(
                            text = { Text(s.deckDelete, color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(
    vm: HomeViewModel,
    onDismiss: () -> Unit,
    onDone: (com.fest.mdflashcards.data.ImportSummary) -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uris = remember { mutableStateListOf<Uri>() }
    var paste by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("auto") }
    var qstyle by remember { mutableStateOf("prompt") }
    var gran by remember { mutableStateOf("mid") }
    var importing by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result ->
        if (result.isNotEmpty()) {
            uris.clear()
            uris.addAll(result)
            tab = 0
        }
    }

    fun parseOptions() = ParseOptions(
        mode = mode,
        qstyle = qstyle,
        maxCardLen = when (gran) {
            "fine" -> 800
            "coarse" -> 3000
            else -> 1600
        },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(s.importTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { tab = 0 }) { Text(s.importFile) }
                FilledTonalButton(onClick = { tab = 1 }) { Text(s.importPaste) }
            }
            Spacer(Modifier.height(14.dp))

            if (tab == 0) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().clickable {
                        picker.launch(arrayOf("text/*", "application/zip", "application/octet-stream"))
                    },
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(s.importFile, fontWeight = FontWeight.Medium)
                        if (uris.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(s.filesCount.fmt(uris.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = paste,
                    onValueChange = { paste = it },
                    placeholder = { Text(s.importPasteHint) },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(s.importOptions, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("模式", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            ChipRow(
                listOf(
                    "auto" to s.modeAuto,
                    "h2" to s.modeH2,
                    "h3" to s.modeH3,
                    "qa" to s.modeQa,
                    "hr" to s.modeHr,
                    "para" to s.modePara,
                ),
                mode,
                { mode = it },
            )
            Spacer(Modifier.height(10.dp))
            Text("题面", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            ChipRow(
                listOf("prompt" to s.stylePrompt, "title" to s.styleTitle, "cloze" to s.styleCloze),
                qstyle,
                { qstyle = it },
            )
            Spacer(Modifier.height(10.dp))
            Text("粒度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            ChipRow(
                listOf("fine" to s.granFine, "mid" to s.granMid, "coarse" to s.granCoarse),
                gran,
                { gran = it },
            )

            Spacer(Modifier.height(22.dp))
            Button(
                enabled = !importing && (uris.isNotEmpty() || paste.trim().length >= 10),
                onClick = {
                    importing = true
                    scope.launch {
                        var summary = com.fest.mdflashcards.data.ImportSummary()
                        val opts = parseOptions()
                        if (tab == 0) {
                            uris.forEach { uri ->
                                val r = vm.importUri(uri, opts)
                                summary = summary.copy(
                                    decks = summary.decks + r.decks,
                                    cards = summary.cards + r.cards,
                                    replaced = summary.replaced + r.replaced,
                                    skipped = summary.skipped + r.skipped,
                                    messages = summary.messages + r.messages,
                                )
                            }
                        } else {
                            val name = paste.lineSequence().firstOrNull { it.trim().startsWith("#") }
                                ?.trim()?.removePrefix("#")?.trim()?.take(40)
                                ?: "粘贴笔记"
                            summary = vm.importText(name, paste, opts)
                        }
                        importing = false
                        if (summary.decks == 0 && summary.messages.isEmpty()) {
                            onDone(summary.copy(messages = listOf(s.importEmpty)))
                        } else {
                            onDone(summary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (importing) "…" else s.importStart)
            }
        }
    }
}
