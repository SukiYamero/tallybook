package com.kurobello.tallybook.feature.movimientos.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.permission.DictationPermissionLauncher
import com.kurobello.tallybook.core.ui.testing.closeSoftKeyboard
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoEffect
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.COP_CASH
import com.kurobello.tallybook.feature.movimientos.viewmodel.CuentaSelectionState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.SelectionOwner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.discard_changes_title
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_summary_save
import tallybook.shared.generated.resources.save_expense
import tallybook.shared.generated.resources.tag_add_description
import tallybook.shared.generated.resources.tag_picker_create

class AddMovimientoSheetTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun opensOnTheSentenceStepWithContinueDisabledUntilTextIsTyped() {
    var phrase by mutableStateOf("")
    // The field reflects what it emits, as the ViewModel does; a frozen value would be echoed back
    // by the real keyboard once it resyncs.
    val actions = SheetRecordingActions(onPhraseRecorded = { phrase = it })
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.PHRASE, phrase = phrase),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_phrase_continue))
        .assertIsNotEnabled()
    composeRule.onNodeWithTag(PHRASE_INPUT_TAG).performTextInput("Gasté 20.000")
    composeRule.runOnIdle { assertEquals("Gasté 20.000", actions.lastPhrase) }
    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_phrase_continue))
        .assertIsEnabled()
        .performClick()
    composeRule.runOnIdle { assertEquals(1, actions.continueCount) }
  }

  @Test
  fun fillingManuallyReachesTheActionsWithoutSubmitting() {
    val actions = SheetRecordingActions()
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.PHRASE),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_phrase_fill_manually))
        .performClick()

    composeRule.runOnIdle {
      assertEquals(1, actions.fillManuallyCount)
      assertEquals(0, actions.submitCount)
    }
  }

  @Test
  fun theSentenceDialogCreatesATagFromTheTagPickerSearch() {
    val actions = SheetRecordingActions()
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.PHRASE, phrase = "Gasté 1"),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(resourceString(Res.string.tag_add_description))
        .performClick()
    composeRule.onNodeWithText(work.nombre).assertExists()
    composeRule.onNode(hasSetTextAction() and !hasTestTag(PHRASE_INPUT_TAG)).performTextInput("Gym")
    composeRule.onNodeWithText(resourceString(Res.string.tag_picker_create, "Gym")).performClick()

    composeRule.runOnIdle { assertEquals(listOf("Gym"), actions.createdEtiquetas) }
  }

  @Test
  fun summaryOffersEditAndSaveAsTheTwoActions() {
    val actions = SheetRecordingActions()
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.SUMMARY),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithText(resourceString(Res.string.movimiento_summary_edit)).performClick()
    composeRule
        .onNodeWithContentDescription(resourceString(Res.string.movimiento_summary_save))
        .performClick()

    composeRule.runOnIdle {
      assertEquals(1, actions.editCount)
      assertEquals(1, actions.submitCount)
    }
  }

  @Test
  fun backOnTheSummaryStepsBackInsteadOfDismissing() {
    val actions = SheetRecordingActions()
    var dismissCount = 0
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.SUMMARY),
            actions = actions,
            onDismiss = { dismissCount += 1 },
        )
      }
    }
    composeRule
        .onNodeWithContentDescription(resourceString(Res.string.movimiento_summary_save))
        .assertIsDisplayed()

    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals(1, actions.stepBackCount)
      assertEquals(0, dismissCount)
    }
  }

  @Test
  fun backOnAnUntouchedSentenceStepDismissesTheSheet() {
    val actions = SheetRecordingActions()
    var dismissCount = 0
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.PHRASE),
            actions = actions,
            onDismiss = { dismissCount += 1 },
        )
      }
    }
    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_phrase_continue))
        .assertIsDisplayed()

    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals(0, actions.stepBackCount)
      assertEquals(1, dismissCount)
    }
  }

  @Test
  fun backOnATypedSentenceAsksBeforeDiscarding() {
    val actions = SheetRecordingActions()
    var dismissCount = 0
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.PHRASE, phrase = "Gasté 20.000"),
            actions = actions,
            onDismiss = { dismissCount += 1 },
        )
      }
    }
    composeRule
        .onNodeWithText(resourceString(Res.string.movimiento_phrase_continue))
        .assertIsDisplayed()

    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()

    composeRule.onNodeWithText(resourceString(Res.string.discard_changes_title)).assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(0, dismissCount) }
  }

  @Test
  fun theSentenceDialogAndTheReviewSheetNeverShowTogether() {
    var step by mutableStateOf(AddMovimientoStep.PHRASE)
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = step),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }
    val continueLabel = resourceString(Res.string.movimiento_phrase_continue)
    val saveLabel = resourceString(Res.string.movimiento_summary_save)
    composeRule.onNodeWithText(continueLabel).assertIsDisplayed()
    composeRule.onNodeWithContentDescription(saveLabel).assertDoesNotExist()

    composeRule.runOnIdle { step = AddMovimientoStep.SUMMARY }
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithContentDescription(saveLabel).fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithText(continueLabel).fetchSemanticsNodes().isEmpty()
    }
  }

  @Test
  fun reviewSheetOpensTallInsteadOfHuggingItsContent() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(step = AddMovimientoStep.SUMMARY),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    val sheetNode =
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    resourceString(Res.string.add_movimiento_pane_title),
                )
            )
            .fetchSemanticsNode()
    val rootHeight = sheetNode.layoutInfo.coordinates.findRootCoordinates().size.height.toFloat()

    composeRule.runOnIdle {
      assertTrue(
          sheetNode.boundsInRoot.height >= rootHeight * REVIEW_MINIMUM_FRACTION,
          "Expected the review sheet to be tall: ${sheetNode.boundsInRoot.height} of $rootHeight",
      )
    }
  }

  @Test
  fun formStepUsesContentHeightSheet() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    val sheetNode =
        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    resourceString(Res.string.add_movimiento_pane_title),
                )
            )
            .fetchSemanticsNode()
    val rootHeight = sheetNode.layoutInfo.coordinates.findRootCoordinates().size.height.toFloat()

    composeRule.runOnIdle {
      assertTrue(
          sheetNode.boundsInRoot.height < rootHeight,
          "Expected the form sheet to remain shorter than the window",
      )
    }
  }

  @Test
  fun dateAppearsAboveAmount() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    val dateBounds =
        composeRule
            .onNodeWithContentDescription(resourceString(Res.string.date))
            .getUnclippedBoundsInRoot()
    val amountBounds =
        composeRule
            .onNodeWithContentDescription(
                resourceString(Res.string.movimiento_amount_expense_prompt)
            )
            .getUnclippedBoundsInRoot()

    assertTrue(
        dateBounds.bottom <= amountBounds.top,
        "Expected date to be above amount: date=$dateBounds, amount=$amountBounds",
    )
  }

  @Test
  fun preparedFormCannotOpenTheCalendarWhileInitialDataIsLoading() {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state().copy(isLoading = true),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithContentDescription(resourceString(Res.string.date)).performTouchInput {
      click()
    }

    composeRule.onNodeWithText("16").assertDoesNotExist()
  }

  @Test
  fun submittingDisablesTheCtaSoItCannotSubmitTwice() {
    val actions = SheetRecordingActions()

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(isSubmitting = true),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(resourceString(Res.string.save_expense))
        .assertIsNotEnabled()
    composeRule.runOnIdle { assertEquals(0, actions.submitCount) }
  }

  @Test
  fun validCtaSubmitsOnce() {
    val actions = SheetRecordingActions()

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = actions,
            onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithText(resourceString(Res.string.save_expense)).performClick()
    composeRule.runOnIdle { assertEquals(1, actions.submitCount) }
  }

  @Test
  fun savingAnIncompleteFormShowsTheAmountError() {
    var formState by mutableStateOf(state().copy(amountInput = ""))
    val actions = SheetRecordingActions { formState = formState.copy(submitAttempted = true) }
    composeRule.setContent {
      TallybookTheme { MovimientoForm(uiState = formState, actions = actions) }
    }

    composeRule
        .onNodeWithText(resourceString(Res.string.save_expense))
        .assertIsEnabled()
        .performClick()

    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                resourceString(Res.string.amount_required),
            )
        )
        .assertExists()
  }

  @Test
  fun validationErrorsKeepTheSaveActionInsideTheWindow() {
    var navigationBarBottom = 0
    composeRule.setContent {
      TallybookTheme {
        navigationBarBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
        AddMovimientoSheet(
            visible = true,
            uiState = state().copy(amountInput = "", submitAttempted = true),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                resourceString(Res.string.amount_required),
            )
        )
        .assertExists()
    // The error focuses the amount, so the real keyboard opens over the footer until dismissed.
    composeRule.closeSoftKeyboard()
    assertSaveFullyReachable(navigationBarBottom)
  }

  @Test
  fun submittingCtaKeepsTheIdleCtaHeight() {
    var isSubmitting by mutableStateOf(false)

    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(
            visible = true,
            uiState = state(isSubmitting = isSubmitting),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    val idleHeight =
        composeRule
            .onNodeWithContentDescription(resourceString(Res.string.save_expense))
            .fetchSemanticsNode()
            .boundsInRoot
            .height
    composeRule.runOnIdle { isSubmitting = true }
    composeRule.waitForIdle()

    assertEquals(
        idleHeight,
        composeRule
            .onNodeWithContentDescription(resourceString(Res.string.save_expense))
            .fetchSemanticsNode()
            .boundsInRoot
            .height,
    )
  }

  @Test
  fun closedKeyboardCtaIsFullyReachable() {
    var navigationBarBottom = 0
    composeRule.setContent {
      TallybookTheme {
        navigationBarBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
        AddMovimientoSheet(
            visible = true,
            uiState = state(),
            actions = SheetRecordingActions(),
            onDismiss = {},
        )
      }
    }

    assertSaveFullyReachable(navigationBarBottom)
  }

  @Test
  fun savedEffectUsesTheLatestDismissCallbackExactlyOnce() {
    val effects = MutableSharedFlow<AddMovimientoEffect>(extraBufferCapacity = 1)
    var firstDismisses = 0
    var latestDismisses = 0
    var useLatestCallback by mutableStateOf(false)

    composeRule.setContent {
      AddMovimientoEffects(
          effects = effects,
          onSaved = { if (useLatestCallback) latestDismisses += 1 else firstDismisses += 1 },
          dictationPermission = InertDictationPermission,
      )
    }
    composeRule.runOnIdle { useLatestCallback = true }
    composeRule.runOnIdle { effects.tryEmit(AddMovimientoEffect.Saved(null)) }
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals(0, firstDismisses)
      assertEquals(1, latestDismisses)
    }
  }

  private companion object {
    const val REVIEW_MINIMUM_FRACTION = 0.6f
  }

  private fun assertSaveFullyReachable(navigationBarBottom: Int) {
    val cta = composeRule.onNodeWithContentDescription(resourceString(Res.string.save_expense))
    cta.assertIsDisplayed()
    val coordinates = cta.fetchSemanticsNode().layoutInfo.coordinates
    val bottom = coordinates.positionInRoot().y + coordinates.size.height
    val usableBottom = coordinates.findRootCoordinates().size.height - navigationBarBottom
    assertTrue(coordinates.size.height >= with(composeRule.density) { 48.dp.toPx() })
    assertTrue(bottom <= usableBottom, "Expected complete CTA bottom $bottom above $usableBottom")
  }

  private fun state(
      step: AddMovimientoStep = AddMovimientoStep.FORM,
      isSubmitting: Boolean = false,
      phrase: String = "",
  ) =
      AddMovimientoUiState(
          tipo = TipoMovimiento.GASTO,
          amountInput = "1",
          fecha = LocalDate(2026, 9, 15),
          primaryMoneda = Moneda.COP,
          cuentas = listOf(COP_CASH),
          cuentaChoice =
              CuentaSelectionState(MedioPago.EFECTIVO, COP_CASH.id, SelectionOwner.DEFAULT),
          categorias = listOf(category, otros),
          categoriaId = category.id,
          titulo = "Gasté 1 en comida",
          descripcion = "",
          isLoading = false,
          isSubmitting = isSubmitting,
          submitAttempted = false,
          error = null,
          step = step,
          phrase = phrase,
          hasUnsavedChanges = phrase.isNotBlank(),
          formOpenedFromSummary = step == AddMovimientoStep.FORM,
          etiquetas = listOf(work),
          etiquetasLoad = EtiquetasLoad.READY,
      )

  private val category =
      Categoria(
          id = "food",
          nombre = "Comida",
          padreId = null,
          icono = CategoryIcon.UTENSILS,
          color = CategoryTint.AMBER,
      )

  private val work = Etiqueta("work", "Empresa", Instant.parse("2026-01-01T00:00:00Z"))

  private val otros =
      Categoria(
          id = CATEGORIA_OTROS_ID,
          nombre = "Otros",
          padreId = null,
          icono = CategoryIcon.CIRCLE_DASHED,
          color = CategoryTint.NEUTRAL,
      )

  private fun resourceString(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  @Suppress("TooManyFunctions")
  private class SheetRecordingActions(
      private val onPhraseRecorded: (String) -> Unit = {},
      private val onSubmitRequested: () -> Unit = {},
  ) : AddMovimientoActions {
    var submitCount = 0
    var continueCount = 0
    var fillManuallyCount = 0
    var editCount = 0
    var stepBackCount = 0
    var lastPhrase: String? = null
    val createdEtiquetas = mutableListOf<String>()

    override fun onTipoChanged(tipo: TipoMovimiento) = Unit

    override fun onAmountChanged(value: String) = Unit

    override fun onFechaChanged(value: LocalDate) = Unit

    override fun onCategoriaSelected(id: String) = Unit

    override fun onCategoriaCleared() = Unit

    override fun onEtiquetasChanged(ids: List<String>) = Unit

    override fun onEtiquetaRemoved(id: String) = Unit

    override fun onCreateEtiqueta(nombre: String) {
      createdEtiquetas += nombre
    }

    override fun onRetryEtiquetas() = Unit

    override fun onTituloChanged(value: String) = Unit

    override fun onDescripcionChanged(value: String) = Unit

    override fun onDiscardChanges() = Unit

    override fun onPhraseChanged(value: String) {
      lastPhrase = value
      onPhraseRecorded(value)
    }

    override fun onPhraseContinue() {
      continueCount += 1
    }

    override fun onFillManually() {
      fillManuallyCount += 1
    }

    override fun onEditMovimiento() {
      editCount += 1
    }

    override fun onSuggestionSelected(id: String) = Unit

    override fun onStepBack() {
      stepBackCount += 1
    }

    override fun onRepetirChanged(enabled: Boolean) = Unit

    override fun onFrecuenciaChanged(frecuencia: Frecuencia) = Unit

    override fun onPartCategoriaSelected(index: Int, id: String) = Unit

    override fun onPartCategoriaCleared(index: Int) = Unit

    override fun onPartRemoved(index: Int) = Unit

    override fun onJoinParts() = Unit

    override fun onPartEdit(index: Int) = Unit

    override fun onPartDone() = Unit

    override fun onVoiceStart() = Unit

    override fun onVoiceStop() = Unit

    override fun onVoiceCancel() = Unit

    override fun onVoiceDiscard() = Unit

    override fun onVoicePermissionResult(granted: Boolean) = Unit

    override fun onVoiceOpenSettings() = Unit

    override fun onRetryLoad() = Unit

    override fun onSubmit() {
      submitCount += 1
      onSubmitRequested()
    }

    override fun onMedioSelected(medio: MedioPago) = Unit

    override fun onCuentaSelected(id: String) = Unit
  }
}

private object InertDictationPermission : DictationPermissionLauncher {
  override fun request() = Unit

  override fun openSettings() = Unit
}
