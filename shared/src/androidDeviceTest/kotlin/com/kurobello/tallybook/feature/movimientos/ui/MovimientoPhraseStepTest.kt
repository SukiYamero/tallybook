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
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
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
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_relative_yesterday

@OptIn(ExperimentalResourceApi::class)
class MovimientoPhraseStepTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private var continues = 0
  private var manualFills = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun continueIsDisabledForBlankTextAndEnabledOnceTheUserTypes() {
    show(preview = samples.emptyPreview)
    val continueLabel = string(Res.string.movimiento_phrase_continue)

    composeRule.onNodeWithText(continueLabel).assertIsNotEnabled()
    composeRule.onNodeWithContentDescription(inputLabel()).performTextInput("Spent 20 on lunch")
    composeRule.onNodeWithText(continueLabel).assertIsEnabled().performClick()
    composeRule.runOnIdle { assertEquals(1, continues) }
  }

  @Test
  fun fillManuallyIsAlwaysAvailable() {
    show(preview = samples.emptyPreview)

    composeRule.onNodeWithText(string(Res.string.movimiento_phrase_fill_manually)).performClick()
    composeRule.runOnIdle { assertEquals(1, manualFills) }
  }

  @Test
  fun inputTakesFocusOnFirstComposition() {
    show(preview = samples.emptyPreview, requestFocusOnStart = true)

    composeRule.onNodeWithContentDescription(inputLabel()).assertIsFocused()
  }

  @Test
  fun previewShowsOnlyTheDetectedValues() {
    show(preview = samples.partialPreview)

    composeRule
        .onNodeWithText(
            signedMovimientoAmountText(samples.partialPreview.monto!!, TipoMovimiento.GASTO)
        )
        .assertIsDisplayed()
    composeRule
        .onNodeWithText(string(Res.string.movimiento_relative_yesterday))
        .assertDoesNotExist()
  }

  @Test
  fun completePreviewAddsTheCategoryAndRelativeDate() {
    show(preview = samples.completePreview)

    composeRule.onNodeWithText(samples.cinema.nombre).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)).assertIsDisplayed()
  }

  @Test
  fun typedTextIsCappedAtTheDescriptionLimit() {
    show(preview = samples.emptyPreview)

    composeRule
        .onNodeWithContentDescription(inputLabel())
        .performTextInput("x".repeat(MOVIMIENTO_DESCRIPTION_MAX_LENGTH + 20))
    composeRule
        .onNodeWithText("$MOVIMIENTO_DESCRIPTION_MAX_LENGTH/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH")
        .assertIsDisplayed()
  }

  private fun show(preview: PhrasePreviewUi, requestFocusOnStart: Boolean = false) {
    composeRule.setContent {
      TallybookTheme {
        var text by remember { mutableStateOf("") }
        MovimientoPhraseStep(
            text = text,
            preview = preview,
            moneda = MovimientoEntrySamples.moneda,
            onTextChange = { text = it },
            onContinue = { continues++ },
            onFillManually = { manualFills++ },
            focusRequester = remember { FocusRequester() },
            today = MovimientoEntrySamples.today,
            requestFocusOnStart = requestFocusOnStart,
        )
      }
    }
  }

  private fun inputLabel(): String = string(Res.string.movimiento_phrase_label)

  private fun string(resource: StringResource): String = runBlocking { getString(resource) }
}
