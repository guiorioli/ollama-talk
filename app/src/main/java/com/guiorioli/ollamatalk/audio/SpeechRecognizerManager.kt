package com.guiorioli.ollamatalk.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.guiorioli.ollamatalk.R

class SpeechRecognizerManager(private val context: Context) {

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
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            onListeningChange?.invoke(false)
            val message = when (error) {
                SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.error_network)
                SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.error_audio)
                SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.error_no_match)
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> context.getString(R.string.error_speech_timeout)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.error_mic_not_granted)
                else -> context.getString(R.string.error_speech_recognition, error)
            }
            onError?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onResult?.invoke(text)
            } else {
                onError?.invoke(context.getString(R.string.error_no_match_speech))
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit

        @Deprecated("Deprecated in Java", ReplaceWith(""))
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    init {
        speechRecognizer.setRecognitionListener(listener)
    }

    fun startListening(languageCode: String = "en-US") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
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
