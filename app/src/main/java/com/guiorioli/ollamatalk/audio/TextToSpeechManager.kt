package com.guiorioli.ollamatalk.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var currentLocale: Locale = Locale("en", "US")

    var onDone: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            pendingText?.let { doSpeak(it) }
            pendingText = null
        } else {
            isInitialized = false
            onError?.invoke("Error initializing speech synthesizer")
        }
    }

    init {
        tts = TextToSpeech(context, initListener)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) = Unit
            override fun onDone(uttId: String?) {
                onDone?.invoke()
            }
            override fun onError(uttId: String?) {
                onError?.invoke("Error playing audio")
            }
        })
    }

    fun setLanguage(locale: Locale): Boolean {
        currentLocale = locale
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onError?.invoke("No text to read")
            return
        }
        if (!isInitialized) {
            pendingText = trimmed
            return
        }
        pendingText = null
        doSpeak(trimmed)
    }

    private fun doSpeak(text: String) {
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result != TextToSpeech.SUCCESS) {
            onError?.invoke("Could not start speech")
        }
    }

    fun stop() {
        tts?.stop()
        pendingText = null
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val UTTERANCE_ID = "ollama_tts_utterance"
    }
}
