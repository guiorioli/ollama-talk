package com.ollamachat.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var onDone: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "US")
            isInitialized = true
        } else {
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

    fun speak(text: String) {
        if (!isInitialized) {
            onError?.invoke("Speech synthesizer not ready")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val UTTERANCE_ID = "ollama_tts_utterance"
    }
}
