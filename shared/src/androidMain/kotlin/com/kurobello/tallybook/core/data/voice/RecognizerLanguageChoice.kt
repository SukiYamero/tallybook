package com.kurobello.tallybook.core.data.voice

/** What the on-device recognizer reports for a request (`RecognitionSupport`, API 33+). */
internal data class RecognizerLanguages(
    val installed: List<String>,
    val pending: List<String>,
    val supported: List<String>,
)

internal sealed interface RecognizerLanguageChoice {
  data class Ready(val tag: String) : RecognizerLanguageChoice

  data class Download(val tag: String) : RecognizerLanguageChoice

  data object Downloading : RecognizerLanguageChoice

  data object Unsupported : RecognizerLanguageChoice
}

/**
 * The language to dictate [requested] in. A model of another region of the same language beats
 * waiting for a download: an es-CO speaker is understood by the es-US model.
 */
internal fun chooseRecognizerLanguage(
    requested: String,
    languages: RecognizerLanguages,
): RecognizerLanguageChoice {
  val wanted = normalizedTag(requested)
  fun List<String>.closest(): String? =
      firstOrNull { normalizedTag(it) == wanted }
          ?: firstOrNull { languageOf(it) == languageOf(wanted) }
  return languages.installed.closest()?.let(RecognizerLanguageChoice::Ready)
      ?: languages.pending.closest()?.let { RecognizerLanguageChoice.Downloading }
      ?: languages.supported.closest()?.let(RecognizerLanguageChoice::Download)
      ?: RecognizerLanguageChoice.Unsupported
}

private fun normalizedTag(tag: String): String = tag.replace('_', '-').lowercase()

private fun languageOf(tag: String): String = normalizedTag(tag).substringBefore('-')
