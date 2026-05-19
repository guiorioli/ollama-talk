package com.guiorioli.ollamatalk.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guiorioli.ollamatalk.R
import com.guiorioli.ollamatalk.audio.SpeechRecognizerManager
import com.guiorioli.ollamatalk.audio.StreamingTtsManager
import com.guiorioli.ollamatalk.audio.TextToSpeechManager
import com.guiorioli.ollamatalk.data.api.ChatMessage
import com.guiorioli.ollamatalk.data.api.OllamaApiService
import com.guiorioli.ollamatalk.data.api.ToolCall
import com.guiorioli.ollamatalk.data.api.WebSearchResponse
import com.guiorioli.ollamatalk.data.local.ConversationIndexEntry
import com.guiorioli.ollamatalk.data.local.ConversationManager
import com.guiorioli.ollamatalk.data.local.PreferencesManager
import com.guiorioli.ollamatalk.data.local.StoredMessage
import com.guiorioli.ollamatalk.data.local.StoredToolCall
import com.guiorioli.ollamatalk.data.local.TtsLanguage
import com.guiorioli.ollamatalk.util.ImageUtils
import com.guiorioli.ollamatalk.util.formatConversationText
import com.guiorioli.ollamatalk.util.stripMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class ChatUiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val isLoading: Boolean = false,
    val hasImage: Boolean = false,
    val toolCalls: List<com.guiorioli.ollamatalk.data.api.ToolCall>? = null
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
    val selectedModel: String = "",
    val isWebSearching: Boolean = false,
    val webSearchEnabled: Boolean = false
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
            selectedModel = prefs.selectedModel,
            webSearchEnabled = prefs.webSearchEnabled
        )
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messageIdCounter = 0L
    private var currentChatJob: Job? = null
    private val streamingTts = StreamingTtsManager(textToSpeech)

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
        textToSpeech.onDone = { updateSpeakingState() }
        textToSpeech.onError = { updateSpeakingState() }
        textToSpeech.onQueueEmpty = { updateSpeakingState() }
        streamingTts.onStart = { updateSpeakingState() }
        streamingTts.onStop = { updateSpeakingState() }
        streamingTts.onDone = { updateSpeakingState() }
    }

    private fun updateSpeakingState() {
        val isSpeaking = streamingTts.isActive() || textToSpeech.isSpeaking()
        _state.value = _state.value.copy(
            isSpeaking = isSpeaking,
            speakingMessageId = if (isSpeaking) _state.value.speakingMessageId else null
        )
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
            _state.value = _state.value.copy(error = getApplication<Application>().getString(R.string.error_no_api_key))
            return
        }

        val userMessage = ChatUiMessage(
            id = messageIdCounter++,
            role = "user",
            content = text,
            hasImage = imageUri != null
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            inputText = "",
            pendingImageUri = null,
            error = null
        )

        if (_state.value.webSearchEnabled) {
            processChatWithTools(text, imageUri, apiKey)
        } else {
            processChatNormal(text, imageUri, apiKey)
        }
    }

    private fun processChatNormal(_text: String, imageUri: String?, apiKey: String) {
        val loadingMessage = ChatUiMessage(
            id = messageIdCounter++,
            role = "assistant",
            content = "",
            isLoading = true
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + loadingMessage,
            isLoading = true
        )

        currentChatJob = viewModelScope.launch {
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

            if (_state.value.isAutoSpeak) {
                streamingTts.start()
            }

            try {
                apiService.chatAsFlow(prefs.selectedModel, chatMessages, apiKey)
                    .flowOn(Dispatchers.IO)
                    .onCompletion { error ->
                        apiService.cancelChat()
                        currentChatJob = null
                    }
                    .collect { chunk ->
                        if (!isActive) return@collect
                        val updatedMessages = _state.value.messages.map { msg ->
                            if (msg.isLoading) {
                                msg.copy(content = msg.content + chunk)
                            } else msg
                        }
                        _state.value = _state.value.copy(messages = updatedMessages)
                        if (_state.value.isAutoSpeak) {
                            streamingTts.append(stripMarkdown(chunk))
                        }
                    }

                val finalMessages = _state.value.messages.map { msg ->
                    if (msg.isLoading) msg.copy(isLoading = false) else msg
                }
                _state.value = _state.value.copy(
                    messages = finalMessages,
                    isLoading = false
                )
                if (_state.value.isAutoSpeak) {
                    streamingTts.finish()
                }
                saveCurrentConversation()
            } catch (e: Exception) {
                if (!isActive) return@launch
                streamingTts.stop()
                val updatedMessages = _state.value.messages.filter { !it.isLoading }
                _state.value = _state.value.copy(
                    messages = updatedMessages,
                    isLoading = false,
                    error = e.message ?: getApplication<Application>().getString(R.string.error_sending_message)
                )
                currentChatJob = null
            }
        }
    }

    private fun processChatWithTools(_text: String, imageUri: String?, apiKey: String) {
        currentChatJob = viewModelScope.launch {
            val base64Image = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    val uri = android.net.Uri.parse(imageUri)
                    val app = getApplication<Application>()
                    ImageUtils.compressAndEncode(app, uri)
                }
            } else null

            // Build conversation history for API
            val apiMessages = buildApiMessages(base64Image)

            _state.value = _state.value.copy(isLoading = true)

            val maxToolIterations = 3
            var iterations = 0
            var success = false

            try {
                while (iterations < maxToolIterations) {
                    iterations++

                    // Show loading bubble while waiting for model response
                    val loadingId = messageIdCounter++
                    val loadingMessage = ChatUiMessage(
                        id = loadingId,
                        role = "assistant",
                        content = "",
                        isLoading = true
                    )
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + loadingMessage
                    )

                    val response = withContext(Dispatchers.IO) {
                        apiService.chat(
                            model = prefs.selectedModel,
                            messages = apiMessages,
                            apiKey = apiKey,
                            tools = listOf(OllamaApiService.WEB_SEARCH_TOOL)
                        )
                    }

                    if (response.isFailure) {
                        throw response.exceptionOrNull() ?: IOException(getApplication<Application>().getString(R.string.error_unknown))
                    }

                    val chatResponse = response.getOrThrow()
                    val assistantMessage = chatResponse.message

                    // Check if model called a tool
                    if (assistantMessage.tool_calls.isNullOrEmpty()) {
                        // Final response — no tool calls
                        val assistantUiMessage = ChatUiMessage(
                            id = loadingId,
                            role = "assistant",
                            content = assistantMessage.content ?: "",
                            isLoading = false
                        )
                        val updatedMessages = _state.value.messages.map { msg ->
                            if (msg.id == loadingId) assistantUiMessage else msg
                        }
                        _state.value = _state.value.copy(
                            messages = updatedMessages,
                            isLoading = false,
                            isWebSearching = false
                        )
                        success = true
                        break
                    }

                    // Model called a tool — add tool_calls message to history
                    apiMessages.add(
                        ChatMessage(
                            role = "assistant",
                            content = assistantMessage.content ?: "",
                            tool_calls = assistantMessage.tool_calls
                        )
                    )

                    // Remove loading bubble and add tool call UI indicator
                    val messagesWithoutLoading = _state.value.messages.filter { it.id != loadingId }
                    val toolCall = assistantMessage.tool_calls.first()
                    val query = toolCall.function.arguments["query"] as? String ?: ""
                    val toolUiMessage = ChatUiMessage(
                        id = messageIdCounter++,
                        role = "tool",
                        content = "🔍 Searched: '$query'",
                        isLoading = false
                    )
                    _state.value = _state.value.copy(
                        messages = messagesWithoutLoading + toolUiMessage,
                        isWebSearching = true
                    )

                    // Execute each tool call
                    for (tc in assistantMessage.tool_calls) {
                        when (tc.function.name) {
                            "web_search" -> {
                                val searchQuery = tc.function.arguments["query"] as? String ?: ""
                                val maxResults = (tc.function.arguments["max_results"] as? Number)?.toInt() ?: 5

                                val searchResult = withContext(Dispatchers.IO) {
                                    apiService.webSearch(searchQuery, maxResults, apiKey)
                                }

                                val toolResultContent = if (searchResult.isSuccess) {
                                    formatSearchResults(searchResult.getOrThrow())
                                } else {
                                    getApplication<Application>().getString(R.string.error_sending_message) + ": ${searchResult.exceptionOrNull()?.message}"
                                }

                                apiMessages.add(
                                    ChatMessage(
                                        role = "tool",
                                        content = toolResultContent,
                                        tool_name = "web_search"
                                    )
                                )
                            }
                        }
                    }

                    _state.value = _state.value.copy(isWebSearching = false)
                }

                if (!success && iterations >= maxToolIterations) {
                    // Max iterations reached without final response
                    val errorMsg = ChatUiMessage(
                        id = messageIdCounter++,
                        role = "assistant",
                        content = getApplication<Application>().getString(R.string.error_search_failed),
                        isLoading = false
                    )
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + errorMsg,
                        isLoading = false,
                        isWebSearching = false
                    )
                }

                saveCurrentConversation()
            } catch (e: Exception) {
                if (!isActive) return@launch
                Log.e("ChatViewModel", "Tool chat error", e)
                val updatedMessages = _state.value.messages.filter { !it.isLoading }
                val errorDetail = if (e is IOException) {
                    e.message ?: getApplication<Application>().getString(R.string.error_sending_message)
                } else {
                    "${e.javaClass.simpleName}: ${e.message ?: getApplication<Application>().getString(R.string.error_sending_message)}"
                }
                _state.value = _state.value.copy(
                    messages = updatedMessages,
                    isLoading = false,
                    isWebSearching = false,
                    error = errorDetail
                )
                currentChatJob = null
            }
        }
    }

    private fun buildApiMessages(base64Image: String?): MutableList<ChatMessage> {
        val messages = _state.value.messages
            .filter { it.role != "tool" }
            .map { msg -> ChatMessage(role = msg.role, content = msg.content) }
            .toMutableList()

        if (base64Image != null && messages.isNotEmpty()) {
            val lastIdx = messages.size - 1
            if (messages[lastIdx].role == "user") {
                messages[lastIdx] = messages[lastIdx].copy(images = listOf(base64Image))
            }
        }

        return messages
    }

    private fun formatSearchResults(response: WebSearchResponse): String {
        val results = response.results
        if (results.isNullOrEmpty()) return "No results found."
        return results.joinToString("\n\n") { result ->
            "[${result.title}](${result.url})\n${result.content}"
        }
    }

    fun cancelMessage() {
        currentChatJob?.cancel()
        currentChatJob = null
        apiService.cancelAllCalls()
        streamingTts.stop()
        val updatedMessages = _state.value.messages.filter { !it.isLoading }
        _state.value = _state.value.copy(
            messages = updatedMessages,
            isLoading = false,
            error = null
        )
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
            _state.value = _state.value.copy(error = getApplication<Application>().getString(R.string.error_tts_not_installed, lang.displayName))
            return
        }
        _state.value = _state.value.copy(isSpeaking = true, speakingMessageId = messageId)
        textToSpeech.speak(stripMarkdown(content))
    }

    fun stopSpeaking() {
        streamingTts.stop()
        _state.value = _state.value.copy(isSpeaking = false, speakingMessageId = null)
    }

    fun toggleAutoSpeak() {
        _state.value = _state.value.copy(isAutoSpeak = !_state.value.isAutoSpeak)
    }

    fun onPermissionDenied() {
        _state.value = _state.value.copy(
            error = getApplication<Application>().getString(R.string.error_mic_permission)
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refreshSettingsState() {
        _state.value = _state.value.copy(
            hasApiKey = prefs.apiKey.isNotBlank(),
            selectedModel = prefs.selectedModel,
            webSearchEnabled = prefs.webSearchEnabled
        )
    }

    fun clearChat() {
        streamingTts.stop()
        textToSpeech.stop()
        _state.value = ChatUiState(
            hasApiKey = prefs.apiKey.isNotBlank(),
            conversations = _state.value.conversations
        )
        messageIdCounter = 0L
    }

    fun startNewConversation() {
        streamingTts.stop()
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
                val uiToolCalls = stored.tool_calls?.map { tc ->
                    ToolCall(
                        function = com.guiorioli.ollamatalk.data.api.ToolCallFunction(
                            name = tc.name,
                            arguments = tc.arguments
                        )
                    )
                }
                ChatUiMessage(
                    id = messageIdCounter++,
                    role = stored.role,
                    content = cleanContent,
                    hasImage = hasImage,
                    toolCalls = uiToolCalls
                )
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
                    val storedToolCalls = it.toolCalls?.map { tc ->
                        StoredToolCall(
                            name = tc.function.name,
                            arguments = tc.function.arguments
                        )
                    }
                    StoredMessage(
                        role = it.role,
                        content = storedContent,
                        tool_calls = storedToolCalls
                    )
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
