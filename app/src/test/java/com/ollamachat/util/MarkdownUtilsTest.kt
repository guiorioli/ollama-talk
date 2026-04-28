package com.ollamachat.util

import org.junit.Assert.*
import org.junit.Test

class MarkdownUtilsTest {

    @Test
    fun `stripMarkdown removes bold markers`() {
        assertEquals("hello world", stripMarkdown("**hello** world"))
        assertEquals("hello world", stripMarkdown("__hello__ world"))
    }

    @Test
    fun `stripMarkdown removes italic markers`() {
        assertEquals("hello world", stripMarkdown("*hello* world"))
        assertEquals("hello world", stripMarkdown("_hello_ world"))
    }

    @Test
    fun `stripMarkdown removes code blocks`() {
        val input = "Here is code:\n```\nval x = 1\n```\nAnd more text."
        val result = stripMarkdown(input)
        assertFalse("Should not contain code block markers", result.contains("```"))
        assertTrue("Should keep surrounding text", result.contains("Here is code:"))
        assertTrue("Should keep surrounding text", result.contains("And more text."))
    }

    @Test
    fun `stripMarkdown removes inline code`() {
        assertEquals("use the", stripMarkdown("use the `function`"))
    }

    @Test
    fun `stripMarkdown removes link formatting keeping text`() {
        assertEquals("click here", stripMarkdown("[click here](https://example.com)"))
    }

    @Test
    fun `stripMarkdown removes headers`() {
        assertEquals("My Title", stripMarkdown("# My Title"))
        assertEquals("Subtitle", stripMarkdown("### Subtitle"))
    }

    @Test
    fun `stripMarkdown removes blockquotes`() {
        assertEquals("quoted text", stripMarkdown("> quoted text"))
    }

    @Test
    fun `stripMarkdown removes list markers`() {
        assertEquals("item one", stripMarkdown("- item one"))
        assertEquals("item two", stripMarkdown("* item two"))
        assertEquals("item three", stripMarkdown("+ item three"))
        assertEquals("numbered", stripMarkdown("1. numbered"))
    }

    @Test
    fun `stripMarkdown handles text with no markdown`() {
        val plain = "Plain text without any markdown."
        assertEquals(plain, stripMarkdown(plain))
    }

    @Test
    fun `stripMarkdown handles empty string`() {
        assertEquals("", stripMarkdown(""))
    }

    @Test
    fun `stripMarkdown handles code-only response`() {
        val codeOnly = "```\nfun main() {}\n```"
        val result = stripMarkdown(codeOnly)
        assertTrue("Code-only text should become empty or whitespace", result.isBlank())
    }
}
