package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.SearchTipo
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val FILTER_HASH_MULTIPLIER = 31
private const val LAST_SEVEN_START_OFFSET = 6
private const val LAST_THIRTY_START_OFFSET = 29

internal enum class SearchDatePreset {
  All,
  Last7Days,
  Last30Days,
  ThisMonth,
  ThisYear,
  Custom,
}

@Suppress("LongParameterList")
internal class SearchFilters(
    val preset: SearchDatePreset,
    val customStart: LocalDate,
    val customEnd: LocalDate,
    val tipo: SearchTipo? = null,
    categoryIds: Set<String> = emptySet(),
    etiquetaIds: Set<String> = emptySet(),
    cuentaIds: Set<String> = emptySet(),
    medios: Set<MedioPago> = emptySet(),
) {
  private val capturedCategoryIds = categoryIds.toSet()
  private val capturedEtiquetaIds = etiquetaIds.toSet()
  private val capturedCuentaIds = cuentaIds.toSet()
  private val capturedMedios = medios.toSet()

  init {
    require(capturedCategoryIds.none { it.isBlank() }) { "Selected category IDs must not be blank" }
    require(capturedEtiquetaIds.none { it.isBlank() }) { "Selected etiqueta IDs must not be blank" }
    require(capturedCuentaIds.none { it.isBlank() }) { "Selected cuenta IDs must not be blank" }
  }

  val categoryIds: Set<String>
    get() = capturedCategoryIds.toSet()

  val etiquetaIds: Set<String>
    get() = capturedEtiquetaIds.toSet()

  val cuentaIds: Set<String>
    get() = capturedCuentaIds.toSet()

  val medios: Set<MedioPago>
    get() = capturedMedios.toSet()

  val hasActiveFilters: Boolean
    get() =
        preset != SearchDatePreset.All ||
            tipo != null ||
            capturedCategoryIds.isNotEmpty() ||
            capturedEtiquetaIds.isNotEmpty() ||
            capturedCuentaIds.isNotEmpty() ||
            capturedMedios.isNotEmpty()

  fun copy(
      preset: SearchDatePreset = this.preset,
      customStart: LocalDate = this.customStart,
      customEnd: LocalDate = this.customEnd,
      tipo: SearchTipo? = this.tipo,
      categoryIds: Set<String> = capturedCategoryIds,
      etiquetaIds: Set<String> = capturedEtiquetaIds,
      cuentaIds: Set<String> = capturedCuentaIds,
      medios: Set<MedioPago> = capturedMedios,
  ): SearchFilters =
      SearchFilters(
          preset,
          customStart,
          customEnd,
          tipo,
          categoryIds,
          etiquetaIds,
          cuentaIds,
          medios,
      )

  fun toggleCategory(id: String): SearchFilters =
      copy(categoryIds = capturedCategoryIds.toggled(id))

  fun toggleEtiqueta(id: String): SearchFilters =
      copy(etiquetaIds = capturedEtiquetaIds.toggled(id))

  fun toggleCuenta(id: String): SearchFilters = copy(cuentaIds = capturedCuentaIds.toggled(id))

  fun toggleMedio(medio: MedioPago): SearchFilters =
      copy(medios = if (medio in capturedMedios) capturedMedios - medio else capturedMedios + medio)

  fun availableYears(
      persistedYears: List<Int>,
      today: LocalDate,
      retainedYears: Set<Int> = emptySet(),
  ): List<Int> =
      ((FIRST_SELECTABLE_YEAR..today.year) +
              persistedYears +
              retainedYears +
              today.year +
              customStart.year +
              customEnd.year)
          .filter { it <= today.year }
          .distinct()
          .sortedDescending()

  override fun equals(other: Any?): Boolean =
      other is SearchFilters &&
          preset == other.preset &&
          customStart == other.customStart &&
          customEnd == other.customEnd &&
          tipo == other.tipo &&
          capturedCategoryIds == other.capturedCategoryIds &&
          capturedEtiquetaIds == other.capturedEtiquetaIds &&
          capturedCuentaIds == other.capturedCuentaIds &&
          capturedMedios == other.capturedMedios

  override fun hashCode(): Int {
    var result = preset.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + customStart.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + customEnd.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + (tipo?.hashCode() ?: 0)
    result = FILTER_HASH_MULTIPLIER * result + capturedCategoryIds.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + capturedEtiquetaIds.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + capturedCuentaIds.hashCode()
    return FILTER_HASH_MULTIPLIER * result + capturedMedios.hashCode()
  }

  companion object {
    fun initial(today: LocalDate): SearchFilters = SearchFilters(SearchDatePreset.All, today, today)
  }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

internal enum class SearchDateFailure {
  FutureDate,
}

internal sealed interface SearchFilterResolution {
  data class Valid(val filters: SearchFilters, val range: MovimientoDateRange?) :
      SearchFilterResolution

  data class Invalid(val failure: SearchDateFailure) : SearchFilterResolution
}

internal fun resolveSearchFilters(
    filters: SearchFilters,
    today: LocalDate,
): SearchFilterResolution {
  if (
      filters.preset == SearchDatePreset.Custom &&
          (filters.customStart > today || filters.customEnd > today)
  ) {
    return SearchFilterResolution.Invalid(SearchDateFailure.FutureDate)
  }
  val range =
      when (filters.preset) {
        SearchDatePreset.All -> null
        SearchDatePreset.Last7Days ->
            MovimientoDateRange(today.minus(LAST_SEVEN_START_OFFSET, DateTimeUnit.DAY), today)
        SearchDatePreset.Last30Days ->
            MovimientoDateRange(today.minus(LAST_THIRTY_START_OFFSET, DateTimeUnit.DAY), today)
        SearchDatePreset.ThisMonth ->
            MovimientoDateRange(LocalDate(today.year, today.month, 1), today)
        SearchDatePreset.ThisYear -> MovimientoDateRange(LocalDate(today.year, 1, 1), today)
        SearchDatePreset.Custom ->
            MovimientoDateRange(
                minOf(filters.customStart, filters.customEnd),
                maxOf(filters.customStart, filters.customEnd),
            )
      }
  val normalized =
      if (filters.preset == SearchDatePreset.Custom && range != null) {
        filters.copy(customStart = range.start, customEnd = range.end)
      } else filters
  return SearchFilterResolution.Valid(normalized, range)
}
