package com.fest.mdflashcards.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.fest.mdflashcards.ui.theme.LocalReading

/** A deliberately small, dependency-free Markdown renderer for card content. */
object MarkdownParser {
    sealed interface Block {
        data class Heading(val level: Int, val text: String) : Block
        data class Paragraph(val text: String) : Block
        data class Code(val lang: String, val text: String) : Block
        data class Quote(val text: String) : Block
        data class Bullet(val indent: Int, val marker: String, val text: String) : Block
        data class Rule(val dummy: Unit = Unit) : Block
        data class Table(val rows: List<List<String>>) : Block
        data class Image(val alt: String, val path: String) : Block
    }

    private val fence = Regex("^ {0,3}(`{3,}|~{3,})\\s*([^\\s`]*)\\s*$")
    private val heading = Regex("^ {0,3}(#{1,6})\\s+(.*)$")
    private val rule = Regex("^ {0,3}(?:-{3,}|\\*{3,}|_{3,})[ \\t]*$")
    private val quote = Regex("^ {0,3}>[ \\t]?")
    private val list = Regex("^(\\s*)([-*+]|\\d{1,3}[.)])[ \\t]+(.*)$")
    private val tableSep = Regex("^\\s*\\|?[ \\t]*:?-{2,}:?[ \\t]*(\\|[ \\t]*:?-{2,}:?[ \\t]*)+\\|?[ \\t]*$")

    fun parse(markdown: String): List<Block> {
        val src = convertWikiImages(markdown.replace("\r\n", "\n").replace("\r", "\n")).trim()
        if (src.isEmpty()) return emptyList()
        val lines = src.split("\n")
        val blocks = ArrayList<Block>()
        var i = 0

        fun isBlockStart(idx: Int): Boolean {
            val l = lines.getOrNull(idx) ?: return true
            if (l.isBlank()) return true
            if (fence.matches(l) || heading.matches(l) || rule.matches(l) || quote.containsMatchIn(l) || list.matches(l)) return true
            if (l.contains("|") && lines.getOrNull(idx + 1)?.let { tableSep.matches(it) } == true) return true
            return false
        }

        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) { i++; continue }

            val fm = fence.find(line)
            if (fm != null) {
                val ch = fm.groupValues[1][0]
                val len = fm.groupValues[1].length
                val lang = fm.groupValues[2]
                val buf = ArrayList<String>()
                i++
                while (i < lines.size) {
                    val t = lines[i]
                    val close = Regex("^\\s{0,3}${Regex.escape(ch.toString())}{$len,}[ \\t]*$")
                    if (close.matches(t)) { i++; break }
                    buf += t
                    i++
                }
                blocks += Block.Code(lang, buf.joinToString("\n"))
                continue
            }

            val hm = heading.find(line)
            if (hm != null) {
                blocks += Block.Heading(hm.groupValues[1].length, hm.groupValues[2].replace(Regex("[ \\t]+#+[ \\t]*$"), ""))
                i++
                continue
            }
            if (rule.matches(line)) { blocks += Block.Rule(); i++; continue }

            if (line.contains("|") && lines.getOrNull(i + 1)?.let { tableSep.matches(it) } == true) {
                val rows = ArrayList<List<String>>()
                rows += splitRow(line)
                i += 2
                while (i < lines.size && lines[i].isNotBlank() && lines[i].contains("|")) {
                    rows += splitRow(lines[i])
                    i++
                }
                blocks += Block.Table(rows)
                continue
            }

            if (quote.containsMatchIn(line)) {
                val buf = ArrayList<String>()
                while (i < lines.size && quote.containsMatchIn(lines[i])) {
                    buf += lines[i].replace(quote, "")
                    i++
                }
                blocks += Block.Quote(buf.joinToString("\n"))
                continue
            }

            val lm = list.find(line)
            if (lm != null) {
                while (i < lines.size) {
                    val m = list.find(lines[i]) ?: break
                    val indent = m.groupValues[1].replace("\t", "    ").length
                    blocks += Block.Bullet(indent, m.groupValues[2], m.groupValues[3])
                    i++
                }
                continue
            }

            val image = Regex("^\\s*!\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)\\s*$").find(line)
            if (image != null) {
                blocks += Block.Image(image.groupValues[1], image.groupValues[2])
                i++
                continue
            }

            val pbuf = ArrayList<String>()
            while (i < lines.size && !isBlockStart(i)) {
                pbuf += lines[i]
                i++
            }
            if (pbuf.isEmpty()) {
                pbuf += lines[i]
                i++
            }
            blocks += Block.Paragraph(pbuf.joinToString("\n"))
        }
        return blocks
    }

    fun convertWikiImages(input: String): String =
        Regex("!\\[\\[([^\\]\\r\\n|]+)(?:\\|([^\\]\\r\\n]*))?\\]\\]").replace(input) { m ->
            val path = m.groupValues[1]
            val width = m.groupValues[2].trim()
            if (width.matches(Regex("\\d+"))) "![]($path \"$width\")" else "![]($path)"
        }

    fun splitRow(line: String): List<String> {
        var s = line.trim()
        if (s.startsWith("|")) s = s.substring(1)
        if (s.endsWith("|")) s = s.dropLast(1)
        val cells = ArrayList<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length && s[i + 1] == '|') {
                cur.append('|'); i += 2; continue
            }
            if (ch == '|') { cells += cur.toString().trim(); cur.clear(); i++; continue }
            cur.append(ch); i++
        }
        cells += cur.toString().trim()
        return cells
    }

    fun plainText(markdown: String): String =
        parse(markdown).joinToString(" ") { b ->
            when (b) {
                is Block.Heading -> b.text
                is Block.Paragraph -> b.text
                is Block.Quote -> b.text
                is Block.Bullet -> b.text
                is Block.Code -> b.text
                is Block.Table -> b.rows.flatten().joinToString(" ")
                is Block.Image -> b.alt
                is Block.Rule -> ""
            }
        }.replace(Regex("\\s+"), " ").trim()
}

@Composable
private fun LocalImage(path: String, alt: String) {
    val context = LocalContext.current
    val fileName = remember(path) { path.substringAfterLast('/').substringAfterLast('\\').substringBefore('?') }
    val file = remember(fileName) { File(File(context.filesDir, "images"), fileName) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { decodeSampledBitmap(file.absolutePath)?.asImageBitmap() }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = alt.ifBlank { fileName },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "图片未导入：$fileName",
                modifier = Modifier.padding(14.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun decodeSampledBitmap(path: String, maxDimension: Int = 1400): android.graphics.Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, options)
}.getOrNull()

private fun inlineString(
    text: String,
    codeBackground: Color,
    linkColor: Color,
    baseWeight: FontWeight? = null,
): AnnotatedString = buildAnnotatedString {
    fun appendFormatted(input: String, weight: FontWeight? = null, italic: Boolean = false, strike: Boolean = false) {
        var i = 0
        while (i < input.length) {
            when {
                input.startsWith("![", i) -> {
                    val close = input.indexOf("](", i + 2)
                    val end = if (close >= 0) input.indexOf(')', close + 2) else -1
                    if (close >= 0 && end > close) {
                        append("[图片]")
                        i = end + 1
                    } else { append(input[i]); i++ }
                }
                input.startsWith("**", i) || input.startsWith("__", i) -> {
                    val marker = input.substring(i, i + 2)
                    val close = input.indexOf(marker, i + 2)
                    if (close > i + 2) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        appendFormatted(input.substring(i + 2, close), FontWeight.Bold, italic, strike)
                        pop()
                        i = close + 2
                    } else { append(input[i]); i++ }
                }
                input.startsWith("~~", i) -> {
                    val close = input.indexOf("~~", i + 2)
                    if (close > i + 2) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        appendFormatted(input.substring(i + 2, close), weight, italic, true)
                        pop()
                        i = close + 2
                    } else { append(input[i]); i++ }
                }
                input[i] == '`' -> {
                    val close = input.indexOf('`', i + 1)
                    if (close > i + 1) {
                        pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))
                        append(input.substring(i + 1, close))
                        pop()
                        i = close + 1
                    } else { append(input[i]); i++ }
                }
                input.startsWith("[", i) -> {
                    val close = input.indexOf("](", i + 1)
                    val end = if (close >= 0) input.indexOf(')', close + 2) else -1
                    if (close >= 0 && end > close) {
                        pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                        appendFormatted(input.substring(i + 1, close), weight, italic, strike)
                        pop()
                        i = end + 1
                    } else { append(input[i]); i++ }
                }
                input[i] == '*' || input[i] == '_' -> {
                    val marker = input[i]
                    val close = input.indexOf(marker, i + 1)
                    if (close > i + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        appendFormatted(input.substring(i + 1, close), weight, true, strike)
                        pop()
                        i = close + 1
                    } else { append(input[i]); i++ }
                }
                else -> {
                    append(input[i])
                    i++
                }
            }
        }
    }
    appendFormatted(text, baseWeight)
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    baseFontSize: Float = MaterialTheme.typography.bodyMedium.fontSize.value,
) {
    val reading = LocalReading.current
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val linkColor = MaterialTheme.colorScheme.primary
    val fontScale = reading.fontScale

    SelectionContainer {
        Column(modifier) {
            blocks.forEachIndexed { index, block ->
                if (index > 0) Spacer(Modifier.height(7.dp))
                when (block) {
                    is MarkdownParser.Block.Heading -> {
                        val size = when (block.level) {
                            1 -> 22f
                            2 -> 19f
                            3 -> 17f
                            4 -> 15.5f
                            else -> 14.5f
                        } * fontScale
                        Text(
                            text = inlineString(block.text, codeBg, linkColor, FontWeight.SemiBold),
                            color = color,
                            fontSize = size.sp,
                            lineHeight = (size * 1.35f).sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    is MarkdownParser.Block.Paragraph -> {
                        Text(
                            text = inlineString(block.text, codeBg, linkColor),
                            color = color,
                            fontSize = (baseFontSize * fontScale).sp,
                            lineHeight = (baseFontSize * fontScale * 1.55f * reading.lineHeightScale).sp,
                        )
                    }
                    is MarkdownParser.Block.Code -> {
                        Surface(
                            color = codeBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                if (block.lang.isNotBlank()) {
                                    Text(
                                        block.lang,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                                Text(
                                    block.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (12.5f * fontScale).sp,
                                    lineHeight = (18f * fontScale).sp,
                                    color = color,
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                )
                            }
                        }
                    }
                    is MarkdownParser.Block.Quote -> {
                        val first = block.text.lineSequence().firstOrNull().orEmpty()
                        val callout = Regex("^\\[!(\\w+)]\\s*(.*)$").find(first)
                        Row(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(if (block.text.length > 80) 64.dp else 34.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                if (callout != null) {
                                    Text(
                                        callout.groupValues[2].ifBlank { callout.groupValues[1].uppercase() },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (13f * fontScale).sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = inlineString(block.text.lines().drop(1).joinToString("\n"), codeBg, linkColor),
                                        color = color.copy(alpha = 0.9f),
                                        fontSize = (baseFontSize * fontScale).sp,
                                        lineHeight = (baseFontSize * fontScale * 1.5f).sp,
                                    )
                                } else {
                                    Text(
                                        text = inlineString(block.text, codeBg, linkColor),
                                        color = color.copy(alpha = 0.88f),
                                        fontSize = (baseFontSize * fontScale).sp,
                                        lineHeight = (baseFontSize * fontScale * 1.5f * reading.lineHeightScale).sp,
                                    )
                                }
                            }
                        }
                    }
                    is MarkdownParser.Block.Bullet -> {
                        Row(Modifier.fillMaxWidth().padding(start = (block.indent / 2 * 14).dp)) {
                            Text(
                                if (block.marker.first().isDigit()) block.marker else "•",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = (baseFontSize * fontScale).sp,
                                modifier = Modifier.width(24.dp),
                            )
                            Text(
                                text = inlineString(block.text, codeBg, linkColor),
                                color = color,
                                fontSize = (baseFontSize * fontScale).sp,
                                lineHeight = (baseFontSize * fontScale * 1.5f * reading.lineHeightScale).sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    is MarkdownParser.Block.Image -> {
                        LocalImage(path = block.path, alt = block.alt)
                    }
                    is MarkdownParser.Block.Rule -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    is MarkdownParser.Block.Table -> {
                        if (block.rows.isEmpty()) return@forEachIndexed
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(vertical = 6.dp),
                        ) {
                            block.rows.forEachIndexed { r, row ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    row.forEach { cell ->
                                        Text(
                                            text = inlineString(cell, codeBg, linkColor, if (r == 0) FontWeight.SemiBold else null),
                                            color = color,
                                            fontSize = (baseFontSize * fontScale * 0.92f).sp,
                                            lineHeight = (baseFontSize * fontScale * 1.35f).sp,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                    }
                                }
                                if (r == 0) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
