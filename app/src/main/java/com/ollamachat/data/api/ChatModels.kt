package com.ollamachat.data.api

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

data class ChatResponse(
    val model: String,
    val message: ChatMessage,
    val done: Boolean
)

data class ModelInfo(
    val name: String
)

data class TagsResponse(
    val models: List<ModelInfo>?
)
