package com.guiorioli.ollamatalk.audio

import android.speech.tts.TextToSpeech

/**
 * Gerencia TTS progressivo durante streaming de texto.
 * Acumula chunks em frases completas e as enfileira no TTS usando QUEUE_ADD,
 * deixando o Android gerenciar a fila internamente.
 */
class StreamingTtsManager(private val ttsManager: TextToSpeechManager) {

    private val sentenceBuffer = StringBuilder()
    private var isActive = false

    var onStart: (() -> Unit)? = null
    var onDone: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null

    /**
     * Inicia uma nova sessão de streaming TTS.
     * Limpa buffers e estado anterior.
     */
    fun start() {
        isActive = true
        sentenceBuffer.clear()
        onStart?.invoke()
    }

    /**
     * Adiciona texto ao buffer.
     * Sempre que frases completas são detectadas, elas são imediatamente
     * enfileiradas no TTS via QUEUE_ADD.
     */
    fun append(text: String) {
        if (!isActive) return
        sentenceBuffer.append(text)
        drainSentences()
    }

    /**
     * Sinaliza fim do stream.
     * Qualquer texto remanescente no buffer é tratado como uma frase final
     * e enfileirado no TTS.
     */
    fun finish() {
        if (!isActive) return
        val remainder = sentenceBuffer.toString().trim()
        if (remainder.isNotEmpty()) {
            ttsManager.speak(remainder, TextToSpeech.QUEUE_ADD)
        }
        sentenceBuffer.clear()
        onDone?.invoke()
    }

    /**
     * Para imediatamente todo o TTS e limpa buffers.
     * A fila do Android TTS é limpa via stop().
     */
    fun stop() {
        isActive = false
        sentenceBuffer.clear()
        ttsManager.stop()
        onStop?.invoke()
    }

    /**
     * Retorna se o streaming está ativo.
     */
    fun isActive(): Boolean = isActive

    /**
     * Verifica se há texto pendente no buffer mas ainda não enfileirado.
     */
    fun hasPendingBuffer(): Boolean = sentenceBuffer.isNotEmpty()

    private fun drainSentences() {
        val text = sentenceBuffer.toString()
        val sentenceEndRegex = Regex("([.!?\n]+)")
        val matches = sentenceEndRegex.findAll(text).toList()

        var lastCut = 0
        for (match in matches) {
            val endIndex = match.range.last + 1
            val sentence = text.substring(lastCut, endIndex).trim()
            if (sentence.isNotEmpty()) {
                ttsManager.speak(sentence, TextToSpeech.QUEUE_ADD)
            }
            lastCut = endIndex
        }

        sentenceBuffer.clear()
        if (lastCut < text.length) {
            sentenceBuffer.append(text.substring(lastCut))
        }
    }
}
