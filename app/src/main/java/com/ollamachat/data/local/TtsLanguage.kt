package com.ollamachat.data.local

import java.util.Locale

data class TtsLanguage(
    val code: String,
    val displayName: String,
    val locale: Locale
) {
    companion object {
        val EN_US = TtsLanguage("en-US", "English (US)", Locale("en", "US"))
        val PT_BR = TtsLanguage("pt-BR", "Português (Brasil)", Locale("pt", "BR"))
        val ES_ES = TtsLanguage("es-ES", "Español", Locale("es", "ES"))
        val FR_FR = TtsLanguage("fr-FR", "Français", Locale("fr", "FR"))
        val DE_DE = TtsLanguage("de-DE", "Deutsch", Locale("de", "DE"))

        val DEFAULT = EN_US

        val ALL = listOf(
            EN_US,
            PT_BR,
            ES_ES,
            FR_FR,
            DE_DE
        )

        fun fromCode(code: String): TtsLanguage =
            ALL.firstOrNull { it.code == code } ?: DEFAULT
    }
}
