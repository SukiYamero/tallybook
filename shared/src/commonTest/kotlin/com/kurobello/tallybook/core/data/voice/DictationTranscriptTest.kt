package com.kurobello.tallybook.core.data.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class DictationTranscriptTest {

  private fun transcriptOf(vararg events: DictationEvent): String {
    val transcript = DictationTranscript()
    var last = ""
    events.forEach { event -> transcript.accept(event)?.let { last = it } }
    return last
  }

  // Recorded on a Xiaomi (Android 13, Google on-device recognizer) without a segmented session:
  // each pause restarts the partial text and the segment's final arrives as one more partial.
  @Test
  fun aRecognizerThatRestartsAfterEachPauseKeepsEveryPart() {
    val text =
        transcriptOf(
            DictationEvent.Partial("ca"),
            DictationEvent.Partial("caste 20"),
            DictationEvent.Partial("caste 20 mil en cine"),
            DictationEvent.Pause,
            DictationEvent.Partial("caste 20 mil en cine"),
            DictationEvent.Partial("y"),
            DictationEvent.Partial("y hoy"),
            DictationEvent.Pause,
            DictationEvent.Partial(" y hoy"),
            DictationEvent.Final(""),
        )

    assertEquals("caste 20 mil en cine y hoy", text)
  }

  // Recorded on the same Xiaomi with a segmented session: segments arrive explicitly.
  @Test
  fun explicitSegmentsAreJoinedInOrder() {
    val text =
        transcriptOf(
            DictationEvent.Partial("gaste 20.000 en"),
            DictationEvent.Pause,
            DictationEvent.Partial("gaste 20.000 en cines"),
            DictationEvent.Segment("gaste 20.000 en cines"),
            DictationEvent.Partial("hoy"),
            DictationEvent.Pause,
            DictationEvent.Segment(" hoy"),
            DictationEvent.Final(""),
        )

    assertEquals("gaste 20.000 en cines hoy", text)
  }

  // The documented contract: every result covers everything since the session started.
  @Test
  fun aRecognizerThatResendsEverythingIsNotDuplicated() {
    val text =
        transcriptOf(
            DictationEvent.Partial("gasté"),
            DictationEvent.Partial("gasté 20"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté 20 mil"),
            DictationEvent.Partial("gasté 20 mil en cine"),
            DictationEvent.Final("gasté 20 mil en cine"),
        )

    assertEquals("gasté 20 mil en cine", text)
  }

  @Test
  fun revisionsWithinOneSegmentReplaceTheWords() {
    val text =
        transcriptOf(
            DictationEvent.Partial("caste"),
            DictationEvent.Partial("gasté 20"),
            DictationEvent.Final("gasté 20 en cine"),
        )

    assertEquals("gasté 20 en cine", text)
  }

  @Test
  fun aFinalWithOnlyTheLastSegmentKeepsTheEarlierOnes() {
    val text =
        transcriptOf(
            DictationEvent.Partial("netflix 20 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("y spotify"),
            DictationEvent.Final("y spotify 10 mil"),
        )

    assertEquals("netflix 20 mil y spotify 10 mil", text)
  }

  @Test
  fun aFinalThatCoversEverythingReplacesTheParts() {
    val text =
        transcriptOf(
            DictationEvent.Partial("netflix 20 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("y spotify"),
            DictationEvent.Final("netflix 20 mil y spotify 10 mil"),
        )

    assertEquals("netflix 20 mil y spotify 10 mil", text)
  }

  @Test
  fun aSegmentEndingEmptyKeepsItsLastPartial() {
    val text =
        transcriptOf(
            DictationEvent.Partial("almuerzo"),
            DictationEvent.Segment(""),
            DictationEvent.Partial("con débito"),
            DictationEvent.Final(""),
        )

    assertEquals("almuerzo con débito", text)
  }

  @Test
  fun aPauseWithoutNewWordsChangesNothing() {
    val text =
        transcriptOf(
            DictationEvent.Partial("arriendo"),
            DictationEvent.Pause,
            DictationEvent.Final(""),
        )

    assertEquals("arriendo", text)
  }
}
