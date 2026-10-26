package com.fest.mdflashcards.data

object ImageRefs {
    /** Rewrites standard and Obsidian wiki image references to stored file names. */
    fun rewrite(text: String, renames: Map<String, String>): String {
        if (renames.isEmpty()) return text
        var out = text.replace(Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")) { m ->
            val target = m.groupValues[2].trim().substringBefore(' ').trim('"')
            val base = basename(target)
            val stored = renames[base]
            if (stored != null) "![${m.groupValues[1]}]($stored)" else m.value
        }
        out = out.replace(Regex("!\\[\\[([^\\]|]+)(\\|[^\\]]*)?]]")) { m ->
            val stored = renames[basename(m.groupValues[1].trim())]
            if (stored != null) "![[$stored${m.groupValues[2]}]]" else m.value
        }
        return out
    }

    fun basename(path: String): String =
        path.substringAfterLast('/').substringAfterLast('\\').substringBefore('?')
}
