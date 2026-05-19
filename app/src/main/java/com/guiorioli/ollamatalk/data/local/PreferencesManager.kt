package com.guiorioli.ollamatalk.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var ttsLanguage: String
        get() = prefs.getString(KEY_TTS_LANG, TtsLanguage.DEFAULT.code) ?: TtsLanguage.DEFAULT.code
        set(value) = prefs.edit().putString(KEY_TTS_LANG, value).apply()

    var webSearchEnabled: Boolean
        get() = prefs.getBoolean(KEY_WEB_SEARCH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WEB_SEARCH_ENABLED, value).apply()

    // Cache of models verified via scraping with timestamp
    // TTL of 7 days is enforced when reading
    private var verifiedModelsRaw: String
        get() = prefs.getString(KEY_VERIFIED_MODELS_CACHE, null) ?: ""
        set(value) = prefs.edit().putString(KEY_VERIFIED_MODELS_CACHE, value).apply()

    fun addVerifiedModel(modelName: String) {
        val cache = getVerifiedModelsMap().toMutableMap()
        cache[modelName] = System.currentTimeMillis()
        verifiedModelsRaw = com.google.gson.Gson().toJson(cache)
    }

    fun isModelVerified(modelName: String): Boolean {
        val cache = getVerifiedModelsMap()
        val timestamp = cache[modelName] ?: return false
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - timestamp) < sevenDays
    }

    private fun getVerifiedModelsMap(): Map<String, Long> {
        val json = verifiedModelsRaw
        if (json.isBlank()) return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Long>>() {}.type
            com.google.gson.Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "ollama_talk_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_TTS_LANG = "tts_language"
        private const val KEY_WEB_SEARCH_ENABLED = "web_search_enabled"
        private const val KEY_VERIFIED_MODELS_CACHE = "verified_models_cache"
        const val DEFAULT_MODEL = "gemma3:27b-cloud"
    }
}
