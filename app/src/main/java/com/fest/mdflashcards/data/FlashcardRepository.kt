package com.fest.mdflashcards.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.fest.mdflashcards.domain.CardParser
import com.fest.mdflashcards.domain.ParseOptions
import com.fest.mdflashcards.domain.Rating
import com.fest.mdflashcards.domain.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import kotlin.random.Random

data class ImportSummary(
    val decks: Int = 0,
    val cards: Int = 0,
    val images: Int = 0,
    val replaced: Int = 0,
    val skipped: Int = 0,
    val messages: List<String> = emptyList(),
)

data class ImageImport(
    val renames: Map<String, String> = emptyMap(),
    val count: Int = 0,
)

class FlashcardRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }
    private val stateFile = File(context.filesDir, "state.json")
    private val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private var saveJob: Job? = null

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()
    val currentState: AppState get() = _state.value

    fun clearError() { _lastError.value = null }

    private fun load(): AppState {
        if (!stateFile.exists()) return AppState()
        return runCatching {
            json.decodeFromString(AppState.serializer(), stateFile.readText(Charsets.UTF_8))
                .normalized()
        }.getOrElse {
            _lastError.value = "读取本地数据失败：${it.message}"
            AppState()
        }
    }

    private fun AppState.normalized(): AppState {
        val fixedTheme = if (settings.theme in listOf("system", "light", "dark")) settings.theme else "system"
        val fixedLang = when (settings.lang) {
            "en", "zh-Hant" -> settings.lang
            else -> "zh"
        }
        val fixedStatsView = if (settings.statsView == "bars") "bars" else "heat"
        return copy(
            settings = settings.copy(theme = fixedTheme, lang = fixedLang, statsView = fixedStatsView),
        )
    }

    /** Mutate authoritative state (single writer), then debounce a disk write. */
    @Synchronized
    fun update(transform: (AppState) -> AppState) {
        _state.value = transform(_state.value)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(250)
            persistNow()
        }
    }

    suspend fun flush() {
        saveJob?.cancel()
        saveJob = null
        persistNow()
    }

    private suspend fun persistNow() {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val text = json.encodeToString(AppState.serializer(), _state.value.copy(lastChangeAt = System.currentTimeMillis()))
                    val tmp = File(stateFile.parentFile, "state.json.tmp")
                    tmp.writeText(text, Charsets.UTF_8)
                    if (!tmp.renameTo(stateFile)) {
                        stateFile.writeText(text, Charsets.UTF_8)
                        tmp.delete()
                    }
                }.onFailure { _lastError.value = "保存失败：${it.message}" }
            }
        }
    }

    /* ------------------------------ images ------------------------------ */

    suspend fun importImages(uris: List<Uri>): ImageImport = withContext(Dispatchers.IO) {
        val renames = linkedMapOf<String, String>()
        var count = 0
        uris.forEach { uri ->
            val name = displayName(uri)?.substringAfterLast('/')?.substringAfterLast('\\') ?: return@forEach
            if (!isImageName(name)) return@forEach
            val bytes = readBytes(uri) ?: return@forEach
            val stored = storeImage(name, bytes) ?: return@forEach
            renames[name] = stored
            count++
        }
        ImageImport(renames, count)
    }

    private fun storeImage(originalName: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val base = originalName.substringAfterLast('/').substringAfterLast('\\')
        val rawExt = base.substringAfterLast('.', "").lowercase()
        val ext = rawExt.takeIf { it in setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "avif", "heic", "heif") } ?: "png"
        val stem = sanitizeName(base.substringBeforeLast('.')).ifBlank { "image" }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }.take(12)
        val filename = "$digest-$stem.$ext"
        val file = File(imagesDir, filename)
        if (!file.exists()) runCatching { file.writeBytes(bytes) }
        return filename
    }

    private fun isImageName(name: String): Boolean =
        Regex("\\.(png|jpe?g|gif|webp|bmp|svg|avif|heic|heif)$", RegexOption.IGNORE_CASE).containsMatchIn(name)

    private fun sanitizeName(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(60)

    /* ------------------------------ import ------------------------------ */

    suspend fun importMarkdown(
        name: String,
        text: String,
        options: ParseOptions = ParseOptions(),
        allowReplace: Boolean = true,
        imageRenames: Map<String, String> = emptyMap(),
    ): ImportSummary = withContext(Dispatchers.Default) {
        val rewritten = ImageRefs.rewrite(text, imageRenames)
        if (rewritten.trim().length < 10) return@withContext ImportSummary(skipped = 1, messages = listOf("$name 内容太短"))
        val parsed = CardParser.buildDeck(name, rewritten, options)
        if (parsed.cards.isEmpty()) return@withContext ImportSummary(skipped = 1, messages = listOf("$name 未解析出卡片"))

        val tags = parseDeckTags(rewritten)
        val cards = parsed.cards.map { p ->
            parsedCardToCard(p).copy(tags = parseInlineTags(p.answer + " " + p.question))
        }
        val deck = Deck(
            id = parsed.id,
            name = parsed.name,
            title = parsed.title,
            createdAt = System.currentTimeMillis(),
            mode = parsed.mode,
            qstyle = parsed.qstyle,
            raw = rewritten,
            tags = tags,
            cards = cards,
        )

        val exists = _state.value.decks.any { it.name == deck.name }
        if (exists && !allowReplace) {
            return@withContext ImportSummary(
                skipped = 1,
                messages = listOf("已取消：同名笔记 ${deck.name} 已存在"),
            )
        }

        update { s ->
            val existingIndex = s.decks.indexOfFirst { it.name == deck.name }
            val decks = s.decks.toMutableList()
            if (existingIndex >= 0) decks[existingIndex] = deck else decks.add(deck)
            s.copy(
                decks = decks,
                settings = s.settings.copy(
                    mode = options.mode,
                    gran = granForMaxLen(options.maxCardLen),
                    qstyle = options.qstyle,
                ),
            )
        }
        ImportSummary(
            decks = 1,
            cards = deck.cards.size,
            replaced = if (exists) 1 else 0,
            messages = listOf("${if (exists) "已替换" else "已导入"}「${deck.name}」· ${deck.cards.size} 张卡"),
        )
    }

    suspend fun importUri(
        uri: Uri,
        options: ParseOptions,
        allowReplace: Boolean = true,
        imageRenames: Map<String, String> = emptyMap(),
    ): ImportSummary {
        val name = displayName(uri) ?: "note.md"
        val bytes = readBytes(uri) ?: return ImportSummary(skipped = 1, messages = listOf("无法读取文件"))
        return if (name.endsWith(".zip", true) || looksLikeZip(bytes)) {
            importZip(name, bytes, options, allowReplace, imageRenames)
        } else {
            importMarkdown(name, decodeText(bytes), options, allowReplace, imageRenames)
        }
    }

    private suspend fun importZip(
        name: String,
        bytes: ByteArray,
        options: ParseOptions,
        allowReplace: Boolean,
        externalRenames: Map<String, String> = emptyMap(),
    ): ImportSummary = withContext(Dispatchers.Default) {
        val texts = ArrayList<Pair<String, String>>()
        val renames = externalRenames.toMutableMap()
        var imageCount = 0
        ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip ->
            var entry = zip.nextEntry
            var guard = 0
            while (entry != null && guard < 800) {
                guard++
                val entryName = entry.name.replace('\\', '/')
                val base = entryName.substringAfterLast('/')
                if (!entry.isDirectory && !entryName.startsWith("__MACOSX/") && !base.startsWith(".")) {
                    when {
                        isImageName(base) -> {
                            val imageBytes = zip.readBytes()
                            storeImage(base, imageBytes)?.let {
                                renames[base] = it
                                imageCount++
                            }
                        }
                        Regex("\\.(md|markdown|txt)$", RegexOption.IGNORE_CASE).containsMatchIn(entryName) -> {
                            val text = decodeText(zip.readBytes())
                            if (text.trim().length >= 10) texts += base to text
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val aggregate = ArrayList<ImportSummary>()
        texts.forEach { (fileName, text) ->
            aggregate += importMarkdown(fileName, text, options, allowReplace, renames)
        }
        ImportSummary(
            decks = aggregate.sumOf { it.decks },
            cards = aggregate.sumOf { it.cards },
            images = imageCount + aggregate.sumOf { it.images },
            replaced = aggregate.sumOf { it.replaced },
            skipped = aggregate.sumOf { it.skipped },
            messages = aggregate.flatMap { it.messages }.take(8),
        )
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /* ------------------------------ backup ------------------------------ */

    suspend fun exportJson(output: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = _state.value.copy(lastBackupAt = System.currentTimeMillis())
            val text = json.encodeToString(AppState.serializer(), payload)
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            update { it.copy(lastBackupAt = payload.lastBackupAt) }
            payload.decks.sumOf { it.cards.size }
        }
    }

    suspend fun importBackup(uri: Uri, merge: Boolean): ImportSummary = withContext(Dispatchers.IO) {
        val bytes = readBytes(uri) ?: return@withContext ImportSummary(skipped = 1, messages = listOf("无法读取备份"))
        val imported = runCatching {
            json.decodeFromString(AppState.serializer(), decodeText(bytes))
        }.getOrElse {
            _lastError.value = "备份格式不正确：${it.message}"
            return@withContext ImportSummary(skipped = 1, messages = listOf("备份格式不正确"))
        }
        if (imported.decks.isEmpty()) return@withContext ImportSummary(skipped = 1, messages = listOf("备份中没有笔记"))

        if (!merge) {
            update { imported.copy(lastChangeAt = System.currentTimeMillis()) }
            return@withContext ImportSummary(
                decks = imported.decks.size,
                cards = imported.decks.sumOf { it.cards.size },
                replaced = imported.decks.size,
                messages = listOf("已用备份替换全部数据"),
            )
        }

        var addedDecks = 0
        var addedCards = 0
        update { local ->
            val existingNames = local.decks.map { it.name }.toMutableSet()
            val newDecks = ArrayList<Deck>()
            imported.decks.forEach { d ->
                var name = d.name.ifBlank { "导入笔记" }
                if (existingNames.contains(name)) {
                    var i = 2
                    while (existingNames.contains("$name(导入$i)")) i++
                    name = "$name(导入$i)"
                }
                existingNames += name
                newDecks += d.copy(name = name)
                addedCards += d.cards.size
            }
            addedDecks = newDecks.size
            val stats = local.stats.toMutableMap()
            imported.stats.forEach { (id, st) -> if (!stats.containsKey(id)) stats[id] = st }
            val daily = local.daily.toMutableMap()
            imported.daily.forEach { (k, v) -> daily[k] = (daily[k] ?: 0) + v }
            local.copy(decks = local.decks + newDecks, stats = stats, daily = daily)
        }
        ImportSummary(
            decks = addedDecks,
            cards = addedCards,
            messages = listOf("已合并 $addedDecks 本笔记，本地进度优先"),
        )
    }

    /* ------------------------------ actions ------------------------------ */

    fun deleteDeck(deckId: String) {
        update { s ->
            val deck = s.decks.find { it.id == deckId }
            val cardIds = deck?.cards?.map { it.id }?.toSet() ?: emptySet()
            s.copy(
                decks = s.decks.filterNot { it.id == deckId },
                stats = s.stats.filterKeys { it !in cardIds },
                reviewSession = if (s.reviewSession?.deckIds?.contains(deckId) == true) null else s.reviewSession,
            )
        }
    }

    fun renameDeck(deckId: String, name: String) {
        val clean = name.trim().ifEmpty { return }
        update { s -> s.copy(decks = s.decks.map { if (it.id == deckId) it.copy(name = clean) else it }) }
    }

    fun resetDeckProgress(deckId: String) {
        update { s ->
            val ids = s.decks.find { it.id == deckId }?.cards?.map { it.id }?.toSet() ?: emptySet()
            s.copy(stats = s.stats.filterKeys { it !in ids })
        }
    }

    fun updateCard(deckId: String, card: Card) {
        update { s ->
            s.copy(decks = s.decks.map { d ->
                if (d.id != deckId) d else d.copy(cards = d.cards.map { if (it.id == card.id) card.copy(modified = true) else it })
            })
        }
    }

    fun deleteCard(deckId: String, cardId: String) {
        update { s ->
            s.copy(
                decks = s.decks.map { d -> if (d.id == deckId) d.copy(cards = d.cards.filterNot { it.id == cardId }) else d },
                stats = s.stats - cardId,
            )
        }
    }

    fun resetCardProgress(cardId: String) {
        update { s -> s.copy(stats = s.stats - cardId) }
    }

    fun setTheme(theme: String) = update { it.copy(settings = it.settings.copy(theme = theme)) }
    fun setLang(lang: String) = update { it.copy(settings = it.settings.copy(lang = lang)) }
    fun setStatsView(view: String) = update { it.copy(settings = it.settings.copy(statsView = view)) }
    fun setReading(fs: String? = null, lh: Int? = null, cw: Int? = null) = update { s ->
        s.copy(settings = s.settings.copy(reading = s.settings.reading.copy(
            fs = fs ?: s.settings.reading.fs,
            lh = lh ?: s.settings.reading.lh,
            cw = cw ?: s.settings.reading.cw,
        )))
    }
    fun setDeckSort(sort: String) = update { it.copy(settings = it.settings.copy(deckSort = sort)) }

    fun clearAll() {
        update { AppState(settings = it.settings) }
    }

    /* ---------------------------- review session ---------------------------- */

    fun startReview(deckId: String, count: Int, weakOnly: Boolean, chapter: String): Boolean {
        val s = _state.value
        val deck = s.decks.find { it.id == deckId } ?: return false
        var items = deck.cards.filter { c ->
            (!weakOnly || ProgressRules.isWeak(s.stats[c.id])) &&
                (chapter.isEmpty() || ProgressRules.chapterOf(c) == chapter)
        }
        if (items.isEmpty()) return false
        val due = items.filter { ProgressRules.isDue(s.stats[it.id]) }.shuffled()
        val notDue = items.filterNot { ProgressRules.isDue(s.stats[it.id]) }.shuffled()
        items = (due + notDue).let { if (count in 1 until it.size) it.take(count) else it }
        val session = ReviewSession(
            deckIds = listOf(deckId),
            deckName = deck.name,
            filter = "all",
            phase = 1,
            total = items.size,
            queueIds = items.drop(1).map { it.id },
            currentId = items.firstOrNull()?.id,
            savedAt = System.currentTimeMillis(),
        )
        update { it.copy(reviewSession = session) }
        return true
    }

    fun saveReviewSession(session: ReviewSession?) {
        update { it.copy(reviewSession = session?.copy(savedAt = System.currentTimeMillis())) }
    }

    /** Applies a rating to the authoritative stats/daily counters and returns the new stat. */
    fun recordRating(cardId: String, rating: Rating, now: Long = System.currentTimeMillis()): CardStat {
        var updated = CardStat()
        update { s ->
            val old = s.stats[cardId] ?: CardStat()
            val scheduled = Scheduler.schedule(old, rating, now)
            val isGot = rating == Rating.GOT
            updated = old.copy(
                status = if (isGot) "known" else "learning",
                reviews = old.reviews + 1,
                okCount = old.okCount + if (isGot) 1 else 0,
                badCount = old.badCount + if (!isGot) 1 else 0,
                lastReviewedAt = now,
                box = scheduled.box,
                dueAt = scheduled.dueAt,
                intervalDays = scheduled.intervalDays,
            )
            val key = todayKey(now)
            val daily = s.daily.toMutableMap()
            daily[key] = (daily[key] ?: 0) + 1
            s.copy(stats = s.stats + (cardId to updated), daily = daily)
        }
        return updated
    }

    fun restoreRating(cardId: String, previous: CardStat?, dayKey: String) {
        update { s ->
            val stats = s.stats.toMutableMap()
            if (previous == null) stats.remove(cardId) else stats[cardId] = previous
            val daily = s.daily.toMutableMap()
            val current = daily[dayKey] ?: 0
            if (current <= 1) daily.remove(dayKey) else daily[dayKey] = current - 1
            s.copy(stats = stats, daily = daily)
        }
    }

    fun clearReviewSession() = update { it.copy(reviewSession = null) }

    fun sessionRemaining(): Int {
        val rs = _state.value.reviewSession ?: return 0
        return rs.queueIds.size + rs.round2Ids.size + if (rs.currentId != null) 1 else 0
    }

    /* ------------------------------ helpers ------------------------------ */

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun readBytes(uri: Uri): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    private fun decodeText(bytes: ByteArray): String {
        val utf8 = runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString() }.getOrNull()
        if (utf8 != null) return utf8.replace("\uFEFF", "")
        return runCatching { String(bytes, Charset.forName("GBK")) }.getOrDefault(String(bytes, Charsets.UTF_8))
    }

    private fun parseDeckTags(text: String): List<String> {
        val fm = Regex("^---\\n([\\s\\S]*?)\\n---").find(text)?.groupValues?.get(1) ?: return emptyList()
        val line = Regex("^tags?:\\s*(.+)$", RegexOption.MULTILINE).find(fm)?.groupValues?.get(1) ?: return emptyList()
        return line.replace(Regex("[\\[\\]'\"']"), "").split(Regex("[,，]\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseInlineTags(text: String): List<String> {
        val out = ArrayList<String>()
        Regex("(^|\\s)#([\\w\\u4e00-\\u9fa5-]{1,24})").findAll(text).forEach {
            val tag = it.groupValues[2]
            if (!out.contains(tag)) out += tag
        }
        return out
    }

    private fun granForMaxLen(maxLen: Int): String = when {
        maxLen <= 800 -> "fine"
        maxLen >= 3000 -> "coarse"
        else -> "mid"
    }

    fun todayKey(now: Long = System.currentTimeMillis()): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    fun backupFileName(): String {
        val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "md-flashcards-backup-$stamp.json"
    }

    fun shuffleIds(ids: List<String>): List<String> = ids.shuffled(Random.Default)

    companion object {
        const val MAX_CARD_LEN_FINE = 800
        const val MAX_CARD_LEN_MID = 1600
        const val MAX_CARD_LEN_COARSE = 3000
    }
}
