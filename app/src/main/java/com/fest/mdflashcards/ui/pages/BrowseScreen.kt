package com.fest.mdflashcards.ui.pages

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.data.Card
import com.fest.mdflashcards.data.ProgressRules
import com.fest.mdflashcards.ui.components.ConfirmDialog
import com.fest.mdflashcards.ui.components.MarkdownParser
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.viewmodel.BrowseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(vm: BrowseViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val s = LocalStrings.current
    val deck by vm.deck.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Card?>(null) }
    var deleting by remember { mutableStateOf<Card?>(null) }
    val scope = rememberCoroutineScope()

    val cards = remember(deck, query) {
        val list = deck?.cards.orEmpty()
        if (query.isBlank()) list else list.filter {
            val q = query.lowercase(Locale.getDefault())
            it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.question.lowercase(Locale.getDefault()).contains(q) ||
                it.answer.lowercase(Locale.getDefault()).contains(q)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: s.browseTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = s.back) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(s.browseSearch) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.quickEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            onClick = { editing = card },
                            onDelete = { deleting = card },
                            onReset = {
                                vm.reset(card.id)
                                scope.launch { snackbar.showSnackbar(s.cardReset) }
                            },
                        )
                    }
                }
            }
        }
    }

    editing?.let { original ->
        CardEditDialog(
            card = original,
            onDismiss = { editing = null },
            onSave = { updated ->
                if (updated.answer.isBlank()) {
                    scope.launch { snackbar.showSnackbar(s.answerRequired) }
                } else {
                    vm.update(updated)
                    editing = null
                    scope.launch { snackbar.showSnackbar(s.cardSaved) }
                }
            },
        )
    }
    deleting?.let { card ->
        ConfirmDialog(
            title = s.cardDelete,
            text = card.title.ifBlank { card.question },
            confirmLabel = s.delete,
            destructive = true,
            onConfirm = { vm.delete(card.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CardRow(card: Card, onClick: () -> Unit, onDelete: () -> Unit, onReset: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    card.title.ifBlank { card.question.ifBlank { "知识点" } },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val ch = ProgressRules.chapterOf(card)
                Text(ch, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), maxLines = 1)
                Spacer(Modifier.height(4.dp))
                val preview = MarkdownParser.plainText(card.answer)
                Text(
                    preview,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(LocalStrings.current.cardEdit) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menu = false; onClick() },
                    )
                    DropdownMenuItem(
                        text = { Text(LocalStrings.current.cardReset) },
                        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                        onClick = { menu = false; onReset() },
                    )
                    DropdownMenuItem(
                        text = { Text(LocalStrings.current.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardEditDialog(card: Card, onDismiss: () -> Unit, onSave: (Card) -> Unit) {
    val s = LocalStrings.current
    var title by remember(card.id) { mutableStateOf(card.title) }
    var question by remember(card.id) { mutableStateOf(card.question) }
    var answer by remember(card.id) { mutableStateOf(card.answer) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.cardEdit) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.cardTitleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text(s.cardQuestionLabel) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text(s.cardAnswerLabel) },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(card.copy(title = title.trim(), question = question.trim(), answer = answer)) }) {
                Text(s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
