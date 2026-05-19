package com.guiorioli.ollamatalk.data.local

data class Conversation(
    val id: String,
    val title: String,
    val messages: List<StoredMessage>,
    val timestamp: Long,
    val model: String
)

data class StoredMessage(
    val role: String,
    val content: String,
    val tool_name: String? = null,
    val tool_calls: List<StoredToolCall>? = null
)

data class StoredToolCall(
    val name: String,
    val arguments: Map<String, Any>
)
