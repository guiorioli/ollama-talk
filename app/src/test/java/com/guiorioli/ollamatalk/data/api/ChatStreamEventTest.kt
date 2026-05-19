package com.guiorioli.ollamatalk.data.api

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ChatStreamEventTest {

    @Test
    fun `TextChunk holds text value`() {
        val event = ChatStreamEvent.TextChunk("Hello")
        assertEquals("Hello", event.text)
    }

    @Test
    fun `ToolCallDetected holds tool calls and accumulated content`() {
        val toolCalls = listOf(
            ToolCall(
                function = ToolCallFunction(
                    name = "web_search",
                    arguments = mapOf("query" to "test")
                )
            )
        )
        val event = ChatStreamEvent.ToolCallDetected(
            toolCalls = toolCalls,
            accumulatedContent = "Let me search"
        )
        assertEquals(1, event.toolCalls.size)
        assertEquals("web_search", event.toolCalls[0].function.name)
        assertEquals("Let me search", event.accumulatedContent)
    }

    @Test
    fun `StreamError holds exception`() {
        val exception = IllegalStateException("test error")
        val event = ChatStreamEvent.StreamError(exception)
        assertEquals("test error", event.exception.message)
    }

    @Test
    fun `Done is singleton`() = runTest {
        // Verify Done can be emitted and collected in a flow
        val flow = kotlinx.coroutines.flow.flow {
            emit(ChatStreamEvent.Done)
        }
        val events = flow.toList()
        assertEquals(1, events.size)
        assertSame(ChatStreamEvent.Done, events[0])
    }

    @Test
    fun `ChatStreamChunk with tool_calls deserializes in streaming context`() {
        val gson = com.google.gson.Gson()
        val json = """
            {
                "model": "llama3",
                "message": {
                    "role": "assistant",
                    "content": "",
                    "tool_calls": [
                        {
                            "function": {
                                "name": "web_search",
                                "arguments": {"query": "streaming test"}
                            }
                        }
                    ]
                },
                "done": false
            }
        """.trimIndent()
        val chunk = gson.fromJson(json, ChatStreamChunk::class.java)
        assertNotNull(chunk.message.tool_calls)
        assertEquals(1, chunk.message.tool_calls!!.size)
        assertEquals("web_search", chunk.message.tool_calls!![0].function.name)
        assertFalse(chunk.done)
    }

    @Test
    fun `ChatStreamChunk with content only deserializes for normal streaming`() {
        val gson = com.google.gson.Gson()
        val json = """
            {
                "model": "llama3",
                "message": {"role":"assistant","content":"Hello world"},
                "done": true
            }
        """.trimIndent()
        val chunk = gson.fromJson(json, ChatStreamChunk::class.java)
        assertNull(chunk.message.tool_calls)
        assertEquals("Hello world", chunk.message.content)
        assertTrue(chunk.done)
    }
}
