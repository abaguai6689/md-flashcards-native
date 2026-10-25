package com.fest.mdflashcards

import com.fest.mdflashcards.data.CardStat
import com.fest.mdflashcards.domain.CardParser
import com.fest.mdflashcards.domain.ParseOptions
import com.fest.mdflashcards.domain.Rating
import com.fest.mdflashcards.domain.Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardParserTest {
    private val headingDoc = """
        # 计算机网络

        前言部分，不该单独成卡。

        ## TCP 三次握手

        客户端发送 SYN，服务端回复 SYN+ACK，客户端再回 ACK。

        ### 为什么不是两次

        防止历史连接请求造成资源浪费。

        ## HTTP 与 HTTPS 区别

        HTTPS 在 HTTP 下加了 TLS 层。

        ```js
        // 下面这行是假标题，不能拆
        ## 这不是标题
        const a = 1;
        ```

        ## 进程与线程

        进程是资源分配单位，线程是调度单位。
    """.trimIndent()

    @Test
    fun headingDocumentSplitsAtH2() {
        val res = CardParser.parseMarkdown(headingDoc)
        assertEquals(3, res.cards.size)
        val titles = res.cards.joinToString("|") { it.title }
        assertTrue(titles.contains("TCP 三次握手"))
        assertTrue(titles.contains("HTTP 与 HTTPS 区别"))
        val tcp = res.cards.first { it.title.contains("TCP") }
        assertTrue(tcp.answer.contains("历史连接请求"))
    }

    @Test
    fun codeFenceHeadingIsNotASection() {
        val res = CardParser.parseMarkdown(headingDoc)
        assertFalse(res.cards.any { it.title.contains("这不是标题") })
        val http = res.cards.first { it.title.contains("HTTP") }
        assertTrue(http.answer.contains("## 这不是标题"))
    }

    @Test
    fun qaPairsAndHeadingCards() {
        val md = """
            # 操作系统面试

            ## 1. 进程和线程的区别

            Q: 进程和线程最核心的区别是什么？
            A: 进程是资源分配的基本单位，线程是 CPU 调度的基本单位。

            Q: 协程和线程的区别？
            A: 协程是用户态调度，切换开销远小于线程。

            ## 2. 死锁的四个必要条件

            - 互斥
            - 占有并等待
            - 不可抢占
            - 循环等待
        """.trimIndent()
        val res = CardParser.parseMarkdown(md)
        val qa = res.cards.filter { it.question.isNotEmpty() }
        assertTrue(qa.size >= 2)
        assertTrue(qa[0].question.contains("最核心的区别"))
        assertTrue(res.cards.any { it.title.contains("死锁") && it.answer.contains("循环等待") })
    }

    @Test
    fun fullWidthColonAndBoldMarkers() {
        val md = """
            ## 题目一
            **问：** 什么是虚拟内存？
            **答：** 把物理内存抽象成连续的地址空间。
        """.trimIndent()
        val cards = CardParser.parseMarkdown(md).cards
        assertEquals(1, cards.size)
        assertTrue(cards[0].question.contains("虚拟内存"))
        assertTrue(cards[0].answer.contains("地址空间"))
    }

    @Test
    fun qpsApiAndChoicesAreNotQa() {
        val md = """
            ## 性能指标

            QPS: 每秒查询数。
            API: 应用程序接口。
            A/B 测试显示提升。
            A. 进程
        """.trimIndent()
        val cards = CardParser.parseMarkdown(md).cards
        assertTrue(cards.any { it.answer.contains("QPS") })
        assertFalse(cards.any { it.question.startsWith("PS:") })
        assertFalse(cards.any { it.answer.startsWith("PI:") })
    }

    @Test
    fun noHeadingFallsBackToParagraphs() {
        val md = buildString {
            repeat(12) {
                append("第 $it 个知识点：")
                append("这是一段很长的解释文字，用来测试段落切块逻辑。".repeat(6))
                append("\n\n")
            }
        }
        val res = CardParser.parseMarkdown(md)
        assertTrue(res.cards.size >= 2)
        assertTrue(res.cards.all { it.answer.isNotEmpty() })
    }

    @Test
    fun emptyTextProducesNoCards() {
        assertTrue(CardParser.parseMarkdown("").cards.isEmpty())
        assertTrue(CardParser.parseMarkdown("   \n\n  ").cards.isEmpty())
    }

    @Test
    fun idsAreStableAndUnique() {
        val a = CardParser.parseMarkdown(headingDoc).cards.map { it.id }
        val b = CardParser.parseMarkdown(headingDoc).cards.map { it.id }
        assertEquals(a, b)
        val dup = CardParser.parseMarkdown("## 重复\n\n同样内容\n\n## 重复\n\n同样内容").cards
        assertEquals(dup.size, dup.map { it.id }.toSet().size)
    }

    @Test
    fun longSectionSplitsByChildHeadingsAndKeepsPath() {
        val md = buildString {
            append("# 主题\n\n## 章节A\n\n")
            repeat(3) { append("### 问题${it + 1}\n\n答案${it + 1} 的详细内容。\n\n") }
            append("## 章节B\n\n")
            repeat(3) { append("### 问题${it + 4}\n\n答案${it + 4}。\n\n") }
        }
        val res = CardParser.parseMarkdown(md)
        val sub = res.cards.first { it.title == "问题1" }
        assertEquals(listOf("主题", "章节A"), sub.path)
    }

    @Test
    fun questionStyles() {
        assertEquals("", CardParser.parseMarkdown("## TCP 握手\n\n三次握手的内容。", ParseOptions(qstyle = "title")).cards[0].question)
        val prompt = CardParser.parseMarkdown("## 三次握手\n\n三次握手的内容。", ParseOptions(qstyle = "prompt")).cards[0].question
        assertTrue(prompt.contains("三次握手") && prompt.endsWith("？"))

        val list = CardParser.parseMarkdown("## 死锁条件\n\n- 互斥\n- 占有并等待\n- 不可抢占\n").cards[0].question
        assertTrue(list.contains("要点"))

        val clozeCards = CardParser.parseMarkdown(
            "## 三次握手\n\n客户端发送 **SYN**，服务端回复 **SYN+ACK**，最后 **ACK**。",
            ParseOptions(qstyle = "cloze"),
        ).cards
        assertTrue(clozeCards[0].question.contains("____"))
        assertTrue(clozeCards[0].answer.contains("SYN+ACK"))
    }

    @Test
    fun questionLikeTitleIsNotRewritten() {
        val md = "## TCP 为什么需要三次握手？\n\n内容。"
        assertEquals("", CardParser.parseMarkdown(md, ParseOptions(qstyle = "prompt")).cards[0].question)
    }

    @Test
    fun buildDeckNameAndTitle() {
        val deck = CardParser.buildDeck("算法笔记.md", headingDoc)
        assertEquals("算法笔记", deck.name)
        assertEquals("计算机网络", deck.title)
        assertTrue(deck.cards.size >= 3)
    }

    @Test
    fun qaModeKeepsHeadingChain() {
        val md = "# 笔记\n\n## 第一章\n\nQ: a?\nA: b.\n\n## 第二章\n\nQ: c?\nA: d."
        val deck = CardParser.buildDeck("t", md, ParseOptions(mode = "qa"))
        val paths = deck.cards.map { it.path.joinToString("/") }
        assertTrue(paths.contains("笔记/第一章"))
        assertTrue(paths.contains("笔记/第二章"))
    }

    @Test
    fun qaPrefaceIsKeptAsOverviewCard() {
        val deck = CardParser.buildDeck(
            "t",
            "## 事务隔离级别\n\n事务隔离级别共有四种，下面是重点：\n\nQ: 默认级别？\nA: 可重复读。",
        )
        assertTrue(deck.cards.any { it.answer.contains("四种") })
        assertTrue(deck.cards.any { it.question.contains("默认级别") })
    }

    @Test
    fun schedulerProgressionAndReset() {
        val now = 1_700_000_000_000L
        val day = 86_400_000L
        var stat: CardStat? = null
        var result = Scheduler.schedule(stat, Rating.GOT, now)
        assertEquals(1, result.box)
        assertEquals(now + day, result.dueAt)
        stat = CardStat(box = result.box, dueAt = result.dueAt)
        result = Scheduler.schedule(stat, Rating.GOT, now)
        assertEquals(2, result.box)
        stat = CardStat(box = result.box, dueAt = result.dueAt)
        result = Scheduler.schedule(stat, Rating.GOT, now)
        assertEquals(3, result.box)
        result = Scheduler.schedule(stat, Rating.GOT, now)
        assertEquals(3, result.box)
        result = Scheduler.schedule(stat, Rating.FORGOT, now)
        assertEquals(1, result.box)
        assertEquals(now, result.dueAt)
        result = Scheduler.schedule(stat, Rating.FUZZY, now)
        assertEquals(now + day, result.dueAt)
    }

    @Test
    fun hashIsStableFnv() {
        assertEquals(8, CardParser.hash32("hello").length)
        assertEquals(CardParser.hash32("hello"), CardParser.hash32("hello"))
        assertNotEquals(CardParser.hash32("hello"), CardParser.hash32("world"))
    }

    @Test
    fun codeMaskHandlesUnclosedFence() {
        val mask = CardParser.codeMask(listOf("```js", "## not heading", "x"))
        assertEquals(3, mask.count { it })
        val cards = CardParser.parseMarkdown("## 真标题\n\n内容\n\n```\n## 假标题\n```")
        assertTrue(cards.cards.any { it.title == "真标题" })
        assertFalse(cards.cards.any { it.title == "假标题" })
    }
}
