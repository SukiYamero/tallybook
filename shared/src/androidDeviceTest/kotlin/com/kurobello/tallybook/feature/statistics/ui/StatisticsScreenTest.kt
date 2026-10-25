package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.CategoryDetailState
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsCatalog
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsContext
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsDimension
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsSelection
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsUiState
import com.kurobello.tallybook.feature.statistics.viewmodel.presentCategoryDetail
import com.kurobello.tallybook.feature.statistics.viewmodel.presentStatistics
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.payment_method_debit
import tallybook.shared.generated.resources.statistics_detail_view
import tallybook.shared.generated.resources.statistics_dimension_etiquetas
import tallybook.shared.generated.resources.statistics_scope_month
import tallybook.shared.generated.resources.statistics_tags_note
import tallybook.shared.generated.resources.statistics_title

class StatisticsScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun theCurrencySelectorReportsTheChosenCurrency() {
    val actions = RecordingActions()
    show(state(), actions)

    composeRule.onNodeWithText(Moneda.COP.name).performClick()

    composeRule.runOnIdle { assertEquals(listOf<Any>(Moneda.COP), actions.calls) }
  }

  @Test
  fun dimensionTabsReportTheirDimensionAndTagsExplainTheirShares() {
    val actions = RecordingActions()
    val screen = show(state(), actions)
    scrollTo(localized(Res.string.statistics_dimension_etiquetas))

    composeRule.onNodeWithText(localized(Res.string.statistics_dimension_etiquetas)).performClick()
    composeRule.runOnIdle {
      assertEquals(listOf<Any>(StatisticsDimension.Etiquetas), actions.calls)
    }

    screen.value = state().copy(dimension = StatisticsDimension.Etiquetas)
    scrollTo(localized(Res.string.statistics_tags_note))
    composeRule.onNodeWithText(localized(Res.string.statistics_tags_note)).assertIsDisplayed()
  }

  @Test
  fun categoryRowsOpenTheirKeyAndMedioRowsAreNotClickable() {
    val actions = RecordingActions()
    val screen = show(state(), actions)
    scrollTo("Comida")

    composeRule.onNode(hasText("Comida") and hasClickAction()).performClick()
    composeRule.runOnIdle {
      assertEquals(listOf<Any>(StatisticsKey.Categoria("comida")), actions.calls)
    }

    screen.value = state().copy(dimension = StatisticsDimension.Medios)
    val debit = localized(Res.string.payment_method_debit)
    scrollTo(debit)
    composeRule.onNodeWithText(debit, useUnmergedTree = true).assertHasNoClickAction()
  }

  @Test
  fun theSheetOffersItsMovimientos() {
    val actions = RecordingActions()
    val detail =
        presentCategoryDetail(
            CategoryStatisticsSnapshot(
                current = usd(1_000),
                previous = null,
                count = 4,
                subcategorias = emptyList(),
                months = emptyList(),
                largest = emptyList(),
            ),
            "comida",
            context,
            catalog,
            content,
        )
    show(
        state().copy(detail = CategoryDetailState("comida", RetainedLoadState.Ready(detail))),
        actions,
    )
    val view = runBlocking { getPluralString(Res.plurals.statistics_detail_view, 4, 4) }

    composeRule.onNode(hasText(view) and hasClickAction()).assertHasClickAction().performClick()

    composeRule.runOnIdle { assertEquals(listOf<Any>("viewMovimientos"), actions.calls) }
  }

  @Test
  fun theScopeControlMarksTheSelectedScope() {
    show(state(), RecordingActions())

    composeRule.onNode(hasText(scopeMonth()) and hasClickAction()).assertIsSelected()
  }

  private fun show(
      initial: StatisticsUiState,
      actions: StatisticsActions,
  ): MutableState<StatisticsUiState> {
    val state = mutableStateOf(initial)
    composeRule.setContent { TallybookTheme { StatisticsScreen(state.value, actions, 0.dp) } }
    return state
  }

  private fun scrollTo(text: String) {
    composeRule
        .onNodeWithContentDescription(localized(Res.string.statistics_title))
        .performScrollToNode(hasText(text))
  }

  private val today = LocalDate(2026, 10, 24)
  private val catalog =
      StatisticsCatalog(
          categorias =
              mapOf(
                  "comida" to
                      Categoria(
                          "comida",
                          "Comida",
                          null,
                          CategoryIcon.UTENSILS,
                          CategoryTint.AMBER,
                      )
              ),
          etiquetas = mapOf("trip" to "Viaje"),
          cuentas = emptyMap(),
      )
  private val context = StatisticsContext.of(StatisticsSelection.initial(today), Moneda.USD, today)
  private val content =
      presentStatistics(
          StatisticsSnapshot(
              current = MovimientoSummary(usd(0), usd(1_000)),
              previous = null,
              categorias =
                  listOf(DimensionTotal(StatisticsKey.Categoria("comida"), usd(1_000), null)),
              etiquetas = listOf(DimensionTotal(StatisticsKey.Etiqueta("trip"), usd(400), null)),
              cuentas = emptyList(),
              medios =
                  listOf(DimensionTotal(StatisticsKey.Medio(MedioPago.DEBITO), usd(1_000), null)),
              months = emptyList(),
              daily = emptyList(),
              hasAnyMovimiento = true,
          ),
          context,
          catalog,
      )

  private fun state() =
      StatisticsUiState(
          selection = StatisticsSelection.initial(today),
          today = today,
          years = listOf(today.year),
          moneda = Moneda.USD,
          monedas = listOf(Moneda.COP, Moneda.USD),
          content = RetainedLoadState.Ready(content),
      )

  private fun usd(units: Long) = Money(units, Moneda.USD)

  private fun scopeMonth() = localized(Res.string.statistics_scope_month)

  private fun localized(resource: StringResource) = runBlocking { getString(resource) }
}

private class RecordingActions : StatisticsActions {
  val calls = mutableListOf<Any>()

  override fun onScopeChanged(scope: StatisticsScope) {
    calls += scope
  }

  override fun onPeriodSelected(period: MovimientoDateRange) {
    calls += period
  }

  override fun onYearSelected(year: Int) {
    calls += year
  }

  override fun onMonedaSelected(moneda: Moneda) {
    calls += moneda
  }

  override fun onDimensionSelected(dimension: StatisticsDimension) {
    calls += dimension
  }

  override fun onTrendMonthSelected(index: Int) {
    calls += index
  }

  override fun onShareSelected(key: StatisticsKey) {
    calls += key
  }

  override fun onDetailDismissed() {
    calls += "dismissed"
  }

  override fun onDetailHidden() {
    calls += "hidden"
  }

  override fun onViewMovimientos() {
    calls += "viewMovimientos"
  }

  override fun onAddMovimiento() {
    calls += "add"
  }

  override fun onRetry() {
    calls += "retry"
  }

  override fun onResume() {
    calls += "resume"
  }
}
