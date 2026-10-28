package com.suishibeisong.flashcards.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CloudDeck(
    val path: String,
    val name: String,
    val size: Long,
    val uploadedAt: String,
    val uploader: String,
    val description: String,
)

data class CloudDeckList(
    val decks: List<CloudDeck>,
    val skipped: Int,
)

/**
 * Public cloud deck source. Each deck lives under /decks and has a sidecar
 * "<file>.meta.json" in the same directory, e.g.
 * decks/针灸与人文/穴位定位闪卡_按经络分类.md.meta.json
 */
object CloudDecks {
    const val OWNER = "abaguai6689"
    const val REPO = "suishibeisong-cloud"
    const val BRANCH = "main"
    const val REPO_URL = "https://github.com/$OWNER/$REPO"
    private const val API_TREE = "https://api.github.com/repos/$OWNER/$REPO/git/trees/$BRANCH?recursive=1"
    private const val RAW_ROOT = "https://raw.githubusercontent.com/$OWNER/$REPO/$BRANCH/"

    suspend fun list(): Result<CloudDeckList> = withContext(Dispatchers.IO) {
        runCatching {
            val tree = getText(API_TREE)
            val arr = JSONObject(tree).optJSONArray("tree") ?: error("仓库目录为空")
            val files = linkedMapOf<String, Long>()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optString("type") != "blob") continue
                val path = item.optString("path")
                if (!path.startsWith("decks/")) continue
                if (!isDeckFile(path)) continue
                files[path] = item.optLong("size", 0L)
            }

            val decks = ArrayList<CloudDeck>()
            var skipped = 0
            files.forEach { (path, treeSize) ->
                val metaPath = "$path.meta.json"
                val metaText = runCatching { getText(RAW_ROOT + encodePath(metaPath)) }.getOrNull()
                if (metaText == null) {
                    skipped++
                    return@forEach
                }
                val meta = runCatching { JSONObject(metaText) }.getOrNull()
                if (meta == null) {
                    skipped++
                    return@forEach
                }
                val name = path.substringAfterLast('/')
                decks += CloudDeck(
                    path = path,
                    name = name,
                    size = meta.optLong("size", treeSize).takeIf { it > 0 } ?: treeSize,
                    uploadedAt = meta.optString("uploadedAt"),
                    uploader = meta.optString("uploader"),
                    description = meta.optString("description"),
                )
            }
            CloudDeckList(decks.sortedByDescending { it.uploadedAt }, skipped)
        }
    }

    suspend fun download(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching { getBytes(RAW_ROOT + encodePath(path)) }
    }

    private fun isDeckFile(path: String): Boolean {
        val name = path.substringAfterLast('/')
        if (name.startsWith(".")) return false
        return Regex("\\.(md|markdown|txt|zip)$", RegexOption.IGNORE_CASE).containsMatchIn(name)
    }

    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun getText(url: String): String = getBytes(url).toString(Charsets.UTF_8)

    private fun getBytes(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Piggy-Recite-Android")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
