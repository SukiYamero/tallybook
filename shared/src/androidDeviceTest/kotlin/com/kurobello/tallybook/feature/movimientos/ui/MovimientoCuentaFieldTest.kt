package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaPickerSheet
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.QuickCuentaViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.compatibleCuentas
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
import tallybook.shared.generated.resources.account_create
import tallybook.shared.generated.resources.account_name_label
import tallybook.shared.generated.resources.entry_cuenta_create
import tallybook.shared.generated.resources.entry_cuenta_search
import tallybook.shared.generated.resources.entry_cuenta_with_moneda
import tallybook.shared.generated.resources.payment_method_cash
import tallybook.shared.generated.resources.payment_method_credit

@OptIn(ExperimentalResourceApi::class)
class MovimientoCuentaFieldTest {
  @get:Rule val composeRule = createComposeRule()

  private val samples = testEntrySamples()
  private val medios = mutableListOf<MedioPago>()
  private val cuentas = mutableListOf<String>()
  private var pickerRequests = 0
  private var createRequests = 0

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun theSelectedMedioIsAnnouncedAndAnotherOneIsReported() {
    show(samples.cashCuenta)

    composeRule.onNodeWithText(string(Res.string.payment_method_cash)).assertIsSelected()
    composeRule
        .onNodeWithText(string(Res.string.payment_method_credit))
        .assertIsNotSelected()
        .performClick()

    assertEquals(listOf(MedioPago.CREDITO), medios)
  }

  @Test
  fun aSingleCompatibleCuentaIsShownWithoutAPicker() {
    show(samples.cashCuenta)

    composeRule.onNodeWithText(samples.cash.nombre).assertIsDisplayed()
    composeRule.onNodeWithTag(CUENTA_SELECTOR_TAG).assertHasNoClickAction()
  }

  @Test
  fun severalCompatibleCuentasOpenThePicker() {
    show(samples.creditCuenta)

    composeRule.onNodeWithTag(CUENTA_SELECTOR_TAG).assertHasClickAction().performClick()

    assertEquals(1, pickerRequests)
  }

  @Test
  fun aNamedAlternativeIsOfferedAsASuggestion() {
    show(samples.creditCuenta)

    composeRule.onNodeWithText(samples.nequi.nombre).performClick()

    assertEquals(listOf(samples.nequi.id), cuentas)
  }

  @Test
  fun withoutACompatibleCuentaTheEmptyStateOffersCreatingOne() {
    show(samples.emptyCuenta, canCreate = true)

    composeRule.onNodeWithText(string(Res.string.entry_cuenta_create)).performClick()

    assertEquals(1, createRequests)
  }

  @Test
  fun aShortPickerMarksTheSelectionShowsForeignCodesAndReportsATap() {
    composeRule.setContent {
      TallybookTheme {
        CuentaPickerSheet(
            visible = true,
            cuentas = samples.creditCuenta.options,
            selectedId = samples.nu.id,
            onSelect = { cuentas += it },
            onDismiss = {},
            primaryMoneda = samples.creditCuenta.primaryMoneda,
        )
      }
    }
    val visa =
        string(Res.string.entry_cuenta_with_moneda, samples.visa.nombre, samples.visa.moneda.name)

    composeRule.onNodeWithText(samples.nu.nombre).assertIsSelected()
    composeRule.onNodeWithText(visa).performClick()

    assertEquals(listOf(samples.visa.id), cuentas)
    composeRule.onNodeWithText(string(Res.string.entry_cuenta_search)).assertDoesNotExist()
  }

  @Test
  fun anAccountCreatedInPlaceIsSelectedOnceTheCatalogListsIt() {
    val repository = FakeCuentaRepository(listOf(samples.cash))
    val quick = QuickCuentaViewModel(repository)
    composeRule.setContent {
      val catalog by repository.cuentas.collectAsState()
      val state by quick.uiState.collectAsState()
      val field = samples.emptyCuenta.copy(options = compatibleCuentas(catalog, MedioPago.DEBITO))
      val creation = CuentaCreation(state, quick, quick.effects, Moneda.COP)
      TallybookTheme {
        MovimientoCuentaField(
            field = field,
            callbacks =
                MovimientoCuentaCallbacks.Inert.copy(onCreateCuenta = { creation.start(field) }),
            enabled = true,
        )
        CuentaCreationSheet(creation, field, onSelect = { cuentas += it })
      }
    }

    composeRule.onNodeWithText(string(Res.string.entry_cuenta_create)).performClick()
    composeRule.waitForIdle()
    assertEquals(TipoCuenta.BANCO, quick.uiState.value.draft?.tipo)
    composeRule
        .onNode(hasSetTextAction() and hasText(string(Res.string.account_name_label)))
        .performTextInput("Nequi")
    composeRule.onNodeWithText(string(Res.string.account_create)).performClick()

    composeRule.waitUntil(5_000) { cuentas.isNotEmpty() }
    val created = repository.cuentas.value.single { it.nombre == "Nequi" }
    assertEquals(listOf(created.id), cuentas)
    assertEquals(Moneda.COP, created.moneda)
  }

  private fun show(field: MovimientoCuentaUi, canCreate: Boolean = false) {
    composeRule.setContent {
      TallybookTheme {
        MovimientoCuentaField(
            field = field,
            callbacks =
                MovimientoCuentaCallbacks(
                    onMedioSelected = { medios += it },
                    onCuentaSelected = { cuentas += it },
                    onOpenPicker = { pickerRequests++ },
                    onCreateCuenta = if (canCreate) ({ createRequests++ }) else null,
                ),
            enabled = true,
        )
      }
    }
  }

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }
}
