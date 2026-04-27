package com.ollamachat.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ollamachat.data.api.ModelInfo
import com.ollamachat.data.api.OllamaApiService
import com.ollamachat.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val apiKey: String = "",
    val selectedModel: String = PreferencesManager.DEFAULT_MODEL,
    val availableModels: List<ModelInfo> = emptyList(),
    val isLoadingModels: Boolean = false,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val apiService = OllamaApiService()

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            apiKey = prefs.apiKey,
            selectedModel = prefs.selectedModel
        )
    }

    fun onApiKeyChanged(key: String) {
        _state.value = _state.value.copy(apiKey = key, error = null, successMessage = null)
    }

    fun onModelSelected(model: String) {
        _state.value = _state.value.copy(selectedModel = model)
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
}
