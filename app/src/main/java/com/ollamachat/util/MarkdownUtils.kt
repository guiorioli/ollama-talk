package com.ollamachat.util

fun stripMarkdown(markdown: String): String {
    return markdown
        .replace(Regex("```[\\s\\S]*?```"), "")  // code blocks
        .replace(Regex("`[^`]+`"), "")            // inline code
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")  // links
        .replace(Regex("~~([^~]+)~~"), "$1")      // strikethrough
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")  // bold
        .replace(Regex("__([^_]+)__"), "$1")      // bold (alt)
        .replace(Regex("\\*([^*]+)\\*"), "$1")    // italic
        .replace(Regex("_([^_]+)_"), "$1")        // italic (alt)
        .replace(Regex("^#{1,6}\\s+"), "")        // headers
        .replace(Regex("^>\\s+"), "")             // blockquotes
        .replace(Regex("^[-*+]\\s+"), "")         // unordered lists
        .replace(Regex("^\\d+\\.\\s+"), "")       // ordered lists
        .replace(Regex("^[-*_]{3,}\\s*$"), "")    // horizontal rules
        .replace(Regex("\\n{3,}"), "\n\n")       // multiple newlines
        .trim()
}
