package com.guiorioli.ollamatalk.data.local

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
        val IT_IT = TtsLanguage("it-IT", "Italiano", Locale("it", "IT"))
        val NL_NL = TtsLanguage("nl-NL", "Nederlands", Locale("nl", "NL"))
        val JA_JP = TtsLanguage("ja-JP", "日本語", Locale("ja", "JP"))
        val KO_KR = TtsLanguage("ko-KR", "한국어", Locale("ko", "KR"))
        val RU_RU = TtsLanguage("ru-RU", "Русский", Locale("ru", "RU"))
        val ZH_CN = TtsLanguage("zh-CN", "中文 (简体)", Locale("zh", "CN"))
        val HI_IN = TtsLanguage("hi-IN", "हिन्दी", Locale("hi", "IN"))
        val AR_SA = TtsLanguage("ar-SA", "العربية", Locale("ar", "SA"))
        val TR_TR = TtsLanguage("tr-TR", "Türkçe", Locale("tr", "TR"))

        val DEFAULT = EN_US

        val ALL = listOf(
            EN_US,
            PT_BR,
            ES_ES,
            FR_FR,
            DE_DE,
            IT_IT,
            NL_NL,
            JA_JP,
            KO_KR,
            RU_RU,
            ZH_CN,
            HI_IN,
            AR_SA,
            TR_TR
        )

        fun fromCode(code: String): TtsLanguage =
            ALL.firstOrNull { it.code == code } ?: DEFAULT
    }
}
