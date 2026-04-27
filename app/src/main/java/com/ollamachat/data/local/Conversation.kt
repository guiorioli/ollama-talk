package com.ollamachat.data.local

data class Conversation(
    val id: String,
    val title: String,
    val messages: List<StoredMessage>,
    val timestamp: Long,
    val model: String
)

data class StoredMessage(
    val role: String,
    val content: String
)
