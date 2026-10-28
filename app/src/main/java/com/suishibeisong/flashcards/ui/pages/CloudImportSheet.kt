package com.suishibeisong.flashcards.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suishibeisong.flashcards.data.CloudDeck
import com.suishibeisong.flashcards.data.CloudDeckList
import com.suishibeisong.flashcards.data.CloudDecks
import com.suishibeisong.flashcards.data.ImportSummary
import com.suishibeisong.flashcards.ui.i18n.LocalStrings
import com.suishibeisong.flashcards.ui.i18n.fmt
import com.suishibeisong.flashcards.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudImportSheet(
    vm: HomeViewModel,
    onDismiss: () -> Unit,
    onDone: (ImportSummary) -> Unit,
    onError: (String) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var data by remember { mutableStateOf<CloudDeckList?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingPath by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(retry) {
        error = null
        data = null
        CloudDecks.list()
            .onSuccess { data = it }
            .onFailure { error = it.message ?: "network error" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        ) {
            Text(s.cloudTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(CloudDecks.REPO_URL, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))

            val current = data
            when {
                error != null -> {
                    Text(s.cloudError.fmt(error!!), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { retry++ }) { Text(s.cloudRetry) }
                }
                current == null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.padding(start = 10.dp))
                        Text(s.cloudLoading, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                current.decks.isEmpty() -> {
                    Text(s.cloudEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(current.decks, key = { it.path }) { deck ->
                            CloudDeckCard(
                                deck = deck,
                                downloading = downloadingPath == deck.path,
                                onClick = {
                                    if (downloadingPath != null) return@CloudDeckCard
                                    downloadingPath = deck.path
                                    scope.launch {
                                        CloudDecks.download(deck.path)
                                            .onSuccess { bytes ->
                                                val summary = vm.importCloud(deck.name, bytes)
                                                downloadingPath = null
                                                onDone(summary)
                                            }
                                            .onFailure {
                                                downloadingPath = null
                                                onError(it.message ?: "download failed")
                                            }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudDeckCard(
    deck: CloudDeck,
    downloading: Boolean,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !downloading, onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    deck.name.substringBeforeLast('.').ifBlank { deck.name },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (downloading) {
                    CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("↓", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (deck.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    deck.description,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "${s.cloudSize}: ${formatSize(deck.size)}   ·   ${s.cloudUploadedAt}: ${formatTime(deck.uploadedAt)}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${s.cloudUploader}: ${deck.uploader.ifBlank { "-" }}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (downloading) {
                Spacer(Modifier.height(4.dp))
                Text(s.cloudDownloading, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

private fun formatTime(value: String): String {
    if (value.isBlank()) return "-"
    return value.replace("T", " ").substringBefore("+").substringBefore("Z")
}
