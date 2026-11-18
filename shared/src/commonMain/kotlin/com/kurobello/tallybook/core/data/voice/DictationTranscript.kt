package com.kurobello.tallybook.core.data.voice

/**
 * The whole sentence spoken during one press, joined from what the recognizer reports. Recognizers
 * differ: some repeat everything heard so far, others restart their text after each pause, with or
 * without announcing the [DictationEvent.Segment] that ended. When it is unclear whether new text
 * replaces or follows the old one, the old one is kept: a repeated word is visible and easy to
 * delete, a lost sentence is not.
 */
internal class DictationTranscript {
  private val closed = mutableListOf<String>()
  private var open = ""
  private var pausedSinceOpen = false

  private val text: String
    get() = (closed + open).filter(String::isNotEmpty).joinToString(" ")

  /** The sentence after [event], or null when the event carries no words. */
  fun accept(event: DictationEvent): String? =
      when (event) {
        is DictationEvent.Partial -> onPartial(event.text.trim())
        is DictationEvent.Segment -> onSegment(event.text.trim())
        is DictationEvent.Final -> onFinal(event.text.trim())
        DictationEvent.Pause -> {
          pausedSinceOpen = true
          null
        }
        DictationEvent.Listening,
        is DictationEvent.Failed -> null
      }

  private fun onPartial(partial: String): String? {
    if (partial.isEmpty()) return null
    val restarted =
        pausedSinceOpen && open.isNotEmpty() && partial != open && !partial.startsWith(open)
    if (restarted) close(open)
    if (restarted || partial != open) pausedSinceOpen = false
    open = withoutClosedPrefix(partial)
    return text
  }

  private fun onSegment(segment: String): String {
    close(segment.ifEmpty { open })
    return text
  }

  // A final may cover only the last stretch or the whole press; whatever it does not repeat stays.
  private fun onFinal(final: String): String {
    if (final.isEmpty()) {
      close(open)
    } else {
      open = withoutClosedPrefix(final)
      close(open)
    }
    return text
  }

  private fun withoutClosedPrefix(value: String): String {
    val joined = closed.joinToString(" ")
    return if (joined.isNotEmpty() && value.startsWith(joined)) {
      value.removePrefix(joined).trim()
    } else {
      value
    }
  }

  private fun close(value: String) {
    if (value.isNotEmpty()) closed += value
    open = ""
    pausedSinceOpen = false
  }
}
