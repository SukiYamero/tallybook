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
  fun aRewordingAfterAPauseInsideASegmentIsNotDuplicated() {
    val text =
        transcriptOf(
            DictationEvent.Partial("caste 20"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté 20 mil"),
            DictationEvent.Segment("gasté 20 mil"),
            DictationEvent.Final(""),
        )

    assertEquals("gasté 20 mil", text)
  }

  @Test
  fun aSegmentKeepsAnEarlierStretchItDoesNotCover() {
    val text =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil en cine"),
            DictationEvent.Pause,
            DictationEvent.Partial("y 40 mil"),
            DictationEvent.Segment("y 40 mil en comida"),
            DictationEvent.Final(""),
        )

    assertEquals("gasté 20 mil en cine y 40 mil en comida", text)
  }

  @Test
  fun aRecognizerThatResendsEverythingMayRewordAfterAPause() {
    val reworded =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté 20.000 en"),
            DictationEvent.Final("gasté 20.000 en cine"),
        )
    val shortened =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil en"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté 20 mil"),
            DictationEvent.Final("gasté 20 mil en cine"),
        )

    assertEquals("gasté 20.000 en cine", reworded)
    assertEquals("gasté 20 mil en cine", shortened)
  }

  @Test
  fun aSegmentDropsAStretchThePauseGuessClosedTooEarly() {
    val text =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil en cine"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté"),
            DictationEvent.Partial("gasté 20 mil en cine"),
            DictationEvent.Segment("gasté 20 mil en cine"),
            DictationEvent.Final(""),
        )

    assertEquals("gasté 20 mil en cine", text)
  }

  @Test
  fun stretchesThatOpenWithTheSameWordAreAllKept() {
    val sameConnector =
        transcriptOf(
            DictationEvent.Partial("netflix 20 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("y"),
            DictationEvent.Partial("y spotify 10 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("y spotify 10 mil"),
            DictationEvent.Partial("y"),
            DictationEvent.Partial("y disney 15 mil"),
            DictationEvent.Final(""),
        )
    val sameVerb =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil en cine"),
            DictationEvent.Pause,
            DictationEvent.Partial("gasté"),
            DictationEvent.Partial("gasté 40 mil en comida"),
            DictationEvent.Final(""),
        )

    assertEquals("netflix 20 mil y spotify 10 mil y disney 15 mil", sameConnector)
    assertEquals("gasté 20 mil en cine gasté 40 mil en comida", sameVerb)
  }

  @Test
  fun aRepeatedStretchIsRemovedOnlyAsWholeWords() {
    val text =
        transcriptOf(
            DictationEvent.Partial("y"),
            DictationEvent.Pause,
            DictationEvent.Partial("yo pagué"),
            DictationEvent.Partial("yo pagué 5"),
            DictationEvent.Final(""),
        )

    assertEquals("y yo pagué 5", text)
  }

  @Test
  fun aFinalRepeatingEarlierStretchesWithOtherCapitalsIsNotDuplicated() {
    val text =
        transcriptOf(
            DictationEvent.Partial("gasté 20 mil"),
            DictationEvent.Pause,
            DictationEvent.Partial("y"),
            DictationEvent.Final("Gasté 20 mil y 30"),
        )

    assertEquals("gasté 20 mil y 30", text)
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
