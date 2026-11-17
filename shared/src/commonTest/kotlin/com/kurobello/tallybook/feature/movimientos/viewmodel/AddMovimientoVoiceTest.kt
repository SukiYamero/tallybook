package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.voice.DictationError
import com.kurobello.tallybook.core.data.voice.DictationEvent
import com.kurobello.tallybook.core.data.voice.DictationPermission
import com.kurobello.tallybook.core.data.voice.FakeVoiceDictation
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AddMovimientoVoiceTest {

  @Test
  fun dictatedWordsFillTheSentenceAndItsPreviewUntilTheFinalResult() = runTest {
    val f = fixture()

    f.viewModel.onVoiceStart()
    runCurrent()
    assertTrue(f.state.voice.isListening)
    assertFalse(f.state.voice.isMicReady, "words said before the mic opens are lost")
    assertEquals(listOf("es-ES"), f.voice.languageTags)

    f.voice.events.send(DictationEvent.Listening)
    runCurrent()
    assertTrue(f.state.voice.isMicReady)

    f.voice.events.send(DictationEvent.Partial("gasté 15000"))
    runCurrent()
    assertEquals("gasté 15000", f.state.phrase)

    f.voice.events.send(DictationEvent.Final("gasté 15000 en cine"))
    advanceTimeBy(PREVIEW_SETTLE_MILLIS)
    runCurrent()

    assertEquals("gasté 15000 en cine", f.state.phrase)
    assertFalse(f.state.voice.isListening)
    assertNull(f.state.voice.notice)
    assertEquals(Money(1_500_000, Moneda.COP), f.state.detected.monto)
    assertEquals(0, f.voice.activeSessions)
  }

  @Test
  fun theDetectedCardKeepsItsVisibilityWhileTheMicrophoneIsHeld() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("gasté 50 000 en juegos"))
    runCurrent()
    assertFalse(f.state.sentence.showsDetected, "words arriving must not push the button down")

    f.voice.events.send(DictationEvent.Final("gasté 50 000 en juegos de computadora"))
    advanceTimeBy(PREVIEW_SETTLE_MILLIS)
    runCurrent()
    assertTrue(f.state.sentence.showsDetected)
    assertEquals(Money(5_000_000, Moneda.COP), f.state.detected.monto)

    f.viewModel.onVoiceStart()
    runCurrent()
    assertTrue(f.state.sentence.showsDetected, "a card already shown stays while held")
  }

  @Test
  fun dictationExtendsWhatWasAlreadyTyped() = runTest {
    val f = fixture()
    f.viewModel.onPhraseChanged("almuerzo ")

    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Final("20000 con tarjeta"))
    runCurrent()

    assertEquals("almuerzo 20000 con tarjeta", f.state.phrase)
  }

  @Test
  fun pausingWhileHeldKeepsListeningAndReleasingKeepsWhatWasHeard() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("arriendo"))
    runCurrent()

    advanceTimeBy(LONG_PAUSE_MILLIS)
    runCurrent()
    assertEquals(0, f.voice.finishRequestCount)
    assertTrue(f.state.voice.isListening)

    f.viewModel.onVoiceStop()
    runCurrent()
    assertEquals(0, f.voice.finishRequestCount, "the last syllable is still being heard")
    advanceTimeBy(RELEASE_TAIL_MILLIS)
    runCurrent()
    assertEquals(1, f.voice.finishRequestCount)

    // The fake never answers the release, so the grace period ends the session itself.
    advanceTimeBy(FINISH_GRACE_UPPER_BOUND_MILLIS)
    runCurrent()
    assertFalse(f.state.voice.isListening)
    assertEquals("arriendo", f.state.phrase)
    assertNull(f.state.voice.notice)
    assertEquals(0, f.voice.activeSessions)
  }

  @Test
  fun aSessionNeverListensLongerThanTheMaximumDuration() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()

    var words = ""
    repeat(KEEP_TALKING_STEPS) {
      words += " palabra"
      f.voice.events.send(DictationEvent.Partial(words.trim()))
      advanceTimeBy(KEEP_TALKING_INTERVAL_MILLIS)
      runCurrent()
    }

    assertTrue(KEEP_TALKING_STEPS * KEEP_TALKING_INTERVAL_MILLIS >= MAX_MILLIS)
    assertEquals(1, f.voice.finishRequestCount)
  }

  @Test
  fun releasingFinishesWithTheRecognizersFinalWords() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()

    f.viewModel.onVoiceStop()
    runCurrent()
    assertEquals(0, f.voice.finishRequestCount, "the last syllable is still being heard")
    advanceTimeBy(RELEASE_TAIL_MILLIS)
    runCurrent()
    assertEquals(1, f.voice.finishRequestCount)

    f.voice.events.send(DictationEvent.Final("pagué netflix"))
    runCurrent()
    assertEquals("pagué netflix", f.state.phrase)
    assertFalse(f.state.voice.isListening)
  }

  @Test
  fun hearingNothingAsksToTryAgain() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()

    f.voice.events.send(DictationEvent.Final(""))
    runCurrent()

    assertFalse(f.state.voice.isListening)
    assertEquals(VoiceNotice.NOT_HEARD, f.state.voice.notice)
    assertEquals("", f.state.phrase)
  }

  @Test
  fun recognizerFailuresBecomeNoticesAndAMissingRecognizerDisablesTheMicrophone() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Failed(DictationError.LANGUAGE_UNAVAILABLE))
    runCurrent()
    assertEquals(VoiceNotice.LANGUAGE_UNAVAILABLE, f.state.voice.notice)
    assertTrue(f.state.voice.isAvailable)

    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("café"))
    f.voice.events.send(DictationEvent.Failed(DictationError.NO_MATCH))
    runCurrent()
    assertNull(f.state.voice.notice, "words already heard need no warning")
    assertEquals("café", f.state.phrase)

    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Failed(DictationError.UNAVAILABLE))
    runCurrent()
    assertFalse(f.state.voice.isAvailable)
    assertFalse(f.state.voice.isListening)
  }

  @Test
  fun typingOrMovingOnStopsListeningAtOnce() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.viewModel.onPhraseChanged("comida 8000")
    runCurrent()
    assertFalse(f.state.voice.isListening)
    assertEquals(0, f.voice.activeSessions)

    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("comida 8000 en efectivo"))
    runCurrent()
    f.viewModel.onPhraseContinue()
    runCurrent()
    assertFalse(f.state.voice.isListening)
    assertEquals(0, f.voice.activeSessions)
    assertEquals(AddMovimientoStep.SUMMARY, f.state.step)

    f.viewModel.onVoiceStart()
    runCurrent()
    assertEquals(0, f.voice.activeSessions, "dictation only runs on the sentence step")
  }

  @Test
  fun slidingToCancelDropsTheDictationAndRestoresTheTypedSentence() = runTest {
    val f = fixture()
    f.viewModel.onPhraseChanged("almuerzo ")
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("20000 con tarjeta"))
    advanceTimeBy(PREVIEW_SETTLE_MILLIS)
    runCurrent()
    assertEquals("almuerzo 20000 con tarjeta", f.state.phrase)
    assertNotNull(f.state.detected.monto)

    f.viewModel.onVoiceDiscard()
    runCurrent()
    assertEquals("almuerzo ", f.state.phrase)
    assertNull(f.state.detected.monto, "the cancelled amount must not linger in Detected")
    assertFalse(f.state.voice.isListening)
    assertNull(f.state.voice.notice)
    assertEquals(0, f.voice.activeSessions)

    // The finger lifts after the slide already cancelled.
    f.viewModel.onVoiceStop()
    advanceTimeBy(RELEASE_TAIL_MILLIS + FINISH_GRACE_UPPER_BOUND_MILLIS)
    runCurrent()
    assertEquals("almuerzo ", f.state.phrase)
    assertFalse(f.state.voice.isListening)
  }

  @Test
  fun wordsAlreadyReleasedSurviveASlideWhileTheRecognizerFinishes() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Partial("almuerzo 20000"))
    runCurrent()

    f.viewModel.onVoiceStop()
    runCurrent()
    assertTrue(f.state.voice.isListening, "the release tail still listens")
    f.viewModel.onVoiceDiscard()
    runCurrent()

    assertEquals("almuerzo 20000", f.state.phrase)
    assertEquals(1, f.voice.activeSessions)
    f.voice.events.send(DictationEvent.Final("almuerzo 20000 pesos"))
    runCurrent()
    assertEquals("almuerzo 20000 pesos", f.state.phrase)
    assertFalse(f.state.voice.isFinishing)
  }

  @Test
  fun slidingToCancelWithoutAnActiveSessionKeepsTheSentence() = runTest {
    val f = fixture()
    f.viewModel.onVoiceStart()
    runCurrent()
    f.voice.events.send(DictationEvent.Final("arriendo 500000"))
    runCurrent()

    f.viewModel.onVoiceDiscard()
    runCurrent()

    assertEquals("arriendo 500000", f.state.phrase, "a finished dictation is the user's text")
  }

  @Test
  fun anUnaskedPermissionIsRequestedAndRepeatedRefusalsPointToSettings() = runTest {
    val f = fixture(FakeVoiceDictation(permission = DictationPermission.NOT_DETERMINED))

    f.viewModel.onVoiceStart()
    assertEquals(AddMovimientoEffect.RequestDictationPermission, f.viewModel.effects.first())
    assertFalse(f.state.voice.isListening)

    f.viewModel.onVoicePermissionResult(false)
    assertEquals(VoiceNotice.PERMISSION_DENIED, f.state.voice.notice)
    f.viewModel.onVoicePermissionResult(false)
    assertEquals(VoiceNotice.PERMISSION_BLOCKED, f.state.voice.notice)

    f.viewModel.onVoiceOpenSettings()
    assertEquals(AddMovimientoEffect.OpenDictationSettings, f.viewModel.effects.first())

    f.viewModel.onVoicePermissionResult(true)
    runCurrent()
    assertFalse(f.state.voice.isListening, "the press that asked is over; the next one listens")
    assertNull(f.state.voice.notice)

    f.voice.permission = DictationPermission.GRANTED
    f.viewModel.onVoiceStart()
    runCurrent()
    assertTrue(f.state.voice.isListening)
  }

  @Test
  fun tappingAgainWhileThePermissionDialogIsUpDoesNotAskTwice() = runTest {
    val f = fixture(FakeVoiceDictation(permission = DictationPermission.NOT_DETERMINED))

    f.viewModel.onVoiceStart()
    f.viewModel.onVoiceStart()
    assertEquals(AddMovimientoEffect.RequestDictationPermission, f.viewModel.effects.first())
    f.viewModel.onVoicePermissionResult(false)
    assertEquals(VoiceNotice.PERMISSION_DENIED, f.state.voice.notice)

    f.viewModel.onVoiceOpenSettings()
    assertEquals(
        AddMovimientoEffect.OpenDictationSettings,
        f.viewModel.effects.first(),
        "the second tap queued no permission request",
    )
  }

  @Test
  fun aDeniedPermissionGoesStraightToSettings() = runTest {
    val f = fixture(FakeVoiceDictation(permission = DictationPermission.DENIED))

    f.viewModel.onVoiceStart()
    runCurrent()

    assertEquals(VoiceNotice.PERMISSION_BLOCKED, f.state.voice.notice)
    assertEquals(0, f.voice.activeSessions)
  }

  @Test
  fun withoutAnOnDeviceRecognizerTheMicrophoneIsUnavailable() = runTest {
    val f = fixture(FakeVoiceDictation(available = false))

    f.viewModel.onVoiceStart()
    runCurrent()

    assertFalse(f.state.voice.isAvailable)
    assertFalse(f.state.voice.isListening)
    assertTrue(f.voice.languageTags.isEmpty())
  }

  private class Fixture(val viewModel: AddMovimientoViewModel, val voice: FakeVoiceDictation) {
    val state: AddMovimientoUiState
      get() = viewModel.uiState.value
  }

  private fun TestScope.fixture(voice: FakeVoiceDictation = FakeVoiceDictation()): Fixture {
    val viewModel =
        createAddViewModel(
            categoriaRepository = AddFakeCategorias(DataOutcome.Success(addSeedCategorias())),
            preferencesRepository = addPreferences(Moneda.COP),
            voice = voice,
            scope = backgroundScope,
        )
    runCurrent()
    return Fixture(viewModel, voice)
  }

  private companion object {
    const val PREVIEW_SETTLE_MILLIS = 300L
    const val FINISH_GRACE_UPPER_BOUND_MILLIS = 3_000L
    const val LONG_PAUSE_MILLIS = 10_000L
    val RELEASE_TAIL_MILLIS = DICTATION_RELEASE_TAIL.inWholeMilliseconds
    const val KEEP_TALKING_INTERVAL_MILLIS = 3_000L
    const val KEEP_TALKING_STEPS = 11
    val MAX_MILLIS = DICTATION_MAX_DURATION.inWholeMilliseconds
  }
}
