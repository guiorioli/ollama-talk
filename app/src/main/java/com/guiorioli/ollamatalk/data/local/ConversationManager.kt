package com.guiorioli.ollamatalk.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ConversationManager(context: Context) {

    private val gson = Gson()
    private val dir = File(context.filesDir, CONVERSATIONS_DIR).also { it.mkdirs() }
    private val indexFile = File(dir, INDEX_FILE)

    private fun loadIndex(): MutableList<ConversationIndexEntry> {
        if (!indexFile.exists()) return mutableListOf()
        val json = indexFile.readText()
        val type = object : TypeToken<MutableList<ConversationIndexEntry>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveIndex(entries: List<ConversationIndexEntry>) {
        indexFile.writeText(gson.toJson(entries))
    }

    fun listConversations(): List<ConversationIndexEntry> {
        return loadIndex().sortedByDescending { it.timestamp }
    }

    fun loadConversation(id: String): Conversation? {
        val file = File(dir, "${id}.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), Conversation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveConversation(
        id: String,
        messages: List<StoredMessage>,
        model: String
    ) {
        val title = generateTitle(messages)
        val timestamp = System.currentTimeMillis()

        val conversation = Conversation(
            id = id,
            title = title,
            messages = messages,
            timestamp = timestamp,
            model = model
        )

        File(dir, "${id}.json").writeText(gson.toJson(conversation))

        val index = loadIndex().toMutableList()
        val existing = index.indexOfFirst { it.id == id }
        val entry = ConversationIndexEntry(id, title, timestamp, model)
        if (existing >= 0) {
            index[existing] = entry
        } else {
            index.add(entry)
        }
        saveIndex(index)
    }

    fun deleteConversation(id: String) {
        File(dir, "${id}.json").delete()
        val index = loadIndex().toMutableList()
        index.removeAll { it.id == id }
        saveIndex(index)
    }

    private fun generateTitle(messages: List<StoredMessage>): String {
        val firstUser = messages.firstOrNull { it.role == "user" }
            ?: return "New conversation"
        val text = firstUser.content.trim()
        val cleaned = text.replace("\n", " ")
        return if (cleaned.length > MAX_TITLE_LEN) {
            cleaned.take(MAX_TITLE_LEN).trimEnd() + "..."
        } else {
            cleaned
        }
    }

    companion object {
        private const val CONVERSATIONS_DIR = "conversations"
        private const val INDEX_FILE = "index.json"
        private const val MAX_TITLE_LEN = 50
    }
}
