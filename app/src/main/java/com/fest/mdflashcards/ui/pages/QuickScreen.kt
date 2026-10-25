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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fest.mdflashcards.data.Card
import com.fest.mdflashcards.data.ProgressRules
import com.fest.mdflashcards.ui.components.MarkdownText
import com.fest.mdflashcards.ui.i18n.LocalStrings
import com.fest.mdflashcards.ui.i18n.fmt
import com.fest.mdflashcards.viewmodel.QuickViewModel
import kotlinx.coroutines.delay

private sealed interface QuickRow {
    data class Header(val chapter: String, val count: Int) : QuickRow
    data class CardRow(val card: Card) : QuickRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScreen(vm: QuickViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val deck by vm.deck.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var chapter by remember { mutableStateOf("") }
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    var tocOpen by remember { mutableStateOf(false) }
    var highlight by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = remember(deck, query) { vm.filter(deck, query) }
    val chapters = remember(filtered) { filtered.map { ProgressRules.chapterOf(it) }.distinct() }
    val visible = remember(filtered, chapter) { if (chapter.isBlank()) filtered else filtered.filter { ProgressRules.chapterOf(it) == chapter } }
    val rows = remember(visible, collapsed) {
        val out = ArrayList<QuickRow>()
        var last = ""
        visible.forEach { c ->
            val ch = ProgressRules.chapterOf(c)
            if (ch != last) {
                out += QuickRow.Header(ch, visible.count { ProgressRules.chapterOf(it) == ch })
                last = ch
            }
            if (ch !in collapsed) out += QuickRow.CardRow(c)
        }
        out
    }

    LaunchedEffect(highlight, rows) {
        val id = highlight ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is QuickRow.CardRow && it.card.id == id }
        if (index >= 0) listState.animateScrollToItem(index)
        delay(1600)
        highlight = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deck?.name ?: s.quickTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(s.quickCount.fmt(filtered.size), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = s.back) } },
                actions = {
                    IconButton(onClick = { searching = !searching }) {
                        Icon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, contentDescription = s.quickSearch)
                    }
                    IconButton(onClick = {
                        collapsed = if (collapsed.isEmpty()) chapters.toSet() else emptySet()
                    }) {
                        Icon(
                            if (collapsed.isEmpty()) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                            contentDescription = if (collapsed.isEmpty()) s.collapseAll else s.expandAll,
                        )
                    }
                    IconButton(onClick = { tocOpen = true }) { Icon(Icons.Outlined.List, contentDescription = s.toc) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(s.quickSearch) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = chapter.isBlank(),
                        onClick = { chapter = "" },
                        label = { Text(s.quickAll.fmt(filtered.size)) },
                    )
                }
                items(chapters) { ch ->
                    FilterChip(
                        selected = chapter == ch,
                        onClick = { chapter = if (chapter == ch) "" else ch },
                        label = { Text(ch, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.quickEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows) { row ->
                        when (row) {
                            is QuickRow.Header -> {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            collapsed = if (row.chapter in collapsed) collapsed - row.chapter else collapsed + row.chapter
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Text(row.chapter, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(s.quickCount.fmt(row.count), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        if (row.chapter in collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            is QuickRow.CardRow -> {
                                val highlighted = row.card.id == highlight
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            row.card.title.ifBlank { row.card.question.ifBlank { "知识点" } },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (row.card.question.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            MarkdownText(row.card.question, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                                        Spacer(Modifier.height(10.dp))
                                        MarkdownText(row.card.answer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (tocOpen) {
        ModalBottomSheet(onDismissRequest = { tocOpen = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
                Text(s.toc, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.height(460.dp)) {
                    var lastChapter = ""
                    items(filtered) { c ->
                        val ch = ProgressRules.chapterOf(c)
                        if (ch != lastChapter) {
                            lastChapter = ch
                            Text(ch, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                        }
                        Text(
                            c.title.ifBlank { c.question }.replace(Regex("\\s+"), " ").take(70),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    chapter = ch
                                    collapsed = collapsed - ch
                                    highlight = c.id
                                    tocOpen = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
