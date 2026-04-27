package com.ollamachat.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerManager(context: Context) {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    var onResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onListeningChange: ((Boolean) -> Unit)? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onListeningChange?.invoke(true)
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            onListeningChange?.invoke(false)
        }

        override fun onError(error: Int) {
            onListeningChange?.invoke(false)
            val message = when (error) {
                SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
                SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
                SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi o que foi dito"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo de fala esgotado"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone não concedida"
                else -> "Erro no reconhecimento de voz ($error)"
            }
            onError?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onResult?.invoke(text)
            } else {
                onError?.invoke("Não foi possível reconhecer a fala")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit

        @Deprecated("Deprecated in Java", ReplaceWith(""))
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    init {
        speechRecognizer.setRecognitionListener(listener)
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua mensagem")
        }
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
