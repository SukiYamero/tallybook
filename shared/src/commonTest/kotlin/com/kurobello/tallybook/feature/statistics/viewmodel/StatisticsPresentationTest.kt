package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryStatisticsSnapshot
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.DayGasto
import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MonthTotals
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.model.StatisticsSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class StatisticsPresentationTest {
  @Test
  fun rankColorsSkipTheRedPaletteEntryAndRunOutAfterThePalette() {
    assertEquals(
        listOf(0, 1, 2, 4, 5, 6, 7, 8, null),
        (0..8).map { chartPaletteIndex(it, PALETTE_SIZE) },
    )
  }

  @Test
  fun exclusiveGroupsPastTheDonutJoinOtrasInTheNeutralColor() {
    val totals =
        (1..8).map { DimensionTotal(cat("c$it"), clp(1_000L * (10 - it)), null) } +
            DimensionTotal(cat("fell"), clp(0), clp(500))
    val content = present(snapshot(categorias = totals))
    val distribution = content.distributions.categorias

    val donut = assertNotNull(distribution.donut)
    assertEquals(6, donut.size)
    assertEquals((0..4).map { StatisticsColor.Rank(it) }, donut.take(5).map { it.color })
    assertEquals(StatisticsLabel.Otras, donut.last().label)
    assertEquals(StatisticsColor.Otras, donut.last().color)
    assertEquals(10_000, donut.sumOf { it.basisPoints })

    assertEquals((1..8).map { cat("c$it") }, distribution.rows.map { it.key })
    assertEquals(
        (0..4).map { StatisticsColor.Rank(it) } + List(3) { StatisticsColor.Otras },
        distribution.rows.map { it.color },
    )
    assertEquals(10_000, distribution.rows.sumOf { it.basisPoints })
  }

  @Test
  fun keysWithoutCurrentGastoNeverReachTheDonutOrTheRows() {
    val content =
        present(
            snapshot(
                cuentas =
                    listOf(
                        DimensionTotal(StatisticsKey.Cuenta("visa"), clp(3_000), clp(1_000)),
                        DimensionTotal(StatisticsKey.Cuenta("cash"), clp(0), clp(2_000)),
                    )
            )
        )

    val cuentas = content.distributions.cuentas
    assertEquals(listOf(StatisticsKey.Cuenta("visa")), cuentas.rows.map { it.key })
    assertEquals(listOf(10_000), cuentas.donut?.map { it.basisPoints })
    assertEquals(StatisticsLabel.Named("Visa"), cuentas.rows.single().label)
  }

  @Test
  fun tagsAreMeasuredAgainstTheWholeGastoAndNeverDrawADonut() {
    val content =
        present(
            snapshot(
                gastos = clp(10_000),
                etiquetas =
                    listOf(
                        DimensionTotal(StatisticsKey.Etiqueta("trip"), clp(6_000), null),
                        DimensionTotal(StatisticsKey.Etiqueta("work"), clp(5_000), null),
                    ),
            )
        )

    val etiquetas = content.distributions.etiquetas
    assertNull(etiquetas.donut)
    assertEquals(listOf(6_000, 5_000), etiquetas.rows.map { it.basisPoints })
    assertEquals(
        listOf(StatisticsColor.Rank(0), StatisticsColor.Rank(1)),
        etiquetas.rows.map { it.color },
    )
    assertEquals(StatisticsLabel.Named("Viaje"), etiquetas.rows.first().label)
    assertEquals(StatisticsLabel.UnknownEtiqueta, etiquetas.rows.last().label)
  }

  @Test
  fun changeIsHiddenWithoutAPreviousGasto() {
    val withoutPrevious =
        present(
            snapshot(
                previous = null,
                medios =
                    listOf(DimensionTotal(StatisticsKey.Medio(MedioPago.DEBITO), clp(10), null)),
            )
        )
    assertNull(withoutPrevious.summary.change)
    assertNull(withoutPrevious.summary.previousGastos)
    assertNull(withoutPrevious.distributions.medios.rows.single().change)
    assertEquals(
        StatisticsLabel.Medio(MedioPago.DEBITO),
        withoutPrevious.distributions.medios.rows.single().label,
    )

    val zeroPrevious = present(snapshot(previous = MovimientoSummary(clp(500), clp(0))))
    assertNull(zeroPrevious.summary.change)
    assertNull(zeroPrevious.summary.previousGastos)

    val withPrevious = present(snapshot(gastos = clp(1_120), previous = summary(clp(1_000))))
    assertEquals(Change(12, Direction.Up), withPrevious.summary.change)
    assertEquals("$1.000", withPrevious.summary.previousGastos)
  }

  @Test
  fun theMonthInProgressIsPartialAndComparedWithTheSameStretch() {
    val current = present(snapshot(months = trendMonths()))
    assertEquals(
        listOf(null, null, TODAY),
        current.trend.map { it.partialEnd },
    )
    assertTrue(current.summary.partial)
    assertEquals(
        StatisticsComparison.MonthStretch(YearMonth(2026, 9), LocalDate(2026, 9, 24)),
        current.summary.comparison,
    )

    val september = LocalDate(2026, 9, 10)
    val historical = present(snapshot(months = trendMonths()), anchor = september)
    assertTrue(historical.trend.none { it.partial })
    assertEquals(false, historical.summary.partial)
    assertEquals(
        StatisticsComparison.WholeMonth(YearMonth(2026, 8)),
        historical.summary.comparison,
    )

    val year = present(snapshot(), scope = StatisticsScope.Year)
    assertEquals(StatisticsComparison.YearStretch(LocalDate(2025, 10, 24)), year.summary.comparison)
  }

  @Test
  fun amountsAreFormattedInTheViewedCurrency() {
    val usd =
        present(
            snapshot(
                gastos = Money(61_240, Moneda.USD),
                ingresos = Money(125_000, Moneda.USD),
                previous = null,
                months = emptyList(),
                daily = emptyList(),
            ),
            moneda = Moneda.USD,
        )
    assertEquals(Moneda.USD, usd.moneda)
    assertEquals("$612.40", usd.summary.gastos)
    assertEquals("+$1,250.00", usd.summary.ingresos)
    assertEquals("+$637.60", usd.summary.balance)
    assertEquals("$612.40", usd.distributions.categorias.total)

    val negative = present(snapshot(gastos = clp(2_000), ingresos = clp(500)))
    assertEquals("-$1.500", negative.summary.balance)
  }

  @Test
  fun paceAppearsOnlyForTheCurrentMonthWithGasto() {
    val daily = (1..24).map { DayGasto(LocalDate(2026, 10, it), clp(100)) }
    val months = trendMonths()
    val current = present(snapshot(gastos = clp(2_400), months = months, daily = daily))
    val pace = assertNotNull(current.pace)
    assertEquals(24, pace.cumulative.size)
    assertEquals(31, pace.daysInMonth)
    assertEquals("$3.100", pace.projectionText)

    assertNull(present(snapshot(gastos = clp(0), months = months, daily = daily)).pace)
    assertNull(present(snapshot(months = months, daily = daily), scope = StatisticsScope.Year).pace)
  }

  @Test
  fun insightsNameTheirCategoria() {
    val content =
        present(
            snapshot(
                gastos = clp(10_000),
                categorias = listOf(DimensionTotal(cat("ocio"), clp(10_000), clp(5_000))),
            )
        )
    val rose = assertIs<StatisticsInsight.CategoryRose>(content.insights.first())
    assertEquals(StatisticsLabel.Named("Ocio"), rose.label)
    assertEquals(100, rose.percent)
    assertEquals("$5.000", rose.difference)
  }

  @Test
  fun theDetailNamesTheRootGeneralAndAveragesCompleteMonths() {
    val main =
        present(
            snapshot(
                gastos = clp(4_000),
                categorias =
                    listOf(
                        DimensionTotal(cat("ocio"), clp(3_000), null),
                        DimensionTotal(cat("comida"), clp(1_000), null),
                    ),
            )
        )
    val detail =
        presentCategoryDetail(
            CategoryStatisticsSnapshot(
                current = clp(1_000),
                previous = clp(0),
                count = 4,
                subcategorias =
                    listOf(
                        DimensionTotal(cat("comida"), clp(600), null),
                        DimensionTotal(cat("super"), clp(400), null),
                        DimensionTotal(cat("gone"), clp(0), clp(50)),
                    ),
                months =
                    listOf(
                        YearMonth(2026, 8) to clp(900),
                        YearMonth(2026, 9) to clp(1_200),
                        YearMonth(2026, 10) to clp(1_000),
                    ),
                largest = emptyList(),
            ),
            "comida",
            context(),
            CATALOG,
            main,
        )

    assertEquals(StatisticsLabel.Named("Comida"), detail.label)
    assertEquals(StatisticsColor.Rank(1), detail.color)
    assertEquals(2_500, detail.summary.shareBasisPoints)
    assertNull(detail.summary.change)
    assertEquals(listOf(false, false, true), detail.months.map { it.partial })
    assertEquals(CategoryAverage(1_050.0, "$1.050"), detail.average)
    assertEquals(
        listOf(StatisticsLabel.General, StatisticsLabel.Named("Super")),
        detail.subcategorias.map { it.label },
    )
    assertEquals(listOf(6_000, 4_000), detail.subcategorias.map { it.basisPoints })
  }

  @Test
  fun aMissingCategoriaKeepsCountingUnderTheUnknownLabel() {
    val content =
        present(snapshot(categorias = listOf(DimensionTotal(cat("deleted"), clp(100), null))))
    assertEquals(
        StatisticsLabel.UnknownCategoria,
        content.distributions.categorias.rows.single().label,
    )
  }
}

private const val PALETTE_SIZE = 9
private val TODAY = LocalDate(2026, 10, 24)

private fun clp(units: Long) = Money(units, Moneda.CLP)

private fun cat(id: String) = StatisticsKey.Categoria(id)

private fun summary(gastos: Money) = MovimientoSummary(Money(0, gastos.moneda), gastos)

private fun categoria(id: String, nombre: String, padreId: String? = null) =
    Categoria(id, nombre, padreId, CategoryIcon.UTENSILS, CategoryTint.AMBER)

private val CATALOG =
    StatisticsCatalog(
        categorias =
            listOf(
                    categoria("comida", "Comida"),
                    categoria("super", "Super", padreId = "comida"),
                    categoria("ocio", "Ocio"),
                )
                .associateBy { it.id } + (1..8).associate { "c$it" to categoria("c$it", "C$it") },
        etiquetas = mapOf("trip" to "Viaje"),
        cuentas = mapOf("visa" to "Visa"),
    )

private fun trendMonths() =
    listOf(8, 9, 10).map { MonthTotals(YearMonth(2026, it), summary(clp(1_000L * it))) }

@Suppress("LongParameterList")
private fun snapshot(
    gastos: Money = clp(1_000),
    ingresos: Money = Money(0, gastos.moneda),
    previous: MovimientoSummary? = summary(clp(900)),
    categorias: List<DimensionTotal> = emptyList(),
    etiquetas: List<DimensionTotal> = emptyList(),
    cuentas: List<DimensionTotal> = emptyList(),
    medios: List<DimensionTotal> = emptyList(),
    months: List<MonthTotals> = emptyList(),
    daily: List<DayGasto> = emptyList(),
) =
    StatisticsSnapshot(
        current = MovimientoSummary(ingresos, gastos),
        previous = previous,
        categorias = categorias,
        etiquetas = etiquetas,
        cuentas = cuentas,
        medios = medios,
        months = months,
        daily = daily,
        hasAnyMovimiento = true,
    )

private fun context(
    scope: StatisticsScope = StatisticsScope.Month,
    anchor: LocalDate = TODAY,
    moneda: Moneda = Moneda.CLP,
) = StatisticsContext.of(StatisticsSelection(scope, anchor), moneda, TODAY)

private fun present(
    snapshot: StatisticsSnapshot,
    scope: StatisticsScope = StatisticsScope.Month,
    anchor: LocalDate = TODAY,
    moneda: Moneda = Moneda.CLP,
) = presentStatistics(snapshot, context(scope, anchor, moneda), CATALOG)
