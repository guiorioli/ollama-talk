package com.guiorioli.ollamatalk.data.local

import org.junit.Assert.*
import org.junit.Test

class TtsLanguageTest {

    @Test
    fun `fromCode returns correct language for valid code`() {
        assertEquals(TtsLanguage.EN_US, TtsLanguage.fromCode("en-US"))
        assertEquals(TtsLanguage.PT_BR, TtsLanguage.fromCode("pt-BR"))
        assertEquals(TtsLanguage.ES_ES, TtsLanguage.fromCode("es-ES"))
        assertEquals(TtsLanguage.FR_FR, TtsLanguage.fromCode("fr-FR"))
        assertEquals(TtsLanguage.DE_DE, TtsLanguage.fromCode("de-DE"))
        assertEquals(TtsLanguage.IT_IT, TtsLanguage.fromCode("it-IT"))
        assertEquals(TtsLanguage.NL_NL, TtsLanguage.fromCode("nl-NL"))
        assertEquals(TtsLanguage.JA_JP, TtsLanguage.fromCode("ja-JP"))
        assertEquals(TtsLanguage.KO_KR, TtsLanguage.fromCode("ko-KR"))
        assertEquals(TtsLanguage.RU_RU, TtsLanguage.fromCode("ru-RU"))
        assertEquals(TtsLanguage.ZH_CN, TtsLanguage.fromCode("zh-CN"))
        assertEquals(TtsLanguage.HI_IN, TtsLanguage.fromCode("hi-IN"))
        assertEquals(TtsLanguage.AR_SA, TtsLanguage.fromCode("ar-SA"))
        assertEquals(TtsLanguage.TR_TR, TtsLanguage.fromCode("tr-TR"))
    }

    @Test
    fun `fromCode returns DEFAULT for unknown code`() {
        assertEquals(TtsLanguage.DEFAULT, TtsLanguage.fromCode("jp-JP"))
        assertEquals(TtsLanguage.DEFAULT, TtsLanguage.fromCode(""))
    }

    @Test
    fun `language code format matches SpeechRecognizer EXTRA_LANGUAGE`() {
        for (lang in TtsLanguage.ALL) {
            val code = lang.code
            assertTrue(
                "Code '$code' should match BCP-47 format",
                code.matches(Regex("[a-z]{2}-[A-Z]{2}"))
            )
        }
    }

    @Test
    fun `locale matches code`() {
        assertEquals("en", TtsLanguage.EN_US.locale.language)
        assertEquals("US", TtsLanguage.EN_US.locale.country)
        assertEquals("pt", TtsLanguage.PT_BR.locale.language)
        assertEquals("BR", TtsLanguage.PT_BR.locale.country)
        assertEquals("es", TtsLanguage.ES_ES.locale.language)
        assertEquals("ES", TtsLanguage.ES_ES.locale.country)
        assertEquals("ja", TtsLanguage.JA_JP.locale.language)
        assertEquals("JP", TtsLanguage.JA_JP.locale.country)
        assertEquals("zh", TtsLanguage.ZH_CN.locale.language)
        assertEquals("CN", TtsLanguage.ZH_CN.locale.country)
        assertEquals("ar", TtsLanguage.AR_SA.locale.language)
        assertEquals("SA", TtsLanguage.AR_SA.locale.country)
    }

    @Test
    fun `ALL list contains 14 languages`() {
        assertEquals(14, TtsLanguage.ALL.size)
    }

    @Test
    fun `DEFAULT is EN_US`() {
        assertEquals(TtsLanguage.EN_US, TtsLanguage.DEFAULT)
    }
}
