package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoReadCriteria
import com.kurobello.tallybook.core.model.SearchTipo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class SearchFiltersTest {
  private val today = LocalDate(2028, 3, 1)

  @Test
  fun accountAndMedioSelectionsToggleAndActivateFiltersIntoTheCriteria() {
    val initial = SearchFilters.initial(today)
    val selected = initial.toggleCuenta("nequi").toggleMedio(MedioPago.DEBITO)
    assertTrue(selected.hasActiveFilters)
    assertEquals(setOf("nequi"), selected.cuentaIds)
    assertEquals(setOf(MedioPago.DEBITO), selected.medios)
    assertEquals(initial, selected.toggleCuenta("nequi").toggleMedio(MedioPago.DEBITO))
    assertEquals(
        initial.hashCode(),
        selected.toggleCuenta("nequi").toggleMedio(MedioPago.DEBITO).hashCode(),
    )
    assertTrue(initial.copy(tipo = SearchTipo.TRANSFERENCIA).hasActiveFilters)
    assertEquals(
        MovimientoReadCriteria(
            Moneda.USD,
            tipo = SearchTipo.TRANSFERENCIA,
            cuentaIds = setOf("nequi"),
            medios = setOf(MedioPago.DEBITO),
        ),
        selected.copy(tipo = SearchTipo.TRANSFERENCIA).criteria(Moneda.USD, today),
    )
  }

  @Test
  fun transfersAreReadOnlyWithoutCategoryTagOrMedioAndForAnEmptyOrTransferType() {
    val base = MovimientoReadCriteria(Moneda.COP)
    assertTrue(base.includesTransferencias)
    assertTrue(base.copy(cuentaIds = setOf("nequi")).includesTransferencias)
    assertTrue(base.copy(tipo = SearchTipo.TRANSFERENCIA).includesTransferencias)
    assertFalse(base.copy(tipo = SearchTipo.TRANSFERENCIA).includesMovimientos)
    for (excluding in
        listOf(
            base.copy(tipo = SearchTipo.GASTO),
            base.copy(categoryIds = setOf("food")),
            base.copy(etiquetaIds = setOf("trip")),
            base.copy(medios = setOf(MedioPago.EFECTIVO)),
        )) {
      assertFalse(excluding.includesTransferencias)
      assertTrue(excluding.includesMovimientos)
    }
  }

  @Test
  fun relativePresetsIncludeBothBoundariesAcrossLeapMonth() {
    val expected =
        mapOf(
            SearchDatePreset.Last7Days to LocalDate(2028, 2, 24),
            SearchDatePreset.Last30Days to LocalDate(2028, 2, 1),
        )
    expected.forEach { (preset, start) ->
      val range = resolved(SearchFilters.initial(today).copy(preset = preset)).range!!
      assertEquals(MovimientoDateRange(start, today), range)
      assertTrue(start >= range.start && start <= range.end)
      assertTrue(today >= range.start && today <= range.end)
      assertFalse(start.minus(1, DateTimeUnit.DAY) >= range.start)
      assertFalse(today.plus(1, DateTimeUnit.DAY) <= range.end)
    }
  }

  @Test
  fun relativePresetsCrossYearAndKeepTheirSelectedLabels() {
    listOf(LocalDate(2028, 1, 1), LocalDate(2028, 12, 31), LocalDate(2028, 2, 29)).forEach { date ->
      val filters = SearchFilters.initial(date)
      val seven =
          assertIs<SearchFilterResolution.Valid>(
              resolveSearchFilters(filters.copy(preset = SearchDatePreset.Last7Days), date)
          )
      assertEquals(date.minus(6, DateTimeUnit.DAY), seven.range!!.start)
      assertEquals(date, seven.range.end)
      val month =
          assertIs<SearchFilterResolution.Valid>(
              resolveSearchFilters(filters.copy(preset = SearchDatePreset.ThisMonth), date)
          )
      assertEquals(LocalDate(date.year, date.month, 1), month.range!!.start)
      val year =
          assertIs<SearchFilterResolution.Valid>(
              resolveSearchFilters(filters.copy(preset = SearchDatePreset.ThisYear), date)
          )
      assertEquals(LocalDate(date.year, 1, 1), year.range!!.start)
      assertEquals(SearchDatePreset.ThisYear, year.filters.preset)
    }
  }

  @Test
  fun allHasNoRangeAndInactiveCustomEndpointsDoNotActivateFilters() {
    val filters = SearchFilters.initial(today).copy(customStart = LocalDate(1900, 1, 1))
    assertNull(resolved(filters).range)
    assertFalse(filters.hasActiveFilters)
    assertTrue(filters.copy(preset = SearchDatePreset.Custom).hasActiveFilters)
  }

  @Test
  fun customNormalizesReversedCrossYearAndOneDayRanges() {
    val filters =
        SearchFilters.initial(today)
            .copy(preset = SearchDatePreset.Custom, customEnd = LocalDate(2027, 12, 31))
    val resolution = resolved(filters)
    assertEquals(MovimientoDateRange(LocalDate(2027, 12, 31), today), resolution.range)
    assertEquals(resolution.range!!.start, resolution.filters.customStart)
    assertEquals(today, resolution.filters.customEnd)
    assertEquals(
        MovimientoDateRange(today, today),
        resolved(SearchFilters.initial(today).copy(preset = SearchDatePreset.Custom)).range,
    )
  }

  @Test
  fun futureCustomRemainsInvalidAfterBackwardClockInsteadOfClamping() {
    val filters = SearchFilters.initial(today).copy(preset = SearchDatePreset.Custom)
    val failure =
        assertIs<SearchFilterResolution.Invalid>(
            resolveSearchFilters(filters, LocalDate(2028, 2, 29))
        )
    assertEquals(SearchDateFailure.FutureDate, failure.failure)
    assertEquals(today, filters.customStart)
    assertIs<SearchFilterResolution.Invalid>(
        resolveSearchFilters(filters.copy(customEnd = today.plus(1, DateTimeUnit.DAY)), today)
    )
  }

  @Test
  fun switchingPresetsPreservesCustomEndpointsAndExactCategoryIds() {
    val filters =
        SearchFilters.initial(today)
            .copy(
                preset = SearchDatePreset.Custom,
                customStart = LocalDate(1900, 1, 1),
                tipo = SearchTipo.INGRESO,
                categoryIds = setOf("parent", "missing"),
            )
    val all = filters.copy(preset = SearchDatePreset.All)
    assertEquals(filters.customStart, resolved(all).filters.customStart)
    assertEquals(filters, filters.toggleCategory("child").toggleCategory("child"))
    assertEquals(setOf("parent", "missing"), filters.categoryIds)
    assertEquals(SearchTipo.INGRESO, filters.tipo)
    assertTrue(1900 in filters.availableYears(emptyList(), today))
  }

  @Test
  fun callerMutationCannotChangeCapturedFiltersOrCriteriaIdentity() {
    val ids = mutableSetOf("one", "two")
    val filters = SearchFilters.initial(today).copy(categoryIds = ids)
    val criteria = MovimientoReadCriteria(Moneda.COP, categoryIds = ids)
    val criteriaHash = criteria.hashCode()
    ids.clear()
    assertEquals(setOf("one", "two"), filters.categoryIds)
    assertEquals(MovimientoReadCriteria(Moneda.COP, categoryIds = setOf("two", "one")), criteria)
    assertEquals(criteriaHash, criteria.hashCode())
  }

  @Test
  fun togglingTagsAddsAndRemovesExactIdsAndActivatesFilters() {
    val initial = SearchFilters.initial(today)
    val tagged = initial.toggleEtiqueta("t_empresa").toggleEtiqueta("t_viaje")

    assertEquals(setOf("t_empresa", "t_viaje"), tagged.etiquetaIds)
    assertTrue(tagged.hasActiveFilters)
    assertEquals(emptySet(), tagged.categoryIds)
    assertEquals(setOf("t_viaje"), tagged.toggleEtiqueta("t_empresa").etiquetaIds)
    assertEquals(initial, tagged.toggleEtiqueta("t_empresa").toggleEtiqueta("t_viaje"))
    assertFalse(initial.hasActiveFilters)
  }

  @Test
  fun tagsTakePartInEqualityAndSurviveOtherDimensionCopies() {
    val tagged = SearchFilters.initial(today).copy(etiquetaIds = setOf("t_empresa"))

    assertFalse(tagged == SearchFilters.initial(today))
    assertEquals(tagged, SearchFilters.initial(today).toggleEtiqueta("t_empresa"))
    assertEquals(
        tagged.hashCode(),
        SearchFilters.initial(today).toggleEtiqueta("t_empresa").hashCode(),
    )
    val typed = tagged.copy(tipo = SearchTipo.GASTO).toggleCategory("food")
    assertEquals(setOf("t_empresa"), typed.etiquetaIds)
    assertEquals(setOf("t_empresa"), resolved(typed).filters.etiquetaIds)
  }

  @Test
  fun clearingFiltersDropsTags() {
    val tagged = SearchFilters.initial(today).toggleEtiqueta("t_empresa")
    val cleared =
        assertIs<SearchFilterSession.Closing>(
                SearchFilterSession.Editing(SearchFilterSessionId(1), tagged)
                    .clear(SearchFilterSessionId(1), today)
                    .session
            )
            .renderSnapshot

    assertEquals(emptySet(), cleared.etiquetaIds)
    assertFalse(cleared.hasActiveFilters)
  }

  private fun resolved(filters: SearchFilters): SearchFilterResolution.Valid =
      assertIs(resolveSearchFilters(filters, today))
}
