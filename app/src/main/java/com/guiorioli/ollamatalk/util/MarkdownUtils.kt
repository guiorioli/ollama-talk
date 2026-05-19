package com.guiorioli.ollamatalk.util

fun stripMarkdown(markdown: String): String {
    var result = markdown
    // Replace $$...$$ display math: process inner LaTeX, keep readable text
    result = result.replace(Regex("\\$\\$([\\s\\S]*?)\\$\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        inner = stripLatexCommandsWithArgs(inner)
        inner = applyLatexMap(inner)
        " $inner "
    }
    // Replace $...$ inline math similarly
    result = result.replace(Regex("\\$([^\\$\\n]+)\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        inner = stripLatexCommandsWithArgs(inner)
        inner = applyLatexMap(inner)
        " $inner "
    }
    return result
        .replace(Regex("```[\\s\\S]*?```"), "")             // code blocks
        .replace(Regex("`[^`]+`"), "")                        // inline code
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // links
        .replace(Regex("~~([^~]+)~~"), "$1")                // strikethrough
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")        // bold
        .replace(Regex("__([^_]+)__"), "$1")                // bold (alt)
        .replace(Regex("\\*([^*]+)\\*"), "$1")              // italic
        .replace(Regex("_([^_]+)_"), "$1")                  // italic (alt)
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")                   // headers
        .replace(Regex("^>\\s+", RegexOption.MULTILINE), "")                        // blockquotes
        .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "")                    // unordered lists
        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")                  // ordered lists
        .replace(Regex("^[-*_]{3,}\\s*$", RegexOption.MULTILINE), "")               // horizontal rules
        .replace(Regex("^\\|?\\s*:?---+:?\\s*\\|?.*$", RegexOption.MULTILINE), "") // table separators
        .replace(Regex("\\|"), " ")                           // table column separators
        .let { stripLatexCommandsWithArgs(it) }                // clean leftover LaTeX braces/commands
        .let { decodeHtmlEntities(it) }                        // decode HTML entities
        .replace(Regex("\\n{3,}"), "\n\n")                   // multiple newlines
        .trim()
}

data class ConversationMessage(
    val role: String,
    val content: String,
    val hasImage: Boolean = false,
    val isLoading: Boolean = false
)

fun formatConversationText(messages: List<ConversationMessage>, model: String): String {
    return buildString {
        messages.filter { !it.isLoading && it.role != "tool" }.forEach { msg ->
            val prefix = if (msg.role == "user") "--- User" else "--- Ollama ($model)"
            appendLine(prefix)
            val content = if (msg.hasImage) {
                if (msg.content.isBlank()) "[Image]" else "[Image] ${msg.content}"
            } else msg.content
            appendLine(stripMarkdown(content))
            appendLine()
        }
    }.trim()
}

fun preProcessMarkdownForDisplay(markdown: String): String {
    return markdown
        .let { stripLatexCommandsWithArgs(it) }
        .let { replaceLatexWithUnicode(it) }
        .let { decodeHtmlEntities(it) }
}

private fun stripLatexCommandsWithArgs(text: String): String {
    var result = text
    // \\text{...}, \\mathrm{...}, \\mathbf{...}, \\textit{...}, \\textbf{...} — keep inner text
    result = result.replace(Regex("""\\text\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\mathrm\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\mathbf\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\textit\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\textbf\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\mathit\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\mathsf\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\mathcal\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\operatorname\{([^}]+)\}"""), "$1")

    // \\frac{numerator}{denominator}
    result = result.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")) { matchResult ->
        "${matchResult.groupValues[1]}/${matchResult.groupValues[2]}"
    }

    // \\sqrt{...}
    result = result.replace(Regex("""\\sqrt\{([^}]+)\}""")) { matchResult ->
        "sqrt(${matchResult.groupValues[1]})"
    }

    // \\sqrt[n]{...}
    result = result.replace(Regex("""\\sqrt\[(\d+)]\{([^}]+)\}""")) { matchResult ->
        "${matchResult.groupValues[1]}th root of ${matchResult.groupValues[2]}"
    }

    // \\cancel{...}, \\overline{...}, \\underline{...}
    result = result.replace(Regex("""\\cancel\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\overline\{([^}]+)\}"""), "$1")
    result = result.replace(Regex("""\\underline\{([^}]+)\}"""), "$1")

    // \\binom{n}{k}
    result = result.replace(Regex("""\\binom\{([^}]+)\}\{([^}]+)\}""")) { matchResult ->
        "C(${matchResult.groupValues[1]},${matchResult.groupValues[2]})"
    }

    // \\limits_{...}^{...} and similar sub/superscripts
    result = result.replace(Regex("""\{([^}]+)\}\^\{([^}]+)\}""")) { matchResult ->
        "${matchResult.groupValues[1]}^${matchResult.groupValues[2]}"
    }

    // Remove leftover isolated braces like {kcal}, {value}
    result = result.replace(Regex("""\{([^}]+)\}"""), "$1")

    return result
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

private val latexMap = mapOf(
    "\\right" + "arrow" to "→",
    "\\left" + "arrow" to "←",
    "\\up" + "arrow" to "↑",
    "\\down" + "arrow" to "↓",
    "\\Right" + "arrow" to "⇒",
    "\\Left" + "arrow" to "⇐",
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
    "\\iff" to "⇔"
)

private fun applyLatexMap(text: String): String {
    var result = text
    latexMap.forEach { (cmd, unicode) ->
        result = result.replace(cmd, unicode)
    }
    return result
}

private fun replaceLatexWithUnicode(text: String): String {
    var result = text
    // Replace $$...$$ display math: keep content, strip delimiters, replace latex symbols inside
    result = result.replace(Regex("\\$\\$([\\s\\S]*?)\\$\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        inner = stripLatexCommandsWithArgs(inner)
        inner = applyLatexMap(inner)
        inner
    }
    // Replace $...$ inline math similarly
    result = result.replace(Regex("\\$([^\\$\\n]+)\\$")) { matchResult ->
        var inner = matchResult.groupValues[1]
        inner = stripLatexCommandsWithArgs(inner)
        inner = applyLatexMap(inner)
        inner
    }
    // Also replace any remaining LaTeX commands outside math blocks
    result = applyLatexMap(result)
    return result
}
