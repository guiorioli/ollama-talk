package com.ollamachat.util

fun stripMarkdown(markdown: String): String {
    return markdown
        .replace(Regex("\\$\\$[\\s\\S]*?\\$\\$"), "")          // display LaTeX
        .replace(Regex("\\$[^\\$\\n]+\\$"), "")              // inline LaTeX
        .replace(Regex("```[\\s\\S]*?```"), "")             // code blocks
        .replace(Regex("`[^`]+`"), "")                        // inline code
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // links
        .replace(Regex("~~([^~]+)~~"), "$1")                // strikethrough
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")        // bold
        .replace(Regex("__([^_]+)__"), "$1")                // bold (alt)
        .replace(Regex("\\*([^*]+)\\*"), "$1")              // italic
        .replace(Regex("_([^_]+)_"), "$1")                  // italic (alt)
        .replace(Regex("^#{1,6}\\s+"), "")                   // headers
        .replace(Regex("^>\\s+"), "")                        // blockquotes
        .replace(Regex("^[-*+]\\s+"), "")                    // unordered lists
        .replace(Regex("^\\d+\\.\\s+"), "")                  // ordered lists
        .replace(Regex("^[-*_]{3,}\\s*$"), "")               // horizontal rules
        .replace(Regex("^\\|?\\s*:?---+:?\\s*\\|?.*$", RegexOption.MULTILINE), "") // table separators
        .replace(Regex("\\|"), " ")                           // table column separators
        .let { decodeHtmlEntities(it) }                        // decode HTML entities
        .replace(Regex("\\n{3,}"), "\n\n")                   // multiple newlines
        .trim()
}

fun preProcessMarkdownForDisplay(markdown: String): String {
    return markdown
        .let { replaceLatexWithUnicode(it) }
        .let { decodeHtmlEntities(it) }
}

private fun decodeHtmlEntities(text: String): String {
    val entities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&rightarrow;" to "→",
        "&leftarrow;" to "←",
        "&uparrow;" to "↑",
        "&downarrow;" to "↓",
        "&Rightarrow;" to "⇒",
        "&Leftarrow;" to "⇐",
        "&times;" to "×",
        "&plusmn;" to "±",
        "&le;" to "≤",
        "&ge;" to "≥",
        "&ne;" to "≠",
        "&infin;" to "∞",
        "&alpha;" to "α",
        "&beta;" to "β",
        "&gamma;" to "γ",
        "&delta;" to "δ",
        "&epsilon;" to "ε",
        "&sum;" to "∑",
        "&prod;" to "∏",
        "&int;" to "∫",
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&bull;" to "•",
        "&hellip;" to "…",
        "&nbsp;" to " ",
        "&copy;" to "©",
        "&reg;" to "®"
    )
    var result = text
    entities.forEach { (entity, char) ->
        result = result.replace(entity, char)
    }
    // numeric entities: &#123; or &#x7B;
    result = result.replace(Regex("&#(\\d+);")) { matchResult ->
        val code = matchResult.groupValues[1].toIntOrNull()
        if (code != null && code in 0..0x10FFFF) code.toChar().toString() else matchResult.value
    }
    result = result.replace(Regex("&#x([0-9a-fA-F]+);")) { matchResult ->
        val code = matchResult.groupValues[1].toIntOrNull(16)
        if (code != null && code in 0..0x10FFFF) code.toChar().toString() else matchResult.value
    }
    return result
}

private fun replaceLatexWithUnicode(text: String): String {
    val latexMap = mapOf(
        "\\rightarrow" to "→",
        "\\leftarrow" to "←",
        "\\uparrow" to "↑",
        "\\downarrow" to "↓",
        "\\Rightarrow" to "⇒",
        "\\Leftarrow" to "⇐",
        "\\times" to "×",
        "\\pm" to "±",
        "\\leq" to "≤",
        "\\geq" to "≥",
        "\\neq" to "≠",
        "\\infty" to "∞",
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\epsilon" to "ε",
        "\\sum" to "∑",
        "\\prod" to "∏",
        "\\int" to "∫",
        "\\cdot" to "·",
        "\\ldots" to "…",
        "\\bullet" to "•",
        "\\to" to "→",
        "\\mapsto" to "↦",
        "\\in" to "∈",
        "\\subset" to "⊂",
        "\\subseteq" to "⊆",
        "\\cup" to "∪",
        "\\cap" to "∩",
        "\\emptyset" to "∅",
        "\\forall" to "∀",
        "\\exists" to "∃",
        "\\neg" to "¬",
        "\\wedge" to "∧",
        "\\vee" to "∨",
        "\\Rightarrow" to "⇒",
        "\\iff" to "⇔"
    )

    var result = text
    // Replace $$...$$ display math: keep content, strip delimiters, replace latex symbols inside
    result = result.replace(Regex("\\$\\$([\\s\\S]*?)\\$\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        latexMap.forEach { (cmd, unicode) ->
            inner = inner.replace(cmd, unicode)
        }
        inner
    }
    // Replace $...$ inline math similarly
    result = result.replace(Regex("\\$([^\\$\\n]+)\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        latexMap.forEach { (cmd, unicode) ->
            inner = inner.replace(cmd, unicode)
        }
        inner
    }
    return result
}
