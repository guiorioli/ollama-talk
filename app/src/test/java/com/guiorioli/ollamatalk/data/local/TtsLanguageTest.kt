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
    }

    @Test
    fun `ALL list contains 5 languages`() {
        assertEquals(5, TtsLanguage.ALL.size)
    }

    @Test
    fun `DEFAULT is EN_US`() {
        assertEquals(TtsLanguage.EN_US, TtsLanguage.DEFAULT)
    }
}
