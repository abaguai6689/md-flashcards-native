package com.suishibeisong.flashcards.ui.pages

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import com.suishibeisong.flashcards.data.Deck
import com.suishibeisong.flashcards.data.SampleMarkdown
import com.suishibeisong.flashcards.data.FlashcardRepository
import com.suishibeisong.flashcards.domain.ParseOptions
import com.suishibeisong.flashcards.ui.components.ChipRow
import com.suishibeisong.flashcards.ui.components.ConfirmDialog
import com.suishibeisong.flashcards.ui.components.EmptyState
import com.suishibeisong.flashcards.ui.components.SectionHeader
import com.suishibeisong.flashcards.ui.components.StatChip
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.fmt
import com.suishibeisong.flashcards.viewmodel.DeckItem
import com.suishibeisong.flashcards.viewmodel.HomeViewModel
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
    var editingDeck by remember { mutableStateOf<Deck?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                modifier = Modifier.padding(bottom = navInset + 70.dp, end = 4.dp),
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
                bottom = padding.calculateBottomPadding() + navInset + 96.dp,
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
                        onEdit = { editingDeck = item.deck },
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

    editingDeck?.let { deck ->
        DeckEditDialog(
            deck = deck,
            onDismiss = { editingDeck = null },
            onSave = { name -> vm.renameDeck(deck.id, name); editingDeck = null },
            onReset = { editingDeck = null; resetDeck = deck },
            onDelete = { editingDeck = null; deleteDeck = deck },
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
private fun DeckEditDialog(
    deck: Deck,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalStrings.current
    var name by remember(deck.id) { mutableStateOf(deck.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.editDeck) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(s.renameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onReset) { Text(s.deckReset) }
                    TextButton(onClick = onDelete) { Text(s.deckDelete, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckCard(
    item: DeckItem,
    onClick: () -> Unit,
    onReview: () -> Unit,
    onQuick: () -> Unit,
    onBrowse: () -> Unit,
    onEdit: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onEdit,
            ),
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
                        DropdownMenuItem(text = { Text(s.editDeck) }, onClick = { menu = false; onEdit() })
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
    onDone: (com.suishibeisong.flashcards.data.ImportSummary) -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uris = remember { mutableStateListOf<Uri>() }
    val imageUris = remember { mutableStateListOf<Uri>() }
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
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result ->
        if (result.isNotEmpty()) {
            imageUris.clear()
            imageUris.addAll(result)
        }
    }

    fun saveSampleNow() {
        val path = SampleMarkdown.save(context)
        Toast.makeText(
            context,
            if (path != null) s.sampleSaved.fmt(path) else s.sampleFailed,
            Toast.LENGTH_LONG,
        ).show()
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveSampleNow() else Toast.makeText(context, s.sampleFailed, Toast.LENGTH_LONG).show()
    }

    fun onSaveSample() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveSampleNow()
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
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(s.chooseImages, fontSize = 12.sp)
                }
                if (imageUris.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${s.imagesSelected.fmt(imageUris.size)} · ${s.imageHint}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onSaveSample() }) { Text(s.saveSample, fontSize = 12.sp) }
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
                        var summary = com.suishibeisong.flashcards.data.ImportSummary()
                        val opts = parseOptions()
                        val imageImport = if (imageUris.isNotEmpty()) vm.importImages(imageUris.toList())
                        else com.suishibeisong.flashcards.data.ImageImport()
                        fun merge(r: com.suishibeisong.flashcards.data.ImportSummary) {
                            summary = summary.copy(
                                decks = summary.decks + r.decks,
                                cards = summary.cards + r.cards,
                                images = summary.images + r.images,
                                replaced = summary.replaced + r.replaced,
                                skipped = summary.skipped + r.skipped,
                                messages = summary.messages + r.messages,
                            )
                        }
                        summary = summary.copy(images = imageImport.count)
                        if (tab == 0) {
                            uris.forEach { uri -> merge(vm.importUri(uri, opts, imageImport.renames)) }
                        } else {
                            val name = paste.lineSequence().firstOrNull { it.trim().startsWith("#") }
                                ?.trim()?.removePrefix("#")?.trim()?.take(40)
                                ?: "粘贴笔记"
                            merge(vm.importText(name, paste, opts, imageImport.renames))
                        }
                        importing = false
                        val withImages = if (summary.images > 0) {
                            summary.copy(messages = summary.messages + s.imagesSelected.fmt(summary.images))
                        } else summary
                        if (withImages.decks == 0 && withImages.messages.isEmpty()) {
                            onDone(withImages.copy(messages = listOf(s.importEmpty)))
                        } else {
                            onDone(withImages)
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
