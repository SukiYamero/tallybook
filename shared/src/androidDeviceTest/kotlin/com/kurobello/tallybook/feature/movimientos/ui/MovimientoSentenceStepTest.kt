package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
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
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_relative_today
import tallybook.shared.generated.resources.movimiento_relative_yesterday
import tallybook.shared.generated.resources.movimiento_voice_coming_soon
import tallybook.shared.generated.resources.movimiento_voice_title
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.tag_add_description
import tallybook.shared.generated.resources.tag_remove
import tallybook.shared.generated.resources.tags_unavailable

private val SHORT_DIALOG_HEIGHT = 300.dp

@OptIn(ExperimentalResourceApi::class)
class MovimientoSentenceStepTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private var continues = 0
  private var manualFills = 0
  private var deselects = 0
  private val suggested = mutableListOf<String>()
  private var chooseRequests = 0
  private val removedTags = mutableListOf<String>()
  private var tagAddRequests = 0
  private var tagRetries = 0

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
  fun inputDoesNotTakeFocusWhenTheDialogOpens() {
    show(samples.emptySentence)

    composeRule.onNodeWithContentDescription(inputLabel()).assertIsNotFocused()
  }

  @Test
  fun detectedAreaStaysHiddenWhileTheSentenceIsBlank() {
    show(samples.emptySentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_detected_label)).assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_picker_single_title))
        .assertDoesNotExist()
    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_add_description))
        .assertDoesNotExist()
  }

  @Test
  fun detectedAreaAppearsWithTheDefaultsOnceTheUserTypes() {
    show(samples.emptySentence)

    composeRule.onNodeWithContentDescription(inputLabel()).performTextInput("Lunch")

    composeRule.onNodeWithText(string(Res.string.movimiento_detected_label)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.expense)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_relative_today)).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_picker_single_title))
        .assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_add_description))
        .assertIsDisplayed()
  }

  @Test
  fun detectedAreaSaysWhenNoAmountWasDetected() {
    show(samples.emptySentence)
    composeRule.onNodeWithContentDescription(inputLabel()).performTextInput("Lunch")

    composeRule
        .onNodeWithText(string(Res.string.movimiento_amount_not_detected))
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
        .onNodeWithText(string(Res.string.movimiento_amount_not_detected))
        .assertDoesNotExist()
    composeRule
        .onNodeWithText(string(Res.string.movimiento_relative_yesterday))
        .assertDoesNotExist()
  }

  @Test
  fun aDimmedSuggestionStaysClickableAndSwitchesTheCategory() {
    show(samples.completeSentence)

    composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)).assertIsDisplayed()
    composeRule.onNodeWithText(samples.cinema.nombre).assertIsDisplayed()
    composeRule
        .onNodeWithText(samples.food.nombre)
        .assertHasClickAction()
        .assertIsEnabled()
        .performClick()
    composeRule.runOnIdle { assertEquals(listOf(samples.food.id), suggested) }
  }

  @Test
  fun theSelectedCategoryCanBeDeselectedAndChooseOpensThePicker() {
    show(samples.completeSentence)

    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, samples.cinema.nombre))
        .performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_picker_single_title))
        .performClick()
    composeRule.runOnIdle {
      assertEquals(1, deselects)
      assertEquals(1, chooseRequests)
    }
  }

  @Test
  fun detectedTagsCanBeRemovedAndAddOpensTheTagPicker() {
    show(samples.completeSentence)

    composeRule.onNodeWithText(samples.business.nombre).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_remove, samples.business.nombre))
        .performClick()
    composeRule.onNodeWithContentDescription(string(Res.string.tag_add_description)).performClick()
    composeRule.runOnIdle {
      assertEquals(listOf(samples.business.id), removedTags)
      assertEquals(1, tagAddRequests)
    }
  }

  @Test
  fun anUnavailableTagsCatalogOffersRetryInTheTagsSectionOnly() {
    show(samples.completeSentence.copy(etiquetasLoad = EtiquetasLoad.UNAVAILABLE))

    composeRule.onNodeWithText(string(Res.string.tags_unavailable)).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.retry)).performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_add_description))
        .assertDoesNotExist()
    composeRule.onNodeWithText(string(Res.string.movimiento_phrase_continue)).assertIsEnabled()
    composeRule.runOnIdle { assertEquals(1, tagRetries) }
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

  @Test
  fun continueStaysOnScreenWhenTheDialogIsTooShortForItsContent() {
    show(samples.completeSentence, maxHeight = SHORT_DIALOG_HEIGHT)

    composeRule.onNodeWithText(string(Res.string.movimiento_phrase_continue)).assertIsDisplayed()
  }

  private fun show(
      sentence: MovimientoSentenceUi,
      maxHeight: Dp = Dp.Unspecified,
  ) {
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
                    onDeselectCategoria = { deselects++ },
                    onChooseCategoria = { chooseRequests++ },
                    onSuggestion = { suggested += it.id },
                    etiquetas =
                        MovimientoEtiquetasCallbacks(
                            onAdd = { tagAddRequests++ },
                            onRemove = { removedTags += it.id },
                            onRetry = { tagRetries++ },
                        ),
                ),
            modifier = Modifier.heightIn(max = maxHeight),
        )
      }
    }
  }

  private fun inputLabel(): String = string(Res.string.movimiento_phrase_label)

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }
}
