package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.COP_CASH
import com.kurobello.tallybook.feature.movimientos.viewmodel.CuentaSelectionState
import com.kurobello.tallybook.feature.movimientos.viewmodel.EntryPart
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoReviewUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.SelectionOwner
import com.kurobello.tallybook.feature.movimientos.viewmodel.SuggestionQuery
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.category_tag_remove
import tallybook.shared.generated.resources.movimiento_category_section
import tallybook.shared.generated.resources.movimiento_part_done
import tallybook.shared.generated.resources.movimiento_part_no_category
import tallybook.shared.generated.resources.movimiento_parts_detected
import tallybook.shared.generated.resources.movimiento_parts_join
import tallybook.shared.generated.resources.movimiento_parts_remove
import tallybook.shared.generated.resources.movimiento_parts_save
import tallybook.shared.generated.resources.movimiento_parts_title
import tallybook.shared.generated.resources.movimiento_parts_total
import tallybook.shared.generated.resources.movimiento_parts_total_excluded
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_tags_section
import tallybook.shared.generated.resources.recurrencia_repeat_label

@OptIn(ExperimentalResourceApi::class)
class MovimientoReviewStepTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private val edited = mutableListOf<Int>()
  private val categoryRequests = mutableListOf<Int>()
  private val removed = mutableListOf<Int>()
  private var joins = 0
  private var saves = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun theReviewNamesHowManyMovimientosAndTheirTotal() {
    show(samples.review)

    composeRule.onNodeWithText(plural(Res.plurals.movimiento_parts_title, 2)).assertIsDisplayed()
    composeRule
        .onNodeWithText(string(Res.string.movimiento_parts_total, total(samples.review)))
        .assertIsDisplayed()
  }

  @Test
  fun eachRowShowsItsAmountTituloAndCategory() {
    show(samples.review)
    val (first, second) = samples.review.parts

    composeRule.onNodeWithText(requireNotNull(first.monto).format()).assertIsDisplayed()
    composeRule.onNodeWithText(first.titulo).assertIsDisplayed()
    composeRule.onNodeWithText(samples.food.nombre).assertIsDisplayed()
    composeRule.onNodeWithText(requireNotNull(second.monto).format()).assertIsDisplayed()
    composeRule.onNodeWithText(second.titulo).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_part_no_category)).assertIsDisplayed()
  }

  @Test
  fun eachRowIsAnnouncedAsOneUnitWithItsAmountTituloAndCategory() {
    show(samples.review)
    val (first, second) = samples.review.parts

    composeRule
        .onNodeWithText(first.titulo)
        .assertHasClickAction()
        .assert(hasContentDescription(requireNotNull(first.monto).format(), substring = true))
        .assert(hasContentDescription(first.titulo, substring = true))
        .assert(hasContentDescription(samples.food.nombre, substring = true))
    composeRule
        .onNodeWithText(second.titulo)
        .assert(
            hasContentDescription(string(Res.string.movimiento_part_no_category), substring = true)
        )
  }

  @Test
  fun tappingARowOpensThatPart() {
    show(samples.review)

    composeRule.onNodeWithText(samples.review.parts[1].titulo).performClick()

    composeRule.runOnIdle { assertEquals(listOf(1), edited) }
  }

  @Test
  fun eachCategoryChipOpensTheCategoryOfItsPart() {
    show(samples.review)

    composeRule.onNodeWithText(samples.food.nombre).performClick()
    composeRule.onNodeWithText(string(Res.string.movimiento_part_no_category)).performClick()

    composeRule.runOnIdle { assertEquals(listOf(0, 1), categoryRequests) }
  }

  @Test
  fun theRemoveActionNamesItsPartAndRemovesIt() {
    show(samples.review)

    composeRule
        .onNodeWithContentDescription(
            string(Res.string.movimiento_parts_remove, samples.review.parts[1].titulo)
        )
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf(1), removed) }
  }

  @Test
  fun joinIsDisabledWhileThePartsCannotBeJoined() {
    show(samples.review.copy(canJoin = false))

    composeRule.onNodeWithText(string(Res.string.movimiento_parts_join)).assertIsNotEnabled()
  }

  @Test
  fun joinAndSaveReachTheirActionsAndSaveCountsTheParts() {
    show(samples.review)

    composeRule
        .onNodeWithText(string(Res.string.movimiento_parts_join))
        .assertIsEnabled()
        .performClick()
    composeRule.onNodeWithText(string(Res.string.movimiento_parts_save, 2)).performClick()

    composeRule.runOnIdle {
      assertEquals(1, joins)
      assertEquals(1, saves)
    }
  }

  @Test
  fun aDivergedPartShowsWhatDiffersAndTheTotalSaysItIsLeftOut() {
    show(samples.divergedReview)

    composeRule
        .onNodeWithText(plural(Res.plurals.movimiento_parts_total_excluded, 1))
        .assertIsDisplayed()
    composeRule
        .onNode(hasText(samples.visa.nombre, substring = true), useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun theDetectedCardListsEveryPartInsteadOfTheCategoryAndTags() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoSentenceStep(
            sentence = samples.splitSentence,
            callbacks =
                MovimientoSentenceCallbacks(
                    onTextChange = {},
                    onContinue = {},
                    onFillManually = {},
                    onDeselectCategoria = {},
                    onChooseCategoria = {},
                    onSuggestion = {},
                    etiquetas =
                        MovimientoEtiquetasCallbacks(onAdd = {}, onRemove = {}, onRetry = {}),
                ),
        )
      }
    }
    val parts = samples.splitSentence.detected.parts

    composeRule
        .onNode(hasText(plural(Res.plurals.movimiento_parts_detected, 2)), useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText(parts[0].titulo).assertIsDisplayed()
    composeRule.onNodeWithText(parts[1].titulo).assertIsDisplayed()
    composeRule.onNodeWithText(string(Res.string.movimiento_category_section)).assertDoesNotExist()
    composeRule.onNodeWithText(string(Res.string.movimiento_tags_section)).assertDoesNotExist()
  }

  @Test
  fun theSheetShowsTheReviewAsItsOwnStep() {
    showSheet(reviewState(), PartRecordingActions())

    composeRule.onNodeWithTag(REVIEW_STEP_TAG).assertExists()
    composeRule.onNodeWithText(string(Res.string.movimiento_summary_edit)).assertDoesNotExist()
  }

  @Test
  fun aSuggestionUnderAPartChipPicksItForThatPart() {
    val actions = PartRecordingActions()
    showSheet(reviewState(), actions)

    composeRule.onNodeWithText(string(Res.string.movimiento_part_no_category)).performClick()
    composeRule.onNodeWithText(samples.transport.nombre).performClick()

    composeRule.runOnIdle {
      assertEquals(listOf(1 to samples.transport.id), actions.categoriaSelections)
    }
  }

  @Test
  fun removingTheCategoryUnderAPartChipClearsThatPart() {
    val actions = PartRecordingActions()
    showSheet(reviewState(), actions)

    composeRule.onNodeWithText(samples.food.nombre).performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_tag_remove, samples.food.nombre))
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf(0), actions.categoriaClears) }
  }

  @Test
  fun thePickerOpenedFromAPartChipPicksForThatPart() {
    val actions = PartRecordingActions()
    showSheet(reviewState(), actions)

    composeRule.onNodeWithText(string(Res.string.movimiento_part_no_category)).performClick()
    composeRule
        .onNodeWithContentDescription(string(Res.string.category_picker_single_title))
        .performClick()
    composeRule
        .onNode(
            hasText(samples.home.nombre) and hasAnyAncestor(hasTestTag("category_picker_results"))
        )
        .performClick()

    composeRule.runOnIdle {
      assertEquals(listOf(1 to samples.home.id), actions.categoriaSelections)
    }
  }

  @Test
  fun aPartFormSaysDoneAndHidesRepetir() {
    val actions = PartRecordingActions()
    showSheet(reviewState().copy(step = AddMovimientoStep.FORM, editingPart = 0), actions)

    composeRule.onNodeWithText(string(Res.string.recurrencia_repeat_label)).assertDoesNotExist()
    composeRule.onNodeWithContentDescription(string(Res.string.movimiento_part_done)).performClick()

    composeRule.runOnIdle {
      assertEquals(1, actions.partDones)
      assertEquals(0, actions.submits)
    }
  }

  private fun show(review: MovimientoReviewUi) {
    composeRule.setContent {
      TallybookTheme {
        MovimientoReviewStep(
            review = review,
            callbacks =
                MovimientoReviewCallbacks(
                    etiquetas =
                        MovimientoEtiquetasCallbacks(onAdd = {}, onRemove = {}, onRetry = {}),
                    onPartEdit = { edited += it },
                    onPartCategoria = { categoryRequests += it },
                    onPartRemoved = { removed += it },
                    onJoin = { joins++ },
                    onSave = { saves++ },
                ),
            today = MovimientoEntrySamples.today,
        )
      }
    }
  }

  private fun showSheet(state: AddMovimientoUiState, actions: PartRecordingActions) {
    composeRule.setContent {
      TallybookTheme {
        AddMovimientoSheet(visible = true, uiState = state, actions = actions, onDismiss = {})
      }
    }
  }

  private fun reviewState(): AddMovimientoUiState {
    val query = SuggestionQuery("", TipoMovimiento.GASTO)
    return AddMovimientoUiState(
        tipo = TipoMovimiento.GASTO,
        amountInput = "20000",
        fecha = MovimientoEntrySamples.today,
        primaryMoneda = Moneda.COP,
        cuentas = listOf(COP_CASH),
        cuentaChoice =
            CuentaSelectionState(MedioPago.EFECTIVO, COP_CASH.id, SelectionOwner.DEFAULT),
        categorias = samples.all,
        categoriaId = CATEGORIA_OTROS_ID,
        titulo = "",
        descripcion = "",
        isLoading = false,
        isSubmitting = false,
        submitAttempted = false,
        error = null,
        step = AddMovimientoStep.REVIEW,
        phrase = PART_ONE + " y " + PART_TWO,
        hasUnsavedChanges = true,
        etiquetasLoad = EtiquetasLoad.READY,
        parts =
            listOf(
                EntryPart(
                    segment = PART_ONE,
                    titulo = PART_ONE,
                    amountInput = "20000",
                    amountNeedsReview = false,
                    categoriaId = samples.food.id,
                    suggestionQuery = query,
                    suggestions = listOf(samples.cinema),
                ),
                EntryPart(
                    segment = PART_TWO,
                    titulo = PART_TWO,
                    amountInput = "30000",
                    amountNeedsReview = false,
                    categoriaId = null,
                    suggestionQuery = query,
                    suggestions = listOf(samples.transport),
                ),
            ),
    )
  }

  private fun total(review: MovimientoReviewUi): String = requireNotNull(review.total).format()

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private fun plural(resource: PluralStringResource, quantity: Int): String = runBlocking {
    getPluralString(resource, quantity, quantity)
  }

  private class PartRecordingActions : NoOpAddMovimientoActions() {
    val categoriaSelections = mutableListOf<Pair<Int, String>>()
    val categoriaClears = mutableListOf<Int>()
    var partDones = 0
    var submits = 0

    override fun onPartCategoriaSelected(index: Int, id: String) {
      categoriaSelections += index to id
    }

    override fun onPartCategoriaCleared(index: Int) {
      categoriaClears += index
    }

    override fun onPartDone() {
      partDones++
    }

    override fun onSubmit() {
      submits++
    }
  }

  private companion object {
    const val PART_ONE = "Compré 2 hamburguesas por 20mil"
    const val PART_TWO = "3 gaseosas fueron 30mil"
  }
}
