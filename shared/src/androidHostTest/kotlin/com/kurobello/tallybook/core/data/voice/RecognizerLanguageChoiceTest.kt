package com.kurobello.tallybook.core.data.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class RecognizerLanguageChoiceTest {
  private val xiaomiOutOfTheBox =
      RecognizerLanguages(
          installed = listOf("en-US"),
          pending = emptyList(),
          supported = listOf("en-US", "es-ES", "es-US", "pt-BR", "fr-FR"),
      )

  @Test
  fun `a language that only needs downloading is requested instead of failing at once`() {
    assertEquals(
        RecognizerLanguageChoice.Download("es-ES"),
        chooseRecognizerLanguage("es-ES", xiaomiOutOfTheBox),
    )
  }

  @Test
  fun `another installed region of the same language is used before downloading`() {
    val languages = xiaomiOutOfTheBox.copy(installed = listOf("en-US", "es-US"))

    assertEquals(
        RecognizerLanguageChoice.Ready("es-US"),
        chooseRecognizerLanguage("es-CO", languages),
    )
    assertEquals(
        RecognizerLanguageChoice.Ready("es-US"),
        chooseRecognizerLanguage("es_us", languages),
    )
  }

  @Test
  fun `an unsupported region downloads the closest supported one`() {
    assertEquals(
        RecognizerLanguageChoice.Download("es-ES"),
        chooseRecognizerLanguage("es-CO", xiaomiOutOfTheBox),
    )
  }

  @Test
  fun `a download already scheduled waits for it`() {
    val languages = xiaomiOutOfTheBox.copy(pending = listOf("es-ES"))

    assertEquals(
        RecognizerLanguageChoice.Downloading,
        chooseRecognizerLanguage("es-AR", languages),
    )
  }

  @Test
  fun `a language the device cannot recognize offline is unsupported`() {
    assertEquals(
        RecognizerLanguageChoice.Unsupported,
        chooseRecognizerLanguage("de-DE", xiaomiOutOfTheBox),
    )
  }
}
