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

    companion object {
        private const val PREFS_NAME = "ollama_talk_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_TTS_LANG = "tts_language"
        const val DEFAULT_MODEL = "gemma3:27b-cloud"
    }
}
