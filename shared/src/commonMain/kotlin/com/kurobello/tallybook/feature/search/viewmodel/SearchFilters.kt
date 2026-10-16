package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.FIRST_SELECTABLE_YEAR
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoMovimiento
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

internal class SearchFilters(
    val preset: SearchDatePreset,
    val customStart: LocalDate,
    val customEnd: LocalDate,
    val tipo: TipoMovimiento? = null,
    categoryIds: Set<String> = emptySet(),
    etiquetaIds: Set<String> = emptySet(),
) {
  private val capturedCategoryIds = categoryIds.toSet()
  private val capturedEtiquetaIds = etiquetaIds.toSet()

  init {
    require(capturedCategoryIds.none { it.isBlank() }) { "Selected category IDs must not be blank" }
    require(capturedEtiquetaIds.none { it.isBlank() }) { "Selected etiqueta IDs must not be blank" }
  }

  val categoryIds: Set<String>
    get() = capturedCategoryIds.toSet()

  val etiquetaIds: Set<String>
    get() = capturedEtiquetaIds.toSet()

  val hasActiveFilters: Boolean
    get() =
        preset != SearchDatePreset.All ||
            tipo != null ||
            capturedCategoryIds.isNotEmpty() ||
            capturedEtiquetaIds.isNotEmpty()

  @Suppress("LongParameterList")
  fun copy(
      preset: SearchDatePreset = this.preset,
      customStart: LocalDate = this.customStart,
      customEnd: LocalDate = this.customEnd,
      tipo: TipoMovimiento? = this.tipo,
      categoryIds: Set<String> = capturedCategoryIds,
      etiquetaIds: Set<String> = capturedEtiquetaIds,
  ): SearchFilters = SearchFilters(preset, customStart, customEnd, tipo, categoryIds, etiquetaIds)

  fun toggleCategory(id: String): SearchFilters =
      copy(categoryIds = capturedCategoryIds.toggled(id))

  fun toggleEtiqueta(id: String): SearchFilters =
      copy(etiquetaIds = capturedEtiquetaIds.toggled(id))

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
          capturedEtiquetaIds == other.capturedEtiquetaIds

  override fun hashCode(): Int {
    var result = preset.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + customStart.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + customEnd.hashCode()
    result = FILTER_HASH_MULTIPLIER * result + (tipo?.hashCode() ?: 0)
    result = FILTER_HASH_MULTIPLIER * result + capturedCategoryIds.hashCode()
    return FILTER_HASH_MULTIPLIER * result + capturedEtiquetaIds.hashCode()
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
