package com.guiorioli.ollamatalk.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guiorioli.ollamatalk.data.api.ModelInfo
import com.guiorioli.ollamatalk.data.api.OllamaApiService
import com.guiorioli.ollamatalk.data.local.PreferencesManager
import com.guiorioli.ollamatalk.data.local.TtsLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val apiKey: String = "",
    val selectedModel: String = PreferencesManager.DEFAULT_MODEL,
    val ttsLanguage: String = TtsLanguage.DEFAULT.code,
    val ttsLanguages: List<TtsLanguage> = TtsLanguage.ALL,
    val availableModels: List<ModelInfo> = emptyList(),
    val isLoadingModels: Boolean = false,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val webSearchEnabled: Boolean = false,
    val isCheckingToolSupport: Boolean = false,
    val modelToolSupportStatus: ToolSupportStatus = ToolSupportStatus.UNKNOWN,
    val showCompatibilityDialog: Boolean = false
)

enum class ToolSupportStatus {
    SUPPORTED,      // Model is in known list or verified by scraping
    NOT_SUPPORTED,  // Scraping checked and model not found
    UNKNOWN         // Not checked yet
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val apiService = OllamaApiService()

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val model = prefs.selectedModel
        val knownSupport = OllamaApiService.KNOWN_TOOLS_MODELS.contains(model)
        _state.value = _state.value.copy(
            apiKey = prefs.apiKey,
            selectedModel = model,
            ttsLanguage = prefs.ttsLanguage,
            webSearchEnabled = prefs.webSearchEnabled,
            modelToolSupportStatus = if (knownSupport) ToolSupportStatus.SUPPORTED else ToolSupportStatus.UNKNOWN
        )
    }

    fun onApiKeyChanged(key: String) {
        _state.value = _state.value.copy(apiKey = key, error = null, successMessage = null)
    }

    fun onModelSelected(model: String) {
        val knownSupport = OllamaApiService.KNOWN_TOOLS_MODELS.contains(model)
        val cachedSupport = prefs.isModelVerified(model)
        val supportStatus = when {
            knownSupport || cachedSupport -> ToolSupportStatus.SUPPORTED
            else -> ToolSupportStatus.UNKNOWN
        }
        // If model changes to unverified, disable web search
        val shouldDisableWebSearch = !knownSupport && !cachedSupport && _state.value.webSearchEnabled
        _state.value = _state.value.copy(
            selectedModel = model,
            modelToolSupportStatus = supportStatus,
            webSearchEnabled = if (shouldDisableWebSearch) false else _state.value.webSearchEnabled
        )
        if (shouldDisableWebSearch) {
            prefs.webSearchEnabled = false
        }
    }

    fun toggleWebSearch() {
        val currentEnabled = _state.value.webSearchEnabled
        if (currentEnabled) {
            // Disable — no questions asked
            prefs.webSearchEnabled = false
            _state.value = _state.value.copy(webSearchEnabled = false)
            return
        }

        // Trying to enable
        val model = _state.value.selectedModel
        val knownSupport = OllamaApiService.KNOWN_TOOLS_MODELS.contains(model)
        val cachedSupport = prefs.isModelVerified(model)

        if (knownSupport || cachedSupport) {
            // Model is known/cached as supporting tools — enable directly
            prefs.webSearchEnabled = true
            _state.value = _state.value.copy(webSearchEnabled = true, modelToolSupportStatus = ToolSupportStatus.SUPPORTED)
        } else {
            // Model not verified — need to check
            _state.value = _state.value.copy(isCheckingToolSupport = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    apiService.checkModelSupportsTools(model)
                }
                _state.value = _state.value.copy(isCheckingToolSupport = false)

                result.fold(
                    onSuccess = { isSupported ->
                        if (isSupported) {
                            prefs.addVerifiedModel(model)
                            prefs.webSearchEnabled = true
                            _state.value = _state.value.copy(
                                webSearchEnabled = true,
                                modelToolSupportStatus = ToolSupportStatus.SUPPORTED,
                                showCompatibilityDialog = false
                            )
                        } else {
                            _state.value = _state.value.copy(
                                modelToolSupportStatus = ToolSupportStatus.NOT_SUPPORTED,
                                showCompatibilityDialog = true
                            )
                        }
                    },
                    onFailure = {
                        _state.value = _state.value.copy(
                            modelToolSupportStatus = ToolSupportStatus.NOT_SUPPORTED,
                            showCompatibilityDialog = true
                        )
                    }
                )
            }
        }
    }

    fun confirmEnableWebSearch() {
        prefs.webSearchEnabled = true
        _state.value = _state.value.copy(webSearchEnabled = true, showCompatibilityDialog = false)
    }

    fun dismissCompatibilityDialog() {
        _state.value = _state.value.copy(showCompatibilityDialog = false)
    }

    fun onTtsLanguageChanged(code: String) {
        _state.value = _state.value.copy(ttsLanguage = code)
    }

    fun loadModels() {
        val apiKey = _state.value.apiKey
        if (apiKey.isBlank()) return

        _state.value = _state.value.copy(isLoadingModels = true, error = null)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                apiService.listModels(apiKey)
            }

            result.fold(
                onSuccess = { models ->
                    _state.value = _state.value.copy(
                        availableModels = models,
                        isLoadingModels = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoadingModels = false,
                        error = "Error loading models: ${error.message}"
                    )
                }
            )
        }
    }

    fun save() {
        val apiKey = _state.value.apiKey.trim()
        if (apiKey.isBlank()) {
            _state.value = _state.value.copy(error = "Enter an API Key")
            return
        }

        _state.value = _state.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                apiService.validateApiKey(apiKey)
            }

            if (isValid) {
                prefs.apiKey = apiKey
                prefs.selectedModel = _state.value.selectedModel
                prefs.ttsLanguage = _state.value.ttsLanguage
                prefs.webSearchEnabled = _state.value.webSearchEnabled
                _state.value = _state.value.copy(
                    isSaving = false,
                    successMessage = "Settings saved",
                    isSaved = true
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Invalid API Key. Check and try again."
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }

    fun onNavigatedAway() {
        _state.value = _state.value.copy(isSaved = false)
    }
}
