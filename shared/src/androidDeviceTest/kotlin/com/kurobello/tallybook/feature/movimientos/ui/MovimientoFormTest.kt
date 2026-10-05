package com.kurobello.tallybook.feature.movimientos.ui

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.description_optional
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.more_fields
import tallybook.shared.generated.resources.movimiento_amount_expense_prompt
import tallybook.shared.generated.resources.movimiento_amount_income_prompt
import tallybook.shared.generated.resources.movimiento_category_expense_prompt
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.movimiento_save_error
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.save_income

@OptIn(ExperimentalResourceApi::class)
class MovimientoFormTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun typeAndFieldChangesReachActions() {
    var tipo by mutableStateOf(TipoMovimiento.GASTO)
    val actions = RecordingActions(onTipoChangedCallback = { tipo = it })
    val incomeLabel = localizedString(Res.string.income)
    val saveIncomeLabel = localizedString(Res.string.save_income)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(state(tipo = tipo, selectedCategoriaId = category.id), actions)
      }
    }

    composeRule.onNodeWithText(incomeLabel).performClick()
    composeRule.onNodeWithContentDescription(saveIncomeLabel).assertIsDisplayed()
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_income_prompt))
        .performTextInput("12,50")

    composeRule.runOnIdle {
      assertEquals(TipoMovimiento.INGRESO, actions.tipo)
      assertEquals("12,50", actions.amount)
    }
  }

  @Test
  fun emptySubmitShowsAmountAndCategoryErrorsWithoutDismissing() {
    val actions = RecordingActions()
    val invalidState = state(submitAttempted = true)
    val amountRequired = localizedString(Res.string.amount_required)
    val categoryRequired = localizedString(Res.string.category_required)

    composeRule.setContent { TallybookTheme { MovimientoForm(invalidState, actions) } }

    composeRule.onNodeWithText(amountRequired).assertIsDisplayed()
    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                categoryRequired,
            )
        )
        .assertIsDisplayed()
  }

  @Test
  fun invalidAmountReceivesFocusAndAnnouncesItsSpecificError() {
    val amountRequired = localizedString(Res.string.amount_required)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            state(submitAttempted = true, selectedCategoriaId = category.id),
            RecordingActions(),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_expense_prompt))
        .assertIsFocused()
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                amountRequired,
            )
        )
  }

  @Test
  fun missingCategoryReceivesFocusOnTheCategoryField() {
    val categoryRequired = localizedString(Res.string.category_required)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            state(amountInput = "1", submitAttempted = true),
            RecordingActions(),
        )
      }
    }

    composeRule
        .onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                categoryRequired,
            )
        )
        .assertIsFocused()
  }

  @Test
  fun openingTheCategoryPickerClearsFocusFromThePreviousTextField() {
    val categoryLabel = localizedString(Res.string.category)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(state(selectedCategoriaId = category.id), RecordingActions())
      }
    }

    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_expense_prompt))
        .performClick()
        .assertIsFocused()
    composeRule.onNodeWithText(categoryLabel).performClick()

    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_expense_prompt))
        .assertIsNotFocused()
  }

  @Test
  fun newEntryStartsWithCategoryAndSelectionFocusesAmount() {
    var selectedId by mutableStateOf<String?>(null)
    val actions =
        object : AddMovimientoActions by RecordingActions() {
          override fun onCategoriaSelected(id: String) {
            selectedId = id
          }
        }
    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            uiState = state(selectedCategoriaId = selectedId),
            actions = actions,
            startWithCategory = true,
        )
      }
    }
    composeRule
        .onNodeWithText(localizedString(Res.string.movimiento_category_expense_prompt))
        .assertIsDisplayed()
    composeRule.onNodeWithText(category.nombre).performClick()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_expense_prompt))
        .assertIsFocused()
    composeRule.runOnIdle { assertEquals(category.id, selectedId) }
  }

  @Test
  fun amountCanBeEditedWithoutCategorySelection() {
    val actions = RecordingActions()
    composeRule.setContent { TallybookTheme { MovimientoForm(state(), actions) } }
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.movimiento_amount_expense_prompt))
        .assertIsEnabled()
        .performTextInput("12.34")

    composeRule.runOnIdle { assertEquals("12.34", actions.amount) }
  }

  @Test
  fun correctingAmountAfterValidationDoesNotMoveFocusToMissingCategory() {
    var amountInput by mutableStateOf("")
    val actions =
        object : AddMovimientoActions by RecordingActions() {
          override fun onAmountChanged(value: String) {
            amountInput = value
          }
        }
    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            uiState = state(amountInput = amountInput, submitAttempted = true),
            actions = actions,
        )
      }
    }
    val amount =
        composeRule.onNodeWithContentDescription(
            localizedString(Res.string.movimiento_amount_expense_prompt)
        )

    amount.performClick().performTextInput("1")
    amount.assertIsFocused().performTextInput("2")

    composeRule.runOnIdle { assertEquals("12", amountInput) }
  }

  @Test
  fun cancelingCategoryAfterAnAmountErrorDoesNotRefocusAmount() {
    val amountPrompt = localizedString(Res.string.movimiento_amount_expense_prompt)
    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            uiState = state(selectedCategoriaId = category.id, submitAttempted = true),
            actions = RecordingActions(),
        )
      }
    }
    composeRule.onNodeWithContentDescription(amountPrompt).assertIsFocused()
    composeRule.onNodeWithText(localizedString(Res.string.category)).performClick()
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription(amountPrompt).assertIsNotFocused()
  }

  @Test
  fun descriptionDisclosureLimitsInputTo180Characters() {
    var descripcion by mutableStateOf("x".repeat(179))
    var detailsExpanded by mutableStateOf(false)
    val actions = RecordingActions(onDescripcionChangedCallback = { descripcion = it })
    val moreFields = localizedString(Res.string.more_fields)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            uiState = state(descripcion = descripcion),
            actions = actions,
            detailsExpanded = detailsExpanded,
            onDetailsExpandedChange = { detailsExpanded = it },
        )
      }
    }

    composeRule.onNodeWithText(moreFields).performClick()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithContentDescription(localizedString(Res.string.description_optional))
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()
        .assertIsFocused()
        .performTextInput("xx")

    composeRule.runOnIdle {
      assertEquals(180, actions.descripcion.length)
      assertEquals(180, descripcion.length)
    }
    composeRule.onNodeWithText("180/180").assertIsDisplayed()
  }

  @Test
  fun descriptionRemainsComposedUntilItsExitAnimationCompletes() {
    val moreFields = localizedString(Res.string.more_fields)
    val description = localizedString(Res.string.description_optional)
    var detailsExpanded by mutableStateOf(false)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            uiState = state(),
            actions = RecordingActions(),
            detailsExpanded = detailsExpanded,
            onDetailsExpandedChange = { detailsExpanded = it },
        )
      }
    }

    composeRule.onNodeWithText(moreFields).performClick()
    composeRule.waitForIdle()
    composeRule.mainClock.autoAdvance = false
    composeRule.onNodeWithText(moreFields).performClick()

    composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
    composeRule.mainClock.advanceTimeBy(200)
    composeRule.onNodeWithContentDescription(description).assertDoesNotExist()
  }

  @Test
  fun overflowShowsTheAmountError() {
    val amountOverflow = localizedString(Res.string.amount_overflow)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            state(amountInput = "999999999999999999999", submitAttempted = true),
            RecordingActions(),
        )
      }
    }

    composeRule.onNodeWithText(amountOverflow).assertIsDisplayed()
  }

  @Test
  fun futureDateIsDisabledAndDoesNotChangeTheSelectedDate() {
    val selectedDate = LocalDate(2026, 9, 15)
    val futureDate = LocalDate(2026, 9, 16)
    var changedDate: LocalDate? = null
    val dateLabel = localizedString(Res.string.date)

    composeRule.setContent {
      TallybookTheme {
        MovimientoDateField(
            state =
                MovimientoDateFieldState(
                    fecha = selectedDate,
                    enabled = true,
                    maxSelectableDate = selectedDate,
                ),
            onFechaChanged = { changedDate = it },
        )
      }
    }

    composeRule.onNodeWithContentDescription(dateLabel).performClick()
    composeRule.onNodeWithText(futureDate.day.toString()).assertIsNotEnabled()

    composeRule.runOnIdle { assertEquals(null, changedDate) }
  }

  @Test
  fun loadAndSaveErrorsKeepTheExistingValuesAndOfferRetry() {
    val actions = RecordingActions()
    val preferencesError = localizedString(Res.string.movimiento_preferences_error)
    val saveError = localizedString(Res.string.movimiento_save_error)
    val retryLabel = localizedString(Res.string.retry)
    var error by mutableStateOf(MovimientoFormError.PREFERENCES_UNAVAILABLE)

    composeRule.setContent {
      TallybookTheme {
        MovimientoForm(
            state(
                amountInput = "10",
                selectedCategoriaId = category.id,
                error = error,
            ),
            actions,
        )
      }
    }

    composeRule.onNodeWithText(preferencesError).assertIsDisplayed()
    composeRule.onNodeWithText(retryLabel).performClick()
    composeRule.onNodeWithText("10").assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(1, actions.retryCount) }

    composeRule.runOnIdle { error = MovimientoFormError.SAVE_FAILED }
    composeRule.waitForIdle()

    composeRule.onNodeWithText(saveError).assertIsDisplayed()
    composeRule.onNodeWithText("10").assertIsDisplayed()
  }

  private fun localizedString(resource: StringResource): String = runBlocking {
    getString(resource)
  }

  private companion object {
    val category =
        Categoria(
            id = "food",
            nombre = "Comida",
            padreId = null,
            icono = CategoryIcon.UTENSILS,
            color = CategoryTint.AMBER,
        )

    fun state(
        tipo: TipoMovimiento = TipoMovimiento.GASTO,
        amountInput: String = "",
        selectedCategoriaId: String? = null,
        descripcion: String = "",
        submitAttempted: Boolean = false,
        error: MovimientoFormError? = null,
    ) =
        AddMovimientoUiState(
            tipo = tipo,
            amountInput = amountInput,
            fecha = LocalDate(2026, 9, 15),
            moneda = Moneda.COP,
            categorias = listOf(category),
            selectedCategoriaId = selectedCategoriaId,
            descripcion = descripcion,
            isLoading = false,
            isSubmitting = false,
            submitAttempted = submitAttempted,
            error = error,
        )
  }

  private class RecordingActions(
      private val onTipoChangedCallback: (TipoMovimiento) -> Unit = {},
      private val onDescripcionChangedCallback: (String) -> Unit = {},
  ) : AddMovimientoActions {
    var tipo: TipoMovimiento? = null
    var amount = ""
    var descripcion = ""
    var retryCount = 0

    override fun onTipoChanged(tipo: TipoMovimiento) {
      this.tipo = tipo
      onTipoChangedCallback(tipo)
    }

    override fun onAmountChanged(value: String) {
      amount = value
    }

    override fun onFechaChanged(value: LocalDate) = Unit

    override fun onCategoriaSelected(id: String) = Unit

    override fun onDescripcionChanged(value: String) {
      descripcion = value
      onDescripcionChangedCallback(value)
    }

    override fun onDiscardChanges() = Unit

    override fun onRetryLoad() {
      retryCount += 1
    }

    override fun onSubmit() = Unit
  }
}
