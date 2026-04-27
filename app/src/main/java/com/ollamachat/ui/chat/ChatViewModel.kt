package com.ollamachat.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ollamachat.audio.SpeechRecognizerManager
import com.ollamachat.audio.TextToSpeechManager
import com.ollamachat.data.api.ChatMessage
import com.ollamachat.data.api.OllamaApiService
import com.ollamachat.data.local.ConversationIndexEntry
import com.ollamachat.data.local.ConversationManager
import com.ollamachat.data.local.PreferencesManager
import com.ollamachat.data.local.StoredMessage
import com.ollamachat.data.local.TtsLanguage
import com.ollamachat.util.stripMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val speakingMessageId: Long? = null,
    val isAutoSpeak: Boolean = false,
    val error: String? = null,
    val hasApiKey: Boolean = false,
    val conversations: List<ConversationIndexEntry> = emptyList(),
    val currentConversationId: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val apiService = OllamaApiService()
    private val conversationManager = ConversationManager(application)
    val speechRecognizer = SpeechRecognizerManager(application)
    val textToSpeech = TextToSpeechManager(application)

    private val _state = MutableStateFlow(
        ChatUiState(
            hasApiKey = prefs.apiKey.isNotBlank(),
            conversations = conversationManager.listConversations()
        )
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0L

    init {
        setupSpeechRecognizer()
        setupTextToSpeech()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.onResult = { text ->
            _state.value = _state.value.copy(inputText = text, isListening = false)
            sendMessage()
        }
        speechRecognizer.onError = { error ->
            _state.value = _state.value.copy(
                error = error,
                isListening = false
            )
        }
        speechRecognizer.onListeningChange = { listening ->
            _state.value = _state.value.copy(isListening = listening)
        }
    }

    private fun setupTextToSpeech() {
        textToSpeech.onDone = {
            _state.value = _state.value.copy(isSpeaking = false, speakingMessageId = null)
        }
        textToSpeech.onError = {
            _state.value = _state.value.copy(isSpeaking = false, error = it)
        }
    }

    fun onInputChanged(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        if (_state.value.currentConversationId == null) {
            _state.value = _state.value.copy(
                currentConversationId = "${System.currentTimeMillis()}"
            )
        }

        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            _state.value = _state.value.copy(error = "Set your API Key in Settings")
            return
        }

        val userMessage = ChatUiMessage(
            id = messageIdCounter++,
            role = "user",
            content = text
        )

        val loadingMessage = ChatUiMessage(
            id = messageIdCounter++,
            role = "assistant",
            content = "",
            isLoading = true
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage + loadingMessage,
            inputText = "",
            isLoading = true,
            error = null
        )

        val chatMessages = (_state.value.messages + userMessage)
            .filter { !it.isLoading }
            .map { ChatMessage(role = it.role, content = it.content) }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                apiService.chat(prefs.selectedModel, chatMessages, apiKey)
            }

            result.fold(
                onSuccess = { response ->
                    val content = response.message.content
                    val assistantMessageId = _state.value.messages.firstOrNull { it.isLoading }?.id
                    val updatedMessages = _state.value.messages.map { msg ->
                        if (msg.isLoading) {
                            msg.copy(
                                content = content,
                                isLoading = false
                            )
                        } else msg
                    }
                    _state.value = _state.value.copy(
                        messages = updatedMessages,
                        isLoading = false
                    )
                    if (_state.value.isAutoSpeak) {
                        speakMessage(content, assistantMessageId)
                    }
                    saveCurrentConversation()
                },
                onFailure = { error ->
                    val updatedMessages = _state.value.messages.filter { !it.isLoading }
                    _state.value = _state.value.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        error = error.message ?: "Error sending message"
                    )
                }
            )
        }
    }

    fun startListening() {
        speechRecognizer.startListening()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun cancelVoiceInput() {
        speechRecognizer.stopListening()
        _state.value = _state.value.copy(inputText = "", isListening = false)
    }

    fun speakMessage(content: String, messageId: Long? = null) {
        val lang = TtsLanguage.fromCode(prefs.ttsLanguage)
        textToSpeech.setLanguage(lang.locale)
        _state.value = _state.value.copy(isSpeaking = true, speakingMessageId = messageId)
        textToSpeech.speak(stripMarkdown(content))
    }

    fun stopSpeaking() {
        textToSpeech.stop()
        _state.value = _state.value.copy(isSpeaking = false, speakingMessageId = null)
    }

    fun toggleAutoSpeak() {
        _state.value = _state.value.copy(isAutoSpeak = !_state.value.isAutoSpeak)
    }

    fun onPermissionDenied() {
        _state.value = _state.value.copy(
            error = "Microphone permission is required to use voice input"
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refreshApiKeyState() {
        _state.value = _state.value.copy(hasApiKey = prefs.apiKey.isNotBlank())
    }

    fun clearChat() {
        textToSpeech.stop()
        _state.value = ChatUiState(
            hasApiKey = prefs.apiKey.isNotBlank(),
            conversations = _state.value.conversations
        )
        messageIdCounter = 0L
    }

    fun startNewConversation() {
        saveIfHasMessages()
        textToSpeech.stop()
        _state.value = _state.value.copy(
            messages = emptyList(),
            inputText = "",
            isLoading = false,
            isSpeaking = false,
            speakingMessageId = null,
            error = null,
            currentConversationId = null
        )
        messageIdCounter = 0L
    }

    fun deleteConversation(id: String) {
        conversationManager.deleteConversation(id)
        if (_state.value.currentConversationId == id) {
            _state.value = _state.value.copy(
                currentConversationId = null,
                messages = emptyList()
            )
            messageIdCounter = 0L
        }
        _state.value = _state.value.copy(
            conversations = conversationManager.listConversations()
        )
    }

    fun loadConversation(id: String) {
        saveIfHasMessages()
        val conversation = conversationManager.loadConversation(id) ?: return
        textToSpeech.stop()
        messageIdCounter = 0L
        val messages = conversation.messages.filter { it.content.isNotBlank() }.map {
            ChatUiMessage(id = messageIdCounter++, role = it.role, content = it.content)
        }
        _state.value = _state.value.copy(
            messages = messages,
            inputText = "",
            isLoading = false,
            isSpeaking = false,
            speakingMessageId = null,
            error = null,
            currentConversationId = id
        )
    }

    private fun saveCurrentConversation() {
        val messages = _state.value.messages
            .filter { !it.isLoading && it.content.isNotBlank() }
        if (messages.isEmpty()) return

        val id = _state.value.currentConversationId
            ?: return

        conversationManager.saveConversation(
            id = id,
            messages = messages.map { StoredMessage(role = it.role, content = it.content) },
            model = prefs.selectedModel
        )
        _state.value = _state.value.copy(
            conversations = conversationManager.listConversations()
        )
    }

    private fun saveIfHasMessages() {
        val hasMessages = _state.value.messages.any { !it.isLoading && it.content.isNotBlank() }
        if (!hasMessages) return
        val id = _state.value.currentConversationId
            ?: "${System.currentTimeMillis()}"
        _state.value = _state.value.copy(currentConversationId = id)
        saveCurrentConversation()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        textToSpeech.destroy()
    }
}
