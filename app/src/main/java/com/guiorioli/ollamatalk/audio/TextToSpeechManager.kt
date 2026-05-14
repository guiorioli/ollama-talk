package com.guiorioli.ollamatalk.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var pendingQueueMode: Int = TextToSpeech.QUEUE_FLUSH
    private var currentLocale: Locale = Locale("en", "US")
    private var utteranceCounter = 0L
    private var pendingUtterances = 0

    var onDone: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onQueueEmpty: (() -> Unit)? = null

    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            pendingText?.let { doSpeak(it, pendingQueueMode) }
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
                if (uttId != null) {
                    onDone?.invoke(uttId)
                }
                pendingUtterances--
                if (pendingUtterances <= 0) {
                    pendingUtterances = 0
                    onQueueEmpty?.invoke()
                }
            }
            override fun onError(uttId: String?) {
                if (uttId != null) {
                    onError?.invoke(uttId)
                }
                pendingUtterances--
                if (pendingUtterances <= 0) {
                    pendingUtterances = 0
                    onQueueEmpty?.invoke()
                }
            }
        })
    }

    fun setLanguage(locale: Locale): Boolean {
        currentLocale = locale
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onError?.invoke("No text to read")
            return
        }
        if (!isInitialized) {
            pendingText = trimmed
            pendingQueueMode = queueMode
            return
        }
        pendingText = null
        doSpeak(trimmed, queueMode)
    }

    private fun doSpeak(text: String, queueMode: Int) {
        val utteranceId = "${UTTERANCE_PREFIX}_${++utteranceCounter}"
        val result = tts?.speak(text, queueMode, null, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            pendingUtterances++
        } else {
            onError?.invoke(utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
        pendingText = null
        pendingUtterances = 0
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val UTTERANCE_PREFIX = "ollama_tts_utterance"
    }
}
