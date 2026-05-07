package com.guiorioli.ollamatalk.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guiorioli.ollamatalk.audio.SpeechRecognizerManager
import com.guiorioli.ollamatalk.audio.TextToSpeechManager
import com.guiorioli.ollamatalk.data.api.ChatMessage
import com.guiorioli.ollamatalk.data.api.OllamaApiService
import com.guiorioli.ollamatalk.data.local.ConversationIndexEntry
import com.guiorioli.ollamatalk.data.local.ConversationManager
import com.guiorioli.ollamatalk.data.local.PreferencesManager
import com.guiorioli.ollamatalk.data.local.StoredMessage
import com.guiorioli.ollamatalk.data.local.TtsLanguage
import com.guiorioli.ollamatalk.util.ImageUtils
import com.guiorioli.ollamatalk.util.formatConversationText
import com.guiorioli.ollamatalk.util.stripMarkdown
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
    val isLoading: Boolean = false,
    val hasImage: Boolean = false
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
    val currentConversationId: String? = null,
    val pendingImageUri: String? = null,
    val selectedModel: String = ""
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
            conversations = conversationManager.listConversations(),
            selectedModel = prefs.selectedModel
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

    fun onImagePicked(uri: String) {
        _state.value = _state.value.copy(pendingImageUri = uri)
    }

    fun clearPendingImage() {
        _state.value = _state.value.copy(pendingImageUri = null)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        val imageUri = _state.value.pendingImageUri
        if (text.isBlank() && imageUri == null) return

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
            content = text,
            hasImage = imageUri != null
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
            pendingImageUri = null,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val base64Image = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    val uri = android.net.Uri.parse(imageUri)
                    val app = getApplication<Application>()
                    ImageUtils.compressAndEncode(app, uri)
                }
            } else null

            val chatMessages = _state.value.messages
                .filter { !it.isLoading }
                .map { msg -> ChatMessage(role = msg.role, content = msg.content) }
                .toMutableList()

            if (base64Image != null && chatMessages.isNotEmpty()) {
                val lastIdx = chatMessages.size - 1
                chatMessages[lastIdx] = chatMessages[lastIdx].copy(images = listOf(base64Image))
            }

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
        val lang = TtsLanguage.fromCode(prefs.ttsLanguage)
        speechRecognizer.startListening(lang.code)
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
        val languageOk = textToSpeech.setLanguage(lang.locale)
        if (!languageOk) {
            _state.value = _state.value.copy(error = "TTS language data not installed for ${lang.displayName}")
            return
        }
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

    fun refreshSettingsState() {
        _state.value = _state.value.copy(
            hasApiKey = prefs.apiKey.isNotBlank(),
            selectedModel = prefs.selectedModel
        )
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
        textToSpeech.stop()
        viewModelScope.launch(Dispatchers.Main.immediate) { saveCurrentConversation() }
        _state.value = _state.value.copy(
            messages = emptyList(),
            inputText = "",
            isLoading = false,
            isSpeaking = false,
            speakingMessageId = null,
            error = null,
            currentConversationId = null,
            pendingImageUri = null
        )
        messageIdCounter = 0L
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { conversationManager.deleteConversation(id) }
            if (_state.value.currentConversationId == id) {
                _state.value = _state.value.copy(
                    currentConversationId = null,
                    messages = emptyList()
                )
                messageIdCounter = 0L
            }
            _state.value = _state.value.copy(
                conversations = withContext(Dispatchers.IO) {
                    conversationManager.listConversations()
                }
            )
        }
    }

    fun loadConversation(id: String) {
        viewModelScope.launch {
            saveIfHasMessages()
            val conversation = withContext(Dispatchers.IO) {
                conversationManager.loadConversation(id)
            } ?: return@launch
            textToSpeech.stop()
            messageIdCounter = 0L
            val messages = conversation.messages.filter { it.content.isNotBlank() }.map { stored ->
                val hasImage = stored.content.startsWith("[Image]")
                val cleanContent = if (hasImage) {
                    stored.content.removePrefix("[Image] ").removePrefix("[Image]")
                } else stored.content
                ChatUiMessage(id = messageIdCounter++, role = stored.role, content = cleanContent, hasImage = hasImage)
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
    }

    private suspend fun saveCurrentConversation() {
        val msgs = _state.value.messages
            .filter { !it.isLoading && (it.content.isNotBlank() || it.hasImage) }
        if (msgs.isEmpty()) return

        val id = _state.value.currentConversationId ?: return

        withContext(Dispatchers.IO) {
            conversationManager.saveConversation(
                id = id,
                messages = msgs.map {
                    val storedContent = if (it.hasImage) {
                        if (it.content.isBlank()) "[Image]" else "[Image] ${it.content}"
                    } else it.content
                    StoredMessage(role = it.role, content = storedContent)
                },
                model = prefs.selectedModel
            )
        }
        _state.value = _state.value.copy(
            conversations = withContext(Dispatchers.IO) {
                conversationManager.listConversations()
            }
        )
    }

    private suspend fun saveIfHasMessages() {
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
