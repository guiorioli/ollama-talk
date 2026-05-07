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
}
