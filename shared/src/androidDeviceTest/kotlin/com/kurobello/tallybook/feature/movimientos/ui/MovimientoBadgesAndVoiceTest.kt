package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeSize
import com.kurobello.tallybook.core.ui.movimiento.MovimientoTypeBadge
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.VoiceInputUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.VoiceNotice
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.movimiento_relative_today
import tallybook.shared.generated.resources.movimiento_relative_yesterday
import tallybook.shared.generated.resources.movimiento_voice_hint
import tallybook.shared.generated.resources.movimiento_voice_listening
import tallybook.shared.generated.resources.movimiento_voice_open_settings
import tallybook.shared.generated.resources.movimiento_voice_permission_blocked
import tallybook.shared.generated.resources.movimiento_voice_preparing
import tallybook.shared.generated.resources.movimiento_voice_start
import tallybook.shared.generated.resources.movimiento_voice_stop
import tallybook.shared.generated.resources.movimiento_voice_unavailable

@OptIn(ExperimentalResourceApi::class)
class MovimientoBadgesAndVoiceTest {
  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun typeBadgeShowsTheLocalizedLabelInBothSizes() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoTypeBadge(TipoMovimiento.GASTO)
        MovimientoTypeBadge(TipoMovimiento.INGRESO, size = MovimientoBadgeSize.Large)
      }
    }

    composeRule.onNodeWithText(string(Res.string.expense)).assertExists()
    composeRule.onNodeWithText(string(Res.string.income)).assertExists()
  }

  @Test
  fun dateBadgeShowsTodayAndYesterdayRelativeToTheCivilToday() {
    val today = MovimientoEntrySamples.today
    composeRule.setContent {
      TallybookTheme {
        MovimientoDateBadge(today, today)
        MovimientoDateBadge(
            MovimientoEntrySamples.yesterday,
            today,
            size = MovimientoBadgeSize.Large,
        )
      }
    }

    composeRule.onNodeWithText(string(Res.string.movimiento_relative_today)).assertExists()
    composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)).assertExists()
  }

  @Test
  fun withoutAnOnDeviceRecognizerTheMicrophoneIsADisabledButton() {
    composeRule.setContent {
      TallybookTheme { VoiceDictationButton(VoiceInputUi(), true, VoiceDictationCallbacks.Inert) }
    }

    composeRule
        .onNodeWithTag(VOICE_DICTATION_BUTTON_TAG)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assertIsNotEnabled()
    composeRule.onNodeWithText(string(Res.string.movimiento_voice_unavailable)).assertExists()
  }

  @Test
  fun holdingTheMicrophoneListensUntilItIsReleased() {
    var voice by mutableStateOf(VoiceInputUi(isAvailable = true))
    var starts = 0
    var stops = 0
    composeRule.setContent {
      TallybookTheme {
        VoiceDictationButton(
            voice = voice,
            enabled = true,
            callbacks =
                VoiceDictationCallbacks(
                    onStart = {
                      starts += 1
                      voice = voice.copy(isListening = true)
                    },
                    onStop = {
                      stops += 1
                      voice = voice.copy(isListening = false)
                    },
                    onOpenSettings = {},
                ),
        )
      }
    }

    composeRule.onNodeWithText(string(Res.string.movimiento_voice_hint)).assertExists()
    composeRule.onNodeWithTag(VOICE_DICTATION_BUTTON_TAG).performTouchInput { down(center) }
    composeRule.runOnIdle {
      assertEquals(1, starts)
      assertEquals(0, stops)
    }
    composeRule.onNodeWithText(string(Res.string.movimiento_voice_preparing)).assertExists()
    composeRule.runOnIdle { voice = voice.copy(isMicReady = true) }
    composeRule.onNodeWithText(string(Res.string.movimiento_voice_listening)).assertExists()

    composeRule.onNodeWithTag(VOICE_DICTATION_BUTTON_TAG).performTouchInput { up() }
    composeRule.runOnIdle { assertEquals(1, stops) }
  }

  @Test
  fun movingTheFingerOffTheMicrophoneKeepsListeningUntilItLifts() {
    var starts = 0
    var stops = 0
    composeRule.setContent {
      TallybookTheme {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          VoiceDictationButton(
              voice = VoiceInputUi(isAvailable = true),
              enabled = true,
              callbacks =
                  VoiceDictationCallbacks(
                      onStart = { starts += 1 },
                      onStop = { stops += 1 },
                      onOpenSettings = {},
                  ),
          )
        }
      }
    }

    composeRule.onNodeWithTag(VOICE_DICTATION_BUTTON_TAG).performTouchInput {
      down(center)
      moveBy(Offset(width * 2f, height * 3f))
    }
    composeRule.runOnIdle {
      assertEquals(1, starts)
      assertEquals(0, stops, "a finger drifting off the button must not end the dictation")
    }

    composeRule.onNodeWithTag(VOICE_DICTATION_BUTTON_TAG).performTouchInput { up() }
    composeRule.runOnIdle { assertEquals(1, stops) }
  }

  @Test
  fun screenReadersToggleListeningBecauseTheyCannotHoldAPress() {
    var voice by mutableStateOf(VoiceInputUi(isAvailable = true))
    var starts = 0
    var stops = 0
    composeRule.setContent {
      TallybookTheme {
        VoiceDictationButton(
            voice = voice,
            enabled = true,
            callbacks =
                VoiceDictationCallbacks(
                    onStart = { starts += 1 },
                    onStop = { stops += 1 },
                    onOpenSettings = {},
                ),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(string(Res.string.movimiento_voice_start))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assertIsEnabled()
        .performSemanticsAction(SemanticsActions.OnClick)
    composeRule.runOnIdle {
      assertEquals(1, starts)
      voice = voice.copy(isListening = true)
    }
    composeRule
        .onNodeWithContentDescription(string(Res.string.movimiento_voice_stop))
        .performSemanticsAction(SemanticsActions.OnClick)
    composeRule.runOnIdle { assertEquals(1, stops) }
  }

  @Test
  fun aBlockedPermissionOffersTheAppSettings() {
    var settingsOpened = 0
    composeRule.setContent {
      TallybookTheme {
        VoiceDictationButton(
            voice = VoiceInputUi(isAvailable = true, notice = VoiceNotice.PERMISSION_BLOCKED),
            enabled = true,
            callbacks =
                VoiceDictationCallbacks(
                    onStart = {},
                    onStop = {},
                    onOpenSettings = { settingsOpened += 1 },
                ),
        )
      }
    }

    composeRule
        .onNodeWithText(string(Res.string.movimiento_voice_permission_blocked))
        .assertExists()
    composeRule.onNodeWithText(string(Res.string.movimiento_voice_open_settings)).performClick()
    composeRule.runOnIdle { assertEquals(1, settingsOpened) }
  }

  private fun string(resource: StringResource): String = runBlocking { getString(resource) }
}
