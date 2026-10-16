package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSentenceUi
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
import tallybook.shared.generated.resources.category_tag_add_description
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_relative_today
import tallybook.shared.generated.resources.movimiento_relative_yesterday
import tallybook.shared.generated.resources.movimiento_voice_coming_soon
import tallybook.shared.generated.resources.movimiento_voice_title

@OptIn(ExperimentalResourceApi::class)
class MovimientoSentenceStepTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private var continues = 0
  private var manualFills = 0
  private val removed = mutableListOf<String>()
  private val suggested = mutableListOf<String>()
  private var addRequests = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun continueIsDisabledForBlankTextAndEnabledOnceTheUserTypes() {
    show(samples.emptySentence)
    val continueLabel = string(Res.string.movimiento_phrase_continue)

    composeRule.onNodeWithText(continueLabel).assertIsNotEnabled()
    composeRule.onNodeWithContentDescription(inputLabel()).performTextInput("Spent 20 on lunch")
    composeRule.onNodeWithText(continueLabel).assertIsEnabled().performClick()
    composeRule.runOnIdle { assertEquals(1, continues) }
  }

  @Test
  fun fillManuallyIsAlwaysAvailable() {
    show(samples.emptySentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_phrase_fill_manually)).performClick()
    composeRule.runOnIdle { assertEquals(1, manualFills) }
  }

  @Test
  fun inputTakesFocusOnFirstComposition() {
    show(samples.emptySentence, requestFocusOnStart = true)

    composeRule.onNodeWithContentDescription(inputLabel()).assertIsFocused()
  }

  @Test
  fun detectedAreaShowsTheDefaultsBeforeAnythingIsDetected() {
    show(samples.emptySentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_detected_label)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.expense)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_relative_today)).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_add_description))
        .assertIsDisplayed()
  }

  @Test
  fun detectedAreaShowsTheAmountOnlyWhenFound() {
    show(samples.partialSentence)

    composeRule
        .onNodeWithText(
            signedMovimientoAmountText(
                samples.partialSentence.detected.monto!!,
                TipoMovimiento.GASTO,
            )
        )
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(string(Res.string.movimiento_relative_yesterday))
        .assertDoesNotExist()
  }

  @Test
  fun completeSentenceShowsTheDateTheTagAndItsSuggestions() {
    show(samples.completeSentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)).assertIsDisplayed()
    composeRule.onNodeWithText(samples.cinema.nombre).assertIsDisplayed()
    composeRule.onNodeWithText(samples.food.nombre).performClick()
    composeRule.runOnIdle { assertEquals(listOf(samples.food.id), suggested) }
  }

  @Test
  fun theOnlyTagOffersNoRemovalAndAddOpensThePicker() {
    show(samples.completeSentence)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, samples.cinema.nombre))
        .assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_add_description))
        .performClick()
    composeRule.runOnIdle { assertEquals(1, addRequests) }
  }

  @Test
  fun aSecondTagCanBeRemoved() {
    show(samples.completeSentence.copy(categorias = listOf(samples.cinema, samples.food)))

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, samples.food.nombre))
        .performClick()
    composeRule.runOnIdle { assertEquals(listOf(samples.food.id), removed) }
  }

  @Test
  fun voiceDictationIsAnAnnouncedPlaceholder() {
    show(samples.emptySentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_voice_title)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_voice_coming_soon)).assertIsDisplayed()
  }

  @Test
  fun typedTextIsCappedAtTheDescriptionLimit() {
    show(samples.emptySentence)

    composeRule
        .onNodeWithContentDescription(inputLabel())
        .performTextInput("x".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 20))
    composeRule
        .onNodeWithText("$MOVIMIENTO_DESCRIPTION_MAX_LENGTH/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH")
        .assertIsDisplayed()
  }

  private fun show(sentence: MovimientoSentenceUi, requestFocusOnStart: Boolean = false) {
    composeRule.setContent {
      TallybookTheme {
        var text by remember { mutableStateOf(sentence.text) }
        MovimientoSentenceStep(
            sentence = sentence.copy(text = text),
            callbacks =
                MovimientoSentenceCallbacks(
                    onTextChange = { text = it },
                    onContinue = { continues++ },
                    onFillManually = { manualFills++ },
                    onRemoveCategoria = { removed += it.id },
                    onAddCategoria = { addRequests++ },
                    onSuggestion = { suggested += it.id },
                ),
            focusRequester = remember { FocusRequester() },
            requestFocusOnStart = requestFocusOnStart,
        )
      }
    }
  }

  private fun inputLabel(): String = string(Res.string.movimiento_phrase_label)

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }
}
