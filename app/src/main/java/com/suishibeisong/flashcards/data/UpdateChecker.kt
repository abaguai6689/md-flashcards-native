package com.suishibeisong.flashcards.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val apkUrl: String,
    val notes: String,
    val publishedAt: String,
)

object UpdateChecker {
    const val REPO_API = "https://api.github.com/repos/abaguai6689/suishibeisong/releases/latest"
    const val REPO_URL = "https://github.com/abaguai6689/suishibeisong"

    suspend fun fetchLatest(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(REPO_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MD-Flashcards-Android")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) error("HTTP $code")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name").ifBlank { json.optString("name") }
                if (tag.isBlank()) error("No release tag")
                var apkUrl = ""
                json.optJSONArray("assets")?.let { assets ->
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            if (apkUrl.isNotBlank()) break
                        }
                    }
                }
                UpdateInfo(
                    latestVersion = tag.removePrefix("v").removePrefix("V"),
                    releaseUrl = json.optString("html_url").ifBlank { "$REPO_URL/releases" },
                    apkUrl = apkUrl,
                    notes = json.optString("body"),
                    publishedAt = json.optString("published_at"),
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Returns a positive number when [latest] is newer than [current]. */
    fun compareVersions(latest: String, current: String): Int {
        val a = normalize(latest)
        val b = normalize(current)
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun normalize(version: String): List<Int> =
        Regex("\\d+").findAll(version.removePrefix("v").removePrefix("V"))
            .map { it.value.toIntOrNull() ?: 0 }
            .toList()
}
