package com.suishibeisong.flashcards

import com.suishibeisong.flashcards.data.ImageRefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageRefsTest {
    @Test
    fun rewritesStandardImageAndKeepsAlt() {
        val out = ImageRefs.rewrite(
            "看图 ![架构图](images/a.png) 结束",
            mapOf("a.png" to "abc123-a.png"),
        )
        assertEquals("看图 ![架构图](abc123-a.png) 结束", out)
    }

    @Test
    fun rewritesObsidianWikiImageAndWidth() {
        val out = ImageRefs.rewrite(
            "![[notes/images/a.png|300]]",
            mapOf("a.png" to "abc123-a.png"),
        )
        assertEquals("![[abc123-a.png|300]]", out)
    }

    @Test
    fun leavesUnrelatedReferencesUntouched() {
        val text = "![x](other.png) ![[other.png]]"
        assertEquals(text, ImageRefs.rewrite(text, mapOf("a.png" to "abc123-a.png")))
        assertTrue(ImageRefs.basename("images/dir/a.png?raw=1") == "a.png")
    }
}
