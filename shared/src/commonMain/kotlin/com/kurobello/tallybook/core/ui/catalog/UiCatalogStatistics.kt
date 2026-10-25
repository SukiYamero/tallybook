@file:Suppress("MagicNumber", "TooManyFunctions") // Fixed sample data for the catalog.

package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.ui.StatisticsScreen
import com.kurobello.tallybook.feature.statistics.viewmodel.CategoryDetailState
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsCatalog
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsContext
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsDimension
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsSelection
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsUiState
import com.kurobello.tallybook.feature.statistics.viewmodel.presentCategoryDetail
import com.kurobello.tallybook.feature.statistics.viewmodel.presentStatistics
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_compras
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_ocio
import tallybook.shared.generated.resources.category_salud
import tallybook.shared.generated.resources.category_supermercado
import tallybook.shared.generated.resources.category_transporte
import tallybook.shared.generated.resources.statistics_title

/** The screen keeps its own scrolling inside the catalog, in a viewport shorter than the window. */
private const val SCREEN_HEIGHT_FRACTION = 0.8f

internal fun LazyListScope.catalogStatistics() {
  catalogControlSection("statistics-screen", { stringResource(Res.string.statistics_title) }) {
    CatalogStatisticsScreen()
  }
}

@Composable
private fun CatalogStatisticsScreen() {
  val names = catalogStatisticsNames()
  val sample = remember(names) { CatalogStatisticsSample(names) }
  val windowHeight = LocalWindowInfo.current.containerSize.height
  val height = with(LocalDensity.current) { (windowHeight * SCREEN_HEIGHT_FRACTION).toDp() }
  Box(modifier = Modifier.fillMaxWidth().height(height)) {
    StatisticsScreen(sample.state, sample, TallybookTheme.spacing.space0)
  }
}

@Composable
private fun catalogStatisticsNames() =
    mapOf(
        "comida" to stringResource(Res.string.category_comida),
        "super" to stringResource(Res.string.category_supermercado),
        "hogar" to stringResource(Res.string.category_hogar),
        "transporte" to stringResource(Res.string.category_transporte),
        "ocio" to stringResource(Res.string.category_ocio),
        "salud" to stringResource(Res.string.category_salud),
        "compras" to stringResource(Res.string.category_compras),
    )

/** A fixed October 24th read whose dimension tabs, months and category sheet respond locally. */
@Stable
private class CatalogStatisticsSample(names: Map<String, String>) : StatisticsActions {
  private val catalog =
      StatisticsCatalog(
          categorias =
              names.mapValues { (id, nombre) ->
                Categoria(
                    id = id,
                    nombre = nombre,
                    padreId = "comida".takeIf { id == "super" },
                    icono = CatalogIcons.getValue(id),
                    color = CatalogTints.getValue(id),
                )
              },
          etiquetas = mapOf("trabajo" to "Trabajo", "viaje" to "Viaje"),
          cuentas = mapOf("banco" to "Banco", "efectivo" to "Efectivo"),
      )
  private val context =
      StatisticsContext.of(StatisticsSelection.initial(CATALOG_TODAY), Moneda.ARS, CATALOG_TODAY)
  private val content = presentStatistics(catalogSnapshot(), context, catalog)

  var state by
      mutableStateOf(
          StatisticsUiState(
              selection = StatisticsSelection.initial(CATALOG_TODAY),
              today = CATALOG_TODAY,
              years = listOf(CATALOG_TODAY.year),
              moneda = Moneda.ARS,
              content = RetainedLoadState.Ready(content),
          )
      )
    private set

  override fun onDimensionSelected(dimension: StatisticsDimension) {
    state = state.copy(dimension = dimension)
  }

  override fun onTrendMonthSelected(index: Int) {
    state = state.copy(trendSelection = index)
  }

  override fun onShareSelected(key: StatisticsKey) {
    if (key !is StatisticsKey.Categoria || state.detail != null) return
    val detail = presentCategoryDetail(catalogCategory(), key.id, context, catalog, content)
    state = state.copy(detail = CategoryDetailState(key.id, RetainedLoadState.Ready(detail)))
  }

  override fun onDetailDismissed() {
    state = state.copy(detail = state.detail?.copy(isClosing = true))
  }

  override fun onViewMovimientos() = onDetailDismissed()

  override fun onDetailHidden() {
    state = state.copy(detail = null)
  }

  override fun onScopeChanged(scope: StatisticsScope) = Unit

  override fun onPeriodSelected(period: MovimientoDateRange) = Unit

  override fun onYearSelected(year: Int) = Unit

  override fun onMonedaSelected(moneda: Moneda) = Unit

  override fun onAddMovimiento() = Unit

  override fun onRetry() = Unit

  override fun onResume() = Unit
}

private val CATALOG_TODAY = LocalDate(2026, 10, 24)

private val CatalogIcons =
    mapOf(
        "comida" to CategoryIcon.UTENSILS,
        "super" to CategoryIcon.SHOPPING_CART,
        "hogar" to CategoryIcon.HOUSE,
        "transporte" to CategoryIcon.BUS,
        "ocio" to CategoryIcon.GAMEPAD,
        "salud" to CategoryIcon.HEART_PULSE,
        "compras" to CategoryIcon.SHOPPING_BAG,
    )

private val CatalogTints =
    mapOf(
        "comida" to CategoryTint.AMBER,
        "super" to CategoryTint.AMBER,
        "hogar" to CategoryTint.BLUE,
        "transporte" to CategoryTint.INFO,
        "ocio" to CategoryTint.ROSE,
        "salud" to CategoryTint.EMERALD,
        "compras" to CategoryTint.PURPLE,
    )

private fun ars(units: Long) = Money(units * ARS_SCALE, Moneda.ARS)

private fun total(key: StatisticsKey, current: Long, previous: Long) =
    DimensionTotal(key, ars(current), ars(previous))

private fun catalogSnapshot() =
    StatisticsSnapshot(
        current = MovimientoSummary(ars(1_250_000), ars(612_400)),
        previous = MovimientoSummary(ars(1_250_000), ars(548_200)),
        categorias =
            listOf(
                total(StatisticsKey.Categoria("comida"), 180_700, 153_100),
                total(StatisticsKey.Categoria("hogar"), 135_300, 135_300),
                total(StatisticsKey.Categoria("transporte"), 81_500, 89_600),
                total(StatisticsKey.Categoria("ocio"), 70_400, 52_500),
                total(StatisticsKey.Categoria("salud"), 53_900, 51_300),
                total(StatisticsKey.Categoria("compras"), 50_200, 64_400),
                total(StatisticsKey.Categoria("missing"), 40_400, 2_000),
            ),
        etiquetas =
            listOf(
                total(StatisticsKey.Etiqueta("trabajo"), 92_300, 82_400),
                total(StatisticsKey.Etiqueta("viaje"), 64_800, 0),
            ),
        cuentas =
            listOf(
                total(StatisticsKey.Cuenta("banco"), 513_700, 470_000),
                total(StatisticsKey.Cuenta("efectivo"), 98_700, 78_200),
            ),
        medios =
            listOf(
                total(StatisticsKey.Medio(MedioPago.DEBITO), 289_900, 278_000),
                total(StatisticsKey.Medio(MedioPago.CREDITO), 172_500, 139_000),
                total(StatisticsKey.Medio(MedioPago.EFECTIVO), 98_700, 78_200),
                total(StatisticsKey.Medio(MedioPago.TRANSFERENCIA), 51_300, 53_000),
            ),
        months =
            CatalogMonthGastos.mapIndexed { index, gastos ->
              MonthTotals(
                  YearMonth(CATALOG_TODAY.year, FIRST_MONTH + index),
                  MovimientoSummary(ars(1_250_000), ars(gastos)),
              )
            },
        daily =
            CatalogDailyGastos.mapIndexed { index, gasto ->
              DayGasto(LocalDate(CATALOG_TODAY.year, CATALOG_TODAY.month, index + 1), ars(gasto))
            },
        hasAnyMovimiento = true,
    )

private fun catalogCategory() =
    CategoryStatisticsSnapshot(
        current = ars(180_700),
        previous = ars(153_100),
        count = 19,
        subcategorias =
            listOf(
                total(StatisticsKey.Categoria("super"), 98_400, 90_000),
                total(StatisticsKey.Categoria("comida"), 82_300, 63_100),
            ),
        months =
            CatalogCategoryMonths.mapIndexed { index, gasto ->
              YearMonth(CATALOG_TODAY.year, FIRST_MONTH + index) to ars(gasto)
            },
        largest = emptyList(),
    )

private const val ARS_SCALE = 100L
private const val FIRST_MONTH = 5
private val CatalogMonthGastos = listOf(705_000L, 742_000, 720_000, 690_000, 660_000, 612_400)
private val CatalogCategoryMonths = listOf(168_000L, 172_000, 181_000, 160_000, 175_000, 180_700)
private val CatalogDailyGastos =
    listOf(9, 35, 16, 7, 22, 46, 13, 18, 29, 70, 20, 11, 38, 8, 25, 15, 32, 14, 22, 53, 19, 28, 42)
        .map { it * 1_000L } + 20_400L
