package com.kurobello.tallybook.core.data.voice

private val whitespace = Regex("\\s+")

/**
 * The whole sentence spoken during one press, joined from what the recognizer reports. Recognizers
 * differ: some repeat everything heard so far, others restart their text after each pause, with or
 * without announcing the [DictationEvent.Segment] that ended.
 *
 * Without segments, a partial after a pause that does not keep the opening of the open text is
 * taken as a restart, and the open text is kept rather than lost. A segment is authoritative for
 * its stretch: it replaces what a pause guess closed when it revises it.
 */
internal class DictationTranscript {
  private val closed = mutableListOf<String>()
  // Pieces closed by a pause guess since the last segment; the next segment drops those it revises.
  private var segmentStart = 0
  private var open = ""
  private var pausedSinceOpen = false
  private var segmented = false

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
    val next = withoutClosedPrefix(partial, closed.size)
    // A recognizer that announces segments restarts only after one, never after a bare pause.
    val restarted = !segmented && pausedSinceOpen && open.isNotEmpty() && isRestart(open, next)
    if (restarted) {
      closed += open
    }
    if (restarted || next != open) pausedSinceOpen = false
    open = next
    return text
  }

  // A piece a pause guess closed is dropped only when the segment revises it; a piece the segment
  // does not cover was its own stretch and stays.
  private fun onSegment(segment: String): String {
    segmented = true
    val words = withoutClosedPrefix(segment, segmentStart).ifEmpty { joinedSinceSegmentStart() }
    val kept =
        if (segment.isEmpty()) emptyList()
        else closed.drop(segmentStart).filter { piece -> isRestart(piece, words) }
    while (closed.size > segmentStart) closed.removeAt(closed.lastIndex)
    closed += kept
    if (words.isNotEmpty()) closed += words
    segmentStart = closed.size
    open = ""
    pausedSinceOpen = false
    return text
  }

  // A final may cover only the last stretch or the whole press; whatever it does not repeat stays.
  private fun onFinal(final: String): String {
    if (final.isNotEmpty()) open = withoutClosedPrefix(final, closed.size)
    if (open.isNotEmpty()) closed += open
    open = ""
    pausedSinceOpen = false
    return text
  }

  private fun joinedSinceSegmentStart(): String =
      (closed.drop(segmentStart) + open).filter(String::isNotEmpty).joinToString(" ")

  /** Drops the first [count] closed pieces from [value] when it repeats them as whole words. */
  private fun withoutClosedPrefix(value: String, count: Int): String {
    val prefix = closed.take(count).joinToString(" ")
    return when {
      prefix.isEmpty() -> value
      value.equals(prefix, ignoreCase = true) -> ""
      value.startsWith("$prefix ", ignoreCase = true) -> value.substring(prefix.length).trim()
      else -> value
    }
  }
}

/**
 * A revision keeps the opening of the old text (the first word, or the second when the first was
 * misheard) or is one word-prefix of the other, and is about as long: a restarting recognizer sends
 * a new stretch word by word, so its first partials are short even when they repeat the opening
 * ("gasté … gasté …").
 */
private fun isRestart(old: String, new: String): Boolean {
  val oldWords = old.lowercase().split(whitespace)
  val newWords = new.lowercase().split(whitespace)
  if (newWords.size < oldWords.size - 1) return true
  val isWordPrefix =
      newWords.size <= oldWords.size && oldWords.subList(0, newWords.size) == newWords ||
          oldWords.size <= newWords.size && newWords.subList(0, oldWords.size) == oldWords
  val sharesOpening = (0..1).any { i -> i < oldWords.size && oldWords[i] == newWords.getOrNull(i) }
  return !isWordPrefix && !sharesOpening
}
