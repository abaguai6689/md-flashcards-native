package com.fest.mdflashcards.domain

/**
 * Kotlin port of the original single-file app's `src/core.js` (v1.2.0).
 * Pure functions, no Android dependencies, unit-tested in app/src/test.
 */
object CardParser {
    const val CORE_VERSION = "1.2.0"
    val MODES = listOf("auto", "h1", "h2", "h3", "qa", "hr", "para")
    val QSTYLES = listOf("prompt", "title", "cloze")

    /* ------------------------------- utils ------------------------------- */

    fun hash32(input: String): String {
        var h = -0x7ee3623b // 0x811c9dc5
        for (ch in input) {
            h = h xor ch.code
            h *= 0x01000193
        }
        return (h.toUInt()).toString(16).padStart(8, '0').takeLast(8)
    }

    fun normalize(text: String?): String =
        (text ?: "").removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n").replace("\u0000", "")

    fun stripFrontMatter(text: String): Pair<String, String> {
        val m = Regex("^---\\n([\\s\\S]*?)\\n---[ \\t]*(?:\\n|$)").find(text) ?: return text to ""
        return text.substring(m.range.last + 1) to m.groupValues[1]
    }

    fun trimBlank(value: String): String = value.replace(Regex("^\\n+"), "").trimEnd()

    fun cleanInline(value: String?): String {
        var s = value ?: ""
        s = s.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        s = s.replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace(Regex("[`*_~]"), "")
        s = s.replace(Regex("\\s+"), " ")
        return s.trim()
    }

    fun truncate(value: String?, n: Int): String {
        val s = value ?: ""
        return if (s.length > n) s.substring(0, n - 1) + "…" else s
    }

    /* --------------------------- code block mask -------------------------- */

    private val fenceRegex = Regex("^ {0,3}(`{3,}|~{3,})(.*)$")

    fun codeMask(lines: List<String>): BooleanArray {
        val mask = BooleanArray(lines.size)
        var fenceChar: Char? = null
        var fenceLen = 0
        for (i in lines.indices) {
            val m = fenceRegex.find(lines[i])
            if (fenceChar == null) {
                if (m != null) {
                    fenceChar = m.groupValues[1][0]
                    fenceLen = m.groupValues[1].length
                    mask[i] = true
                }
            } else {
                mask[i] = true
                if (m != null && m.groupValues[1][0] == fenceChar &&
                    m.groupValues[1].length >= fenceLen && m.groupValues[2].isBlank()
                ) {
                    fenceChar = null
                }
            }
        }
        return mask
    }

    /* ---------------------------- headings -------------------------------- */

    private data class Heading(val index: Int, val level: Int, val title: String)

    private data class Section(
        val title: String,
        val body: String,
        val path: List<String>,
        val start: Int,
        val end: Int,
        val headingIndex: Int,
    )

    private fun collectHeadings(lines: List<String>, mask: BooleanArray): List<Heading> {
        val result = ArrayList<Heading>()
        lines.forEachIndexed { i, line ->
            if (mask[i]) return@forEachIndexed
            val m = Regex("^ {0,3}(#{1,6})[ \\t]+(.*)$").find(line) ?: return@forEachIndexed
            val raw = m.groupValues[2].replace(Regex("[ \\t]+#+[ \\t]*$"), "").trim()
            val title = cleanInline(raw)
            if (title.isNotEmpty()) result += Heading(i, m.groupValues[1].length, title)
        }
        return result
    }

    private fun sectionsAt(
        lines: List<String>,
        headings: List<Heading>,
        level: Int,
        from: Int = 0,
        to: Int = lines.size,
    ): List<Section> {
        val out = ArrayList<Section>()
        headings.forEachIndexed { k, h ->
            if (h.level != level || h.index < from || h.index >= to) return@forEachIndexed
            var end = to
            for (j in k + 1 until headings.size) {
                if (headings[j].index >= to) break
                if (headings[j].level <= level) {
                    end = headings[j].index
                    break
                }
            }
            val chain = ArrayList<String>()
            var minLevel = level
            for (b in k - 1 downTo 0) {
                val p = headings[b]
                if (p.index < from) break
                if (p.level < minLevel) {
                    chain.add(0, p.title)
                    minLevel = p.level
                }
            }
            out += Section(
                title = h.title,
                body = trimBlank(lines.subList(h.index + 1, end).joinToString("\n")),
                path = chain,
                start = h.index + 1,
                end = end,
                headingIndex = h.index,
            )
        }
        return out
    }

    fun chooseLevel(headings: List<HeadingLike>, lines: List<String>, maxCardLen: Int): Int {
        // Public overload kept simple for tests/callers that only have title+level+index.
        val internal = headings.map { Heading(it.index, it.level, it.title) }
        if (internal.isEmpty()) return 0
        val cands = ArrayList<Triple<Int, Int, Int>>() // level, count, median
        for (level in 1..5) {
            val count = internal.count { it.level == level }
            if (count < 2) continue
            val secs = sectionsAt(lines, internal, level)
            val lens = secs.map { it.body.length }.sorted()
            cands += Triple(level, count, if (lens.isNotEmpty()) lens[lens.size / 2] else 0)
        }
        if (cands.isEmpty()) return 0
        val good = cands.filter { it.second >= 3 && it.third <= maxCardLen }
        if (good.isNotEmpty()) return good.maxByOrNull { it.first }!!.first
        return cands.sortedWith(compareByDescending<Triple<Int, Int, Int>> { it.second }.thenBy { it.first }).first().first
    }

    interface HeadingLike {
        val index: Int
        val level: Int
        val title: String
    }

    private fun chooseLevelInternal(headings: List<Heading>, lines: List<String>, maxCardLen: Int): Int {
        if (headings.isEmpty()) return 0
        val cands = ArrayList<Triple<Int, Int, Int>>()
        for (level in 1..5) {
            val count = headings.count { it.level == level }
            if (count < 2) continue
            val secs = sectionsAt(lines, headings, level)
            val lens = secs.map { it.body.length }.sorted()
            cands += Triple(level, count, if (lens.isNotEmpty()) lens[lens.size / 2] else 0)
        }
        if (cands.isEmpty()) return 0
        val good = cands.filter { it.second >= 3 && it.third <= maxCardLen }
        if (good.isNotEmpty()) return good.maxByOrNull { it.first }!!.first
        return cands.sortedWith(compareByDescending<Triple<Int, Int, Int>> { it.second }.thenBy { it.first }).first().first
    }

    /* ------------------------------- Q/A --------------------------------- */

    private val qMarker = Regex(
        "^[ \\t>*-]{0,4}(?:\\*\\*|__)?[ \\t]*(?:Question|Q|问题|问|题目|考点)[ \\t]*(?:(?::|：)|\\d+[、.])[ \\t]*(?:\\*\\*|__)?[ \\t]*(?::|：)?[ \\t]*",
        RegexOption.IGNORE_CASE,
    )
    private val aMarker = Regex(
        "^[ \\t>*-]{0,4}(?:\\*\\*|__)?[ \\t]*(?:Answer|答案|A|解答|解析|答)[ \\t]*(?:(?::|：)|\\d+[、.])[ \\t]*(?:\\*\\*|__)?[ \\t]*(?::|：)?[ \\t]*",
        RegexOption.IGNORE_CASE,
    )

    private data class QaPair(val q: String, val a: String, val at: Int)
    private data class QaResult(val pairs: List<QaPair>, val pre: String)

    private fun extractQA(lines: List<String>, mask: BooleanArray, from: Int, to: Int): QaResult {
        val pairs = ArrayList<QaPair>()
        var curQ = ""
        var curA = ""
        var curAt = 0
        var hasCurrent = false
        var target = 0 // 0 none, 1 q, 2 a
        val preBuf = ArrayList<String>()
        var seenMarker = false

        fun flush() {
            if (hasCurrent && (curQ.trim().isNotEmpty() || curA.trim().isNotEmpty())) {
                pairs += QaPair(curQ, curA, curAt)
            }
            hasCurrent = false
            target = 0
            curQ = ""
            curA = ""
            curAt = 0
        }

        for (i in from until to) {
            val line = lines[i]
            if (mask[i]) {
                if (hasCurrent && target != 0) {
                    if (target == 1) curQ += (if (curQ.isNotEmpty()) "\n" else "") + line
                    else curA += (if (curA.isNotEmpty()) "\n" else "") + line
                } else if (!seenMarker) preBuf += line
                continue
            }
            val qm = qMarker.find(line)
            if (qm != null && qm.value.isNotEmpty()) {
                seenMarker = true
                flush()
                hasCurrent = true
                curQ = line.substring(qm.value.length)
                curA = ""
                curAt = i
                target = 1
                continue
            }
            val am = aMarker.find(line)
            if (am != null && am.value.isNotEmpty()) {
                seenMarker = true
                if (!hasCurrent) {
                    hasCurrent = true
                    curAt = i
                }
                target = 2
                curA += (if (curA.isNotEmpty()) "\n" else "") + line.substring(am.value.length)
                continue
            }
            if (hasCurrent && target != 0) {
                if (target == 1) curQ += "\n" + line else curA += "\n" + line
            } else if (!seenMarker) {
                preBuf += line
            }
        }
        flush()

        val out = pairs.mapNotNull { p ->
            val q = trimBlank(p.q)
            val a = trimBlank(p.a)
            if (q.isNotEmpty() && a.isNotEmpty()) QaPair(q, a, p.at) else null
        }
        val preText = trimBlank(
            preBuf.filter { it.trim().isNotEmpty() && !Regex("^#{1,6}\\s").containsMatchIn(it) }
                .joinToString("\n"),
        )
        return QaResult(out, preText)
    }

    /* --------------------------- paragraph split -------------------------- */

    private fun chunkParagraphs(lines: List<String>, from: Int, to: Int, maxLen: Int): List<String> {
        val paragraphs = ArrayList<String>()
        val buf = ArrayList<String>()
        for (i in from until to) {
            val line = lines[i]
            if (line.isBlank()) {
                if (buf.isNotEmpty()) {
                    paragraphs += buf.joinToString("\n")
                    buf.clear()
                }
            } else {
                buf += line
            }
        }
        if (buf.isNotEmpty()) paragraphs += buf.joinToString("\n")

        val chunks = ArrayList<String>()
        var cur = ""
        for (para in paragraphs) {
            if (cur.isNotEmpty() && cur.length + para.length + 2 > maxLen) {
                chunks += cur
                cur = para
            } else {
                cur = if (cur.isEmpty()) para else "$cur\n\n$para"
            }
        }
        if (cur.isNotEmpty()) chunks += cur
        return chunks
    }

    /* --------------------------- question style --------------------------- */

    private val questionHint = Regex(
        "(\\?|？|为什么|为何|如何|怎么|怎样|什么是|是什么|有哪些|哪些|区别|对比|异同|原理|作用|优缺点|流程|步骤|实现|设计|能否|是否|说说|解释|介绍)",
    )

    fun isQuestionLike(title: String?): Boolean = questionHint.containsMatchIn(title ?: "")

    private data class Templates(
        val diff: (String) -> String,
        val flow: (String) -> String,
        val why: (String) -> String,
        val use: (String) -> String,
        val pros: (String) -> String,
        val list: (String) -> String,
        val table: (String) -> String,
        val isWhat: (String) -> String,
        val generic: (String) -> String,
        val isPrefix: Regex,
    )

    private val zhHans = Templates(
        diff = { "「$it」和相近概念有什么区别？" },
        flow = { "说一下「$it」的完整流程？" },
        why = { "「$it」的原理是什么？" },
        use = { "「$it」有什么用，为什么需要它？" },
        pros = { "「$it」的优缺点和注意事项有哪些？" },
        list = { "「$it」包含哪几个要点？" },
        table = { "从表格看，「$it」的关键差异是什么？" },
        isWhat = { "「$it」是什么？" },
        generic = { "不看答案，说说「$it」的核心内容？" },
        isPrefix = Regex("^(?:是|指|即|所谓)"),
    )
    private val zhHant = Templates(
        diff = { "「$it」和相近概念有什麼區別？" },
        flow = { "說一下「$it」的完整流程？" },
        why = { "「$it」的原理是什麼？" },
        use = { "「$it」有什麼用，為什麼需要它？" },
        pros = { "「$it」的優缺點和注意事項有哪些？" },
        list = { "「$it」包含哪幾個要點？" },
        table = { "從表格看，「$it」的關鍵差異是什麼？" },
        isWhat = { "「$it」是什麼？" },
        generic = { "不看答案，說說「$it」的核心內容？" },
        isPrefix = Regex("^(?:是|指|即|所謂)"),
    )
    private val en = Templates(
        diff = { "How does \"$it\" differ from similar concepts?" },
        flow = { "Describe the full process of \"$it\"." },
        why = { "Explain how \"$it\" works under the hood." },
        use = { "What is \"$it\" used for, and why is it needed?" },
        pros = { "What are the pros, cons and caveats of \"$it\"?" },
        list = { "List the key points of \"$it\"." },
        table = { "From the table, what are the key differences in \"$it\"?" },
        isWhat = { "What exactly is \"$it\"?" },
        generic = { "Without looking, explain the core of \"$it\"." },
        isPrefix = Regex("^(?:is |refers to|means )", RegexOption.IGNORE_CASE),
    )

    private fun templatesFor(lang: String?): Templates = when (lang) {
        "en" -> en
        "zh-Hant" -> zhHant
        else -> zhHans
    }

    fun pickTemplate(title: String, body: String, lang: String?): String {
        val m = templatesFor(lang)
        val t = cleanInline(title)
        val text = body
        val plain = cleanInline(text.replace(Regex("```[\\s\\S]*?```"), " ")).take(60)

        if (Regex("区别|对比|异同|區別|vs|VS|比较|比較|diff", RegexOption.IGNORE_CASE).containsMatchIn(t)) return m.diff(t)
        if (Regex("流程|步骤|过程|链路|生命周期|流程圖|process|lifecycle", RegexOption.IGNORE_CASE).containsMatchIn(t)) return m.flow(t)
        if (Regex("原理|机制|底层|实现|原理是|mechanism|under the hood|how .* works", RegexOption.IGNORE_CASE).containsMatchIn(t)) return m.why(t)
        if (Regex("作用|用途|场景|为什么用|意义|用途是|purpose|use case", RegexOption.IGNORE_CASE).containsMatchIn(t)) return m.use(t)
        if (Regex("优缺点|优劣|利弊|注意|坑|優缺點|pros|cons|caveat", RegexOption.IGNORE_CASE).containsMatchIn(t)) return m.pros(t)

        val listCount = Regex("^\\s*(?:[-*+]|\\d{1,2}[.)])\\s+\\S", RegexOption.MULTILINE).findAll(text).count()
        if (listCount >= 3) return m.list(t)
        if (text.contains("|") && Regex("\\|\\s*:?-{2,}").containsMatchIn(text)) return m.table(t)
        if (m.isPrefix.containsMatchIn(plain)) return m.isWhat(t)
        return m.generic(t)
    }

    fun makeCloze(body: String?): String {
        val text = body ?: ""
        if (text.isEmpty() || text.length > 1500) return ""
        if (Regex("```|~~~").containsMatchIn(text)) return ""
        var count = 0
        var out = text.replace(Regex("\\*\\*([^*\\n]{2,40})\\*\\*")) {
            count++
            "____"
        }
        out = out.replace(Regex("`([^`\\n]{2,40})`")) {
            count++
            "____"
        }
        return if (count < 2) "" else out
    }

    fun makeQuestion(title: String, body: String, style: String?, lang: String?): String {
        val s = style ?: "prompt"
        val t = cleanInline(title)
        if (t.isEmpty() || s == "title") return ""
        if (s == "cloze") {
            val cloze = makeCloze(body)
            if (cloze.isNotEmpty()) {
                val head = when (lang) {
                    "en" -> "Fill in the blanks (say the missing parts):"
                    "zh-Hant" -> "填空回顧（說出橫線處的內容）："
                    else -> "填空回顾（说出横线处的内容）："
                }
                return "$head\n\n$cloze"
            }
        }
        if (isQuestionLike(t)) return ""
        return pickTemplate(t, body, lang)
    }

    /* ----------------------------- split flow ----------------------------- */

    private data class RawCard(
        val title: String,
        val question: String,
        val answer: String,
        val path: List<String>,
        val chapter: String,
    )

    private fun frontFromChunk(text: String): String {
        val lines = text.split("\n")
        for (line in lines) {
            val t = line.replace(Regex("^\\s*(?:[-*+]|\\d+[.)])\\s+"), "")
                .replace(Regex("^\\s*#{1,6}\\s+"), "")
                .trim()
            if (t.isNotEmpty()) return truncate(cleanInline(t), 90)
        }
        return "（回忆这一段的内容）"
    }

    private fun cardsFromLevel(
        lines: List<String>,
        mask: BooleanArray,
        headings: List<Heading>,
        level: Int,
        opts: ParseOptions,
        out: MutableList<RawCard>,
    ) {
        val secs = sectionsAt(lines, headings, level)
        for (sec in secs) {
            val body = sec.body
            val qa = if (opts.useQA) extractQA(lines, mask, sec.start, sec.end) else QaResult(emptyList(), "")
            if (qa.pairs.isNotEmpty()) {
                if (qa.pre.isNotEmpty()) {
                    out += RawCard(sec.title, "", qa.pre, sec.path, sec.title)
                }
                for (p in qa.pairs) out += RawCard(sec.title, p.q, p.a, sec.path, sec.title)
                continue
            }

            if (body.length > opts.maxCardLen * 1.5) {
                var childLevel = 0
                for (h in headings) {
                    if (h.index > sec.start && h.index < sec.end && h.level > level) {
                        if (childLevel == 0 || h.level < childLevel) childLevel = h.level
                    }
                }
                if (childLevel != 0 && opts.depth < 2) {
                    val subs = sectionsAt(lines, headings, childLevel, sec.start, sec.end)
                    if (subs.size >= 2) {
                        val basePath = sec.path + sec.title
                        val secPre = ArrayList<String>()
                        for (spi in sec.start until subs[0].start) {
                            val sl = lines[spi]
                            if (sl.trim().isNotEmpty() && !Regex("^#{1,6}\\s").containsMatchIn(sl)) secPre += sl
                        }
                        val secPreText = trimBlank(secPre.joinToString("\n"))
                        if (secPreText.isNotEmpty()) {
                            out += RawCard(sec.title + " · 概览", "", secPreText, basePath, sec.title)
                        }
                        for (kid in subs) {
                            if (kid.body.trim().isEmpty()) continue
                            val kidQA = if (opts.useQA) extractQA(lines, mask, kid.start, kid.end) else QaResult(emptyList(), "")
                            if (kidQA.pairs.isNotEmpty()) {
                                if (kidQA.pre.isNotEmpty()) out += RawCard(kid.title, "", kidQA.pre, basePath, sec.title)
                                for (p in kidQA.pairs) out += RawCard(kid.title, p.q, p.a, basePath, sec.title)
                            } else {
                                out += RawCard(kid.title, "", kid.body, basePath, sec.title)
                            }
                        }
                        continue
                    }
                }
                val chunks = chunkParagraphs(lines, sec.start, sec.end, opts.maxCardLen)
                if (chunks.size > 1) {
                    chunks.forEachIndexed { c, chunk ->
                        out += RawCard(
                            "${sec.title} (${c + 1}/${chunks.size})",
                            frontFromChunk(chunk),
                            chunk,
                            sec.path,
                            sec.title,
                        )
                    }
                    continue
                }
            }
            out += RawCard(sec.title, "", body, sec.path, sec.title)
        }
    }

    private fun cardsFromHr(lines: List<String>, mask: BooleanArray, opts: ParseOptions): List<RawCard> {
        val cuts = ArrayList<Int>()
        lines.forEachIndexed { i, line ->
            if (!mask[i] && Regex("^ {0,3}(?:-{3,}|\\*{3,}|_{3,})[ \\t]*$").containsMatchIn(line)) cuts += i
        }
        val ranges = ArrayList<IntRange>()
        var prev = 0
        for (c in cuts) {
            ranges += prev until c
            prev = c + 1
        }
        ranges += prev until lines.size

        val out = ArrayList<RawCard>()
        for (r in ranges) {
            val qa = extractQA(lines, mask, r.first, r.last + 1)
            if (qa.pairs.isNotEmpty()) {
                if (qa.pre.isNotEmpty()) out += RawCard("", "", qa.pre, emptyList(), "")
                for (p in qa.pairs) out += RawCard("", p.q, p.a, emptyList(), "")
                continue
            }
            val text = trimBlank(lines.subList(r.first, r.last + 1).joinToString("\n"))
            if (text.isEmpty()) continue
            val front = frontFromChunk(text)
            val rest = if (text.contains("\n")) text.substring(text.indexOf('\n') + 1).trim() else ""
            out += RawCard("", front, if (rest.isNotEmpty()) rest else text, emptyList(), "")
        }
        return out
    }

    private fun cardsFromParagraphs(lines: List<String>, from: Int, opts: ParseOptions): List<RawCard> {
        val chunks = chunkParagraphs(lines, from, lines.size, opts.maxCardLen)
        return chunks.map { RawCard("", frontFromChunk(it), it, emptyList(), "") }
    }

    private fun finalizeCards(rawCards: List<RawCard>, opts: ParseOptions): List<ParsedCard> {
        val used = HashMap<String, Int>()
        val cards = ArrayList<ParsedCard>()
        rawCards.forEachIndexed { i, c ->
            var answer = trimBlank(c.answer)
            var question = trimBlank(c.question)
            var title = cleanInline(c.title)
            if (answer.isEmpty() && question.isEmpty()) return@forEachIndexed
            if (answer.isEmpty()) answer = title
            if (title.isEmpty()) title = if (question.isNotEmpty()) truncate(cleanInline(question), 40) else "知识点 ${i + 1}"
            if (question.isEmpty()) question = makeQuestion(title, answer, opts.qstyle, opts.lang)

            val key = "$title|$question|$answer"
            var id = "c" + hash32(key)
            val seen = used[id]
            if (seen != null) {
                val n = seen + 1
                used[id] = n
                id = "$id-$n"
            } else {
                used[id] = 1
            }
            val path = c.path.map { cleanInline(it) }.filter { it.isNotEmpty() }
            cards += ParsedCard(id, title, question, answer, path, cleanInline(c.chapter))
        }
        return cards
    }

    fun parseMarkdown(text: String?, options: ParseOptions = ParseOptions()): ParseResult {
        val opts = options.copy(
            mode = if (options.mode in MODES) options.mode else "auto",
            qstyle = if (options.qstyle in QSTYLES) options.qstyle else "prompt",
        )
        val normalized = normalize(text)
        val (bodyText, _) = stripFrontMatter(normalized)
        val lines = bodyText.split("\n")
        val mask = codeMask(lines)
        val headings = collectHeadings(lines, mask)

        val docTitle = if (headings.isNotEmpty() && headings[0].index <= 2 && headings[0].level <= 2) headings[0].title else ""

        val chainAt = Array(lines.size) { emptyList<String>() }
        run {
            var chain = ArrayList<Heading>()
            var hi = 0
            for (li in lines.indices) {
                chainAt[li] = chain.map { it.title }
                while (hi < headings.size && headings[hi].index <= li) {
                    val h = headings[hi]
                    while (chain.isNotEmpty() && chain.last().level >= h.level) chain.removeAt(chain.size - 1)
                    chain += h
                    hi++
                }
            }
        }

        val raw = ArrayList<RawCard>()
        var modeUsed = opts.mode

        when (opts.mode) {
            "qa" -> {
                val qaAll = extractQA(lines, mask, 0, lines.size)
                if (qaAll.pre.isNotEmpty()) {
                    raw += RawCard(if (docTitle.isNotEmpty()) "$docTitle · 概览" else "概览", "", qaAll.pre, emptyList(), "")
                }
                for (p in qaAll.pairs) {
                    val ch = chainAt.getOrElse(p.at) { emptyList() }
                    raw += RawCard("", p.q, p.a, ch, "")
                }
            }
            "hr" -> raw += cardsFromHr(lines, mask, opts)
            "para" -> raw += cardsFromParagraphs(lines, 0, opts)
            else -> {
                val level = when (opts.mode) {
                    "h1" -> 1
                    "h2" -> 2
                    "h3" -> 3
                    else -> chooseLevelInternal(headings, lines, opts.maxCardLen)
                }
                if (level != 0) {
                    cardsFromLevel(lines, mask, headings, level, opts, raw)
                    modeUsed = "h$level"
                }
                if (raw.isEmpty() && opts.mode == "auto") {
                    val qa2 = extractQA(lines, mask, 0, lines.size)
                    if (qa2.pairs.isNotEmpty()) {
                        if (qa2.pre.isNotEmpty()) raw += RawCard(if (docTitle.isNotEmpty()) "$docTitle · 概览" else "概览", "", qa2.pre, emptyList(), "")
                        for (p in qa2.pairs) raw += RawCard("", p.q, p.a, emptyList(), "")
                        modeUsed = "qa"
                    }
                }
                if (raw.isEmpty() && opts.mode == "auto") {
                    val hrCards = cardsFromHr(lines, mask, opts)
                    if (hrCards.size > 1) {
                        raw += hrCards
                        modeUsed = "hr"
                    }
                }
                if (raw.isEmpty() && opts.mode == "auto") {
                    var paraFrom = 0
                    var soleTitle: Heading? = null
                    if (headings.size == 1 && headings[0].index <= 2 && headings[0].level <= 2) {
                        soleTitle = headings[0]
                        paraFrom = headings[0].index + 1
                    }
                    val restBody = trimBlank(lines.subList(paraFrom, lines.size).joinToString("\n"))
                    if (soleTitle != null && restBody.length <= opts.maxCardLen * 1.5) {
                        raw += RawCard(soleTitle.title, "", restBody, emptyList(), soleTitle.title)
                        modeUsed = "h1"
                    } else if (soleTitle != null) {
                        val chunks = chunkParagraphs(lines, paraFrom, lines.size, opts.maxCardLen)
                        chunks.forEachIndexed { sc, chunk ->
                            val suffix = if (chunks.size > 1) " (${sc + 1}/${chunks.size})" else ""
                            raw += RawCard(soleTitle.title + suffix, "", chunk, emptyList(), soleTitle.title)
                        }
                        modeUsed = "para"
                    } else {
                        raw += cardsFromParagraphs(lines, paraFrom, opts)
                        modeUsed = "para"
                    }
                }
            }
        }

        return ParseResult(
            version = CORE_VERSION,
            title = docTitle,
            cards = finalizeCards(raw, opts),
            modeUsed = modeUsed,
            headingCount = headings.size,
            charCount = bodyText.length,
        )
    }

    fun buildDeck(name: String?, text: String, options: ParseOptions = ParseOptions()): ParsedDeck {
        val parsed = parseMarkdown(text, options)
        val deckName = (name ?: "").replace(Regex("\\.(md|markdown|txt)$", RegexOption.IGNORE_CASE), "").trim()
            .ifEmpty { "未命名笔记" }
        return ParsedDeck(
            id = "d" + hash32("$deckName|${text.length}|${parsed.cards.size}"),
            name = deckName,
            title = parsed.title.ifEmpty { deckName },
            mode = parsed.modeUsed,
            qstyle = options.qstyle,
            cards = parsed.cards,
        )
    }
}
