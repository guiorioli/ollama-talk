package com.guiorioli.ollamatalk.data.api

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_name: String? = null
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val tools: List<Tool>? = null
)

data class ChatResponse(
    val model: String,
    val message: ChatMessage,
    val done: Boolean
)

data class ChatStreamChunk(
    val model: String? = null,
    val message: ChatMessage,
    val done: Boolean = false
)

data class ModelInfo(
    val name: String
)

data class TagsResponse(
    val models: List<ModelInfo>?
)

// --- Tool Calling Models ---

data class Tool(
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

data class ToolParameters(
    val type: String = "object",
    val required: List<String>,
    val properties: Map<String, ToolProperty>
)

data class ToolProperty(
    val type: String,
    val description: String,
    val default: Any? = null,
    val enum: List<String>? = null
)

data class ToolCall(
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: Map<String, Any>
)

// --- Web Search Models ---

data class WebSearchResult(
    val title: String,
    val url: String,
    val content: String
)

data class WebSearchResponse(
    val results: List<WebSearchResult>
)
