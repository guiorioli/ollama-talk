package com.guiorioli.ollamatalk.data.api

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ChatModelsTest {

    private val gson = Gson()

    @Test
    fun `ChatMessage serializes without images`() {
        val msg = ChatMessage(role = "user", content = "Hello")
        val json = gson.toJson(msg)
        assertTrue("JSON should have role", json.contains("\"role\":\"user\""))
        assertTrue("JSON should have content", json.contains("\"content\":\"Hello\""))
        assertFalse("JSON should not have images field", json.contains("\"images\""))
    }

    @Test
    fun `ChatMessage serializes with images`() {
        val msg = ChatMessage(role = "user", content = "Hello", images = listOf("base64data"))
        val json = gson.toJson(msg)
        assertTrue("JSON should include images", json.contains("\"images\""))
        assertTrue("JSON should include base64 data", json.contains("base64data"))
    }

    @Test
    fun `ChatMessage images field is excluded when null`() {
        // Gson by default excludes null fields for non-primitive types
        val msg = ChatMessage(role = "user", content = "Hello", images = null)
        val json = gson.toJson(msg)
        assertFalse("JSON should not contain null images", json.contains("\"images\":null"))
        // Actually Gson does include null by default. Let me verify the structure is valid.
        val parsed = gson.fromJson(json, ChatMessage::class.java)
        assertEquals("user", parsed.role)
        assertEquals("Hello", parsed.content)
        assertNull("Images should be null after round-trip", parsed.images)
    }

    @Test
    fun `ChatRequest serialization produces valid JSON for API`() {
        val messages = listOf(
            ChatMessage(role = "user", content = "Hello"),
            ChatMessage(role = "user", content = "With image", images = listOf("abc123"))
        )
        val request = ChatRequest(model = "gemma3:27b-cloud", messages = messages, stream = false)
        val json = gson.toJson(request)

        assertTrue("JSON should contain model", json.contains("gemma3:27b-cloud"))
        assertTrue("JSON should contain stream:false", json.contains("\"stream\":false"))
        assertTrue("JSON should contain messages", json.contains("\"messages\""))
        assertTrue("JSON should have images for the second message", json.contains("abc123"))
    }

    @Test
    fun `ChatResponse deserialization from typical API response`() {
        val responseJson = """
        {
            "model": "gemma3:27b-cloud",
            "message": {
                "role": "assistant",
                "content": "Hello, how can I help?"
            },
            "done": true
        }
        """
        val response = gson.fromJson(responseJson, ChatResponse::class.java)
        assertEquals("gemma3:27b-cloud", response.model)
        assertEquals("assistant", response.message.role)
        assertEquals("Hello, how can I help?", response.message.content)
        assertTrue(response.done)
    }

    @Test
    fun `ModelInfo deserialization`() {
        val json = """{"name": "gemma3:27b-cloud"}"""
        val model = gson.fromJson(json, ModelInfo::class.java)
        assertEquals("gemma3:27b-cloud", model.name)
    }

    @Test
    fun `TagsResponse deserialization`() {
        val json = """
        {
            "models": [
                {"name": "gemma3:27b-cloud"},
                {"name": "llama3:8b"}
            ]
        }
        """
        val tags = gson.fromJson(json, TagsResponse::class.java)
        assertEquals(2, tags.models?.size)
        assertEquals("gemma3:27b-cloud", tags.models?.get(0)?.name)
        assertEquals("llama3:8b", tags.models?.get(1)?.name)
    }

    @Test
    fun `ChatStreamChunk deserialization from streaming response`() {
        val json = """{"model":"llama3","message":{"role":"assistant","content":"Hello"},"done":false}"""
        val chunk = gson.fromJson(json, ChatStreamChunk::class.java)
        assertEquals("llama3", chunk.model)
        assertEquals("assistant", chunk.message.role)
        assertEquals("Hello", chunk.message.content)
        assertFalse(chunk.done)
    }

    @Test
    fun `ChatStreamChunk final chunk with done true`() {
        val json = """
            {
                "model": "llama3",
                "message": {"role":"assistant","content":""},
                "done": true,
                "total_duration": 12345678
            }
        """.trimIndent()
        val chunk = gson.fromJson(json, ChatStreamChunk::class.java)
        assertTrue(chunk.done)
        assertEquals("llama3", chunk.model)
    }

    @Test
    fun `ChatStreamChunk empty content`() {
        val json = """{"message":{"role":"assistant","content":""},"done":false}"""
        val chunk = gson.fromJson(json, ChatStreamChunk::class.java)
        assertEquals("", chunk.message.content)
        assertFalse(chunk.done)
        assertNull(chunk.model)
    }

    // --- Tool Calling Tests ---

    @Test
    fun `ChatMessage with tool_calls serializes correctly`() {
        val toolCall = ToolCall(
            function = ToolCallFunction(
                name = "web_search",
                arguments = mapOf("query" to "latest AI news", "max_results" to 5)
            )
        )
        val msg = ChatMessage(
            role = "assistant",
            content = "",
            tool_calls = listOf(toolCall)
        )
        val json = gson.toJson(msg)
        assertTrue("JSON should contain tool_calls", json.contains("tool_calls"))
        assertTrue("JSON should contain web_search", json.contains("web_search"))
    }

    @Test
    fun `ChatMessage with tool_name serializes correctly`() {
        val msg = ChatMessage(
            role = "tool",
            content = "Search results here",
            tool_name = "web_search"
        )
        val json = gson.toJson(msg)
        assertTrue("JSON should contain tool_name", json.contains("tool_name"))
        assertTrue("JSON should contain web_search", json.contains("web_search"))
    }

    @Test
    fun `ChatRequest with tools serializes correctly`() {
        val tool = Tool(
            function = ToolFunction(
                name = "web_search",
                description = "Search the web",
                parameters = ToolParameters(
                    required = listOf("query"),
                    properties = mapOf(
                        "query" to ToolProperty(type = "string", description = "The query")
                    )
                )
            )
        )
        val request = ChatRequest(
            model = "gemma3:27b-cloud",
            messages = listOf(ChatMessage(role = "user", content = "Hello")),
            stream = false,
            tools = listOf(tool)
        )
        val json = gson.toJson(request)
        assertTrue("JSON should contain tools", json.contains("tools"))
        assertTrue("JSON should contain web_search", json.contains("web_search"))
    }

    @Test
    fun `ChatResponse with tool_calls deserializes correctly`() {
        val json = """
        {
            "model": "gemma3:27b-cloud",
            "message": {
                "role": "assistant",
                "content": "",
                "tool_calls": [
                    {
                        "function": {
                            "name": "web_search",
                            "arguments": {"query": "latest AI", "max_results": 5}
                        }
                    }
                ]
            },
            "done": true
        }
        """
        val response = gson.fromJson(json, ChatResponse::class.java)
        assertNotNull(response.message.tool_calls)
        assertEquals(1, response.message.tool_calls?.size)
        assertEquals("web_search", response.message.tool_calls?.get(0)?.function?.name)
        assertEquals("latest AI", response.message.tool_calls?.get(0)?.function?.arguments?.get("query"))
    }

    @Test
    fun `Tool serialization produces valid JSON`() {
        val tool = Tool(
            function = ToolFunction(
                name = "web_search",
                description = "Search the web",
                parameters = ToolParameters(
                    required = listOf("query"),
                    properties = mapOf(
                        "query" to ToolProperty(type = "string", description = "The query"),
                        "max_results" to ToolProperty(type = "integer", description = "Max results", default = 5)
                    )
                )
            )
        )
        val json = gson.toJson(tool)
        assertTrue("JSON should contain type:function", json.contains("function"))
        assertTrue("JSON should contain name", json.contains("web_search"))
        assertTrue("JSON should contain parameters", json.contains("parameters"))
    }

    @Test
    fun `WebSearchResponse deserialization`() {
        val json = """
        {
            "results": [
                {"title": "AI News", "url": "https://example.com/ai", "content": "Latest AI developments"}
            ]
        }
        """
        val response = gson.fromJson(json, WebSearchResponse::class.java)
        assertEquals(1, response.results!!.size)
        assertEquals("AI News", response.results!![0].title)
        assertEquals("https://example.com/ai", response.results!![0].url)
        assertEquals("Latest AI developments", response.results!![0].content)
    }

    @Test
    fun `KNOWN_TOOLS_MODELS contains expected models`() {
        assertTrue("gemma4 should be in known models", OllamaApiService.KNOWN_TOOLS_MODELS.contains("gemma4"))
        assertTrue("gemma3 should be in known models", OllamaApiService.KNOWN_TOOLS_MODELS.contains("gemma3"))
        assertTrue("gpt-oss should be in known models", OllamaApiService.KNOWN_TOOLS_MODELS.contains("gpt-oss"))
        assertTrue("kimi-k2.6 should be in known models", OllamaApiService.KNOWN_TOOLS_MODELS.contains("kimi-k2.6"))
        assertFalse("unknown-model should not be in known models", OllamaApiService.KNOWN_TOOLS_MODELS.contains("unknown-model"))
    }

    @Test
    fun `ChatMessage content can be null`() {
        val msg = ChatMessage(role = "assistant", content = null, tool_calls = emptyList())
        assertNull(msg.content)
    }

    @Test
    fun `ChatResponse model can be null`() {
        val json = """{"message":{"role":"assistant","content":""},"done":true}"""
        val response = gson.fromJson(json, ChatResponse::class.java)
        assertNull(response.model)
    }
}
