package com.ollamachat.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ollamachat.audio.SpeechRecognizerManager
import com.ollamachat.audio.TextToSpeechManager
import com.ollamachat.data.api.ChatMessage
import com.ollamachat.data.api.OllamaApiService
import com.ollamachat.data.local.PreferencesManager
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
    val isAutoSpeak: Boolean = false,
    val error: String? = null,
    val hasApiKey: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val apiService = OllamaApiService()
    val speechRecognizer = SpeechRecognizerManager(application)
    val textToSpeech = TextToSpeechManager(application)

    private val _state = MutableStateFlow(ChatUiState(hasApiKey = prefs.apiKey.isNotBlank()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0L

    init {
        setupSpeechRecognizer()
        setupTextToSpeech()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.onResult = { text ->
            _state.value = _state.value.copy(inputText = text, isListening = false)
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
            _state.value = _state.value.copy(isSpeaking = false)
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

        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            _state.value = _state.value.copy(error = "Configure sua API Key nas Configurações")
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
                        speakMessage(content)
                    }
                },
                onFailure = { error ->
                    val updatedMessages = _state.value.messages.filter { !it.isLoading }
                    _state.value = _state.value.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        error = error.message ?: "Erro ao enviar mensagem"
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

    fun speakMessage(content: String) {
        _state.value = _state.value.copy(isSpeaking = true)
        textToSpeech.speak(content)
    }

    fun stopSpeaking() {
        textToSpeech.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun toggleAutoSpeak() {
        _state.value = _state.value.copy(isAutoSpeak = !_state.value.isAutoSpeak)
    }

    fun onPermissionDenied() {
        _state.value = _state.value.copy(
            error = "Permissão do microfone necessária para usar entrada de voz"
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearChat() {
        textToSpeech.stop()
        _state.value = ChatUiState(hasApiKey = prefs.apiKey.isNotBlank())
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        textToSpeech.destroy()
    }
}
