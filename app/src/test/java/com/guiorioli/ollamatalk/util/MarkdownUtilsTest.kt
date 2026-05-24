package com.guiorioli.ollamatalk.util

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
    fun `stripMarkdown removes nested bold and italic`() {
        assertEquals("texto", stripMarkdown("***texto***"))
        assertEquals("texto italico texto", stripMarkdown("**texto *italico* texto**"))
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
    fun `stripMarkdown removes headers on multiple lines`() {
        val input = "# Title\n## Section\n### Subsection"
        val result = stripMarkdown(input)
        assertFalse("Should not contain #", result.contains("#"))
        assertTrue("Should keep title text", result.contains("Title"))
        assertTrue("Should keep section text", result.contains("Section"))
        assertTrue("Should keep subsection text", result.contains("Subsection"))
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

    @Test
    fun `stripMarkdown removes inline LaTeX`() {
        // \rightarrow inside $...$ should at least have delimiters removed
        val result1 = stripMarkdown("Go to step ${'$'}\rightarrow${'$'} 2")
        assertFalse("Should not contain $", result1.contains("$"))
        assertTrue("Should keep surrounding text", result1.contains("Go to step"))
        assertTrue("Should keep surrounding text", result1.contains("2"))

        val result2 = stripMarkdown("a ${'$'}x${'$'} ${'$'}y${'$'} b")
        assertTrue("Should keep x", result2.contains("x"))
        assertTrue("Should keep y", result2.contains("y"))
    }

    @Test
    fun `stripMarkdown removes display LaTeX`() {
        val input = """Before
${'$'}${'$'}\n\sum_{i=1}^{n} i\n${'$'}${'$'}\nAfter"""
        val result = stripMarkdown(input)
        assertFalse("Should not contain $$", result.contains("$$"))
        assertTrue("Should keep surrounding text", result.contains("Before"))
        assertTrue("Should keep surrounding text", result.contains("After"))
    }

    @Test
    fun `stripMarkdown decodes and cleans HTML entities`() {
        assertEquals("Go to step → 2", stripMarkdown("Go to step &rightarrow; 2"))
        assertEquals("A < B > C", stripMarkdown("A &lt; B &gt; C"))
    }

    @Test
    fun `stripMarkdown removes table separators and pipes`() {
        val input = "| A | B |\n|---|---|\n| 1 | 2 |"
        val result = stripMarkdown(input)
        assertFalse("Should not contain pipes", result.contains("|"))
        assertTrue("Should keep cell text", result.contains("A"))
        assertTrue("Should keep cell text", result.contains("1"))
    }

    @Test
    fun `preProcessMarkdownForDisplay decodes HTML entities`() {
        assertEquals("Go → there", preProcessMarkdownForDisplay("Go &rightarrow; there"))
        assertEquals("A < B", preProcessMarkdownForDisplay("A &lt; B"))
    }

    @Test
    fun `preProcessMarkdownForDisplay replaces LaTeX arrows with Unicode`() {
        assertEquals("→", preProcessMarkdownForDisplay("""${'$'}\rightarrow${'$'}"""))
        assertEquals("←", preProcessMarkdownForDisplay("""${'$'}\leftarrow${'$'}"""))
        assertEquals("x → y", preProcessMarkdownForDisplay("""${'$'}x \to y${'$'}"""))
    }

    @Test
    fun `preProcessMarkdownForDisplay replaces display LaTeX`() {
        val input = """${'$'}${'$'}\n\sum_{i=1}^{n} i \rightarrow \infty\n${'$'}${'$'}"""
        val result = preProcessMarkdownForDisplay(input)
        assertFalse("Should not contain latex backslash arrows", result.contains("\\rightarrow"))
        assertTrue("Should contain Unicode arrow", result.contains("→"))
        assertTrue("Should contain infinity", result.contains("∞"))
    }

    @Test
    fun `stripMarkdown extracts text from LaTeX text command`() {
        assertEquals("energy", stripMarkdown("${'$'}\\text{energy}${'$'}"))
        assertEquals("Foo bar", stripMarkdown("${'$'}\\textbf{Foo} \\textit{bar}${'$'}"))
    }

    @Test
    fun `stripMarkdown converts frac to slash`() {
        assertEquals("1/2", stripMarkdown("${'$'}\\frac{1}{2}${'$'}"))
    }

    @Test
    fun `stripMarkdown removes isolated braces`() {
        assertEquals("kcal", stripMarkdown("{kcal}"))
        assertEquals("A value B", stripMarkdown("A {value} B"))
    }

    @Test
    fun `preProcessMarkdownForDisplay handles LaTeX commands with braces`() {
        assertEquals("energy", preProcessMarkdownForDisplay("${'$'}\\text{energy}${'$'}"))
        assertEquals("Foo bar", preProcessMarkdownForDisplay("${'$'}\\textbf{Foo} \\textit{bar}${'$'}"))
    }

    @Test
    fun `preProcessMarkdownForDisplay converts frac to slash`() {
        assertEquals("1/2", preProcessMarkdownForDisplay("${'$'}\\frac{1}{2}${'$'}"))
    }

    @Test
    fun `preProcessMarkdownForDisplay removes isolated braces`() {
        assertEquals("kcal", preProcessMarkdownForDisplay("{kcal}"))
    }

    @Test
    fun `stripMarkdown handles sqrt and binom`() {
        assertTrue(stripMarkdown("${'$'}\\sqrt{2}${'$'}").contains("sqrt(2)"))
        assertTrue(stripMarkdown("${'$'}\\binom{5}{2}${'$'}").contains("C(5,2)"))
    }

    @Test
    fun `formatConversationText formats user and assistant messages`() {
        val messages = listOf(
            ConversationMessage(role = "user", content = "Hello **world**"),
            ConversationMessage(role = "assistant", content = "Hi *there*", hasImage = false, isLoading = false)
        )
        val result = formatConversationText(messages, "gemma3:27b-cloud")
        assertTrue(result.contains("--- User"))
        assertTrue(result.contains("Hello world"))
        assertTrue(result.contains("--- Ollama (gemma3:27b-cloud)"))
        assertTrue(result.contains("Hi there"))
        assertFalse(result.contains("**"))
        assertFalse(result.contains("*"))
    }

    @Test
    fun `formatConversationText skips loading messages`() {
        val messages = listOf(
            ConversationMessage(role = "user", content = "Question"),
            ConversationMessage(role = "assistant", content = "", isLoading = true)
        )
        val result = formatConversationText(messages, "model-x")
        assertTrue(result.contains("--- User"))
        assertTrue(result.contains("Question"))
        assertFalse(result.contains("--- Ollama"))
    }

    @Test
    fun `formatConversationText handles image placeholder`() {
        val messages = listOf(
            ConversationMessage(role = "user", content = "", hasImage = true),
            ConversationMessage(role = "user", content = "Look at this", hasImage = true)
        )
        val result = formatConversationText(messages, "model-y")
        assertTrue(result.contains("[Image]"))
        assertTrue(result.contains("[Image] Look at this"))
    }

    @Test
    fun `formatConversationText returns empty string for empty list`() {
        assertEquals("", formatConversationText(emptyList(), "model-z"))
    }
}
