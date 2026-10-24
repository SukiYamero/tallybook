package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.SearchTipo
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

private const val SEARCH_SAVED_VERSION = 1

private val presetTokens =
    mapOf(
        SearchDatePreset.All to "all",
        SearchDatePreset.Last7Days to "last-7-days",
        SearchDatePreset.Last30Days to "last-30-days",
        SearchDatePreset.ThisMonth to "this-month",
        SearchDatePreset.ThisYear to "this-year",
        SearchDatePreset.Custom to "custom",
    )

// "gasto" and "ingreso" are the tokens saved while the type was a TipoMovimiento, so a checkpoint
// from before transfers restores the same selection.
private val typeTokens =
    mapOf(
        SearchTipo.GASTO to "gasto",
        SearchTipo.INGRESO to "ingreso",
        SearchTipo.TRANSFERENCIA to "transferencia",
    )

/**
 * Fields added after the first version default to empty, so older checkpoints still decode. Unknown
 * medio names are dropped one by one; an unknown [moneda] restores the primary currency.
 */
@Serializable
internal data class SearchSavedInput(
    val version: Int,
    val rawQuery: String,
    val preset: String,
    val customStart: String,
    val customEnd: String,
    val tipo: String?,
    val categoryIds: List<String>,
    val etiquetaIds: List<String> = emptyList(),
    val cuentaIds: List<String> = emptyList(),
    val medios: List<String> = emptyList(),
    val moneda: String? = null,
) {
  companion object {
    fun restore(input: SearchSavedInput?, today: LocalDate): SearchRestoredInput {
      val defaults = SearchFilters.initial(today)
      if (input == null || input.version != SEARCH_SAVED_VERSION)
          return SearchRestoredInput("", defaults)
      val filters = input.restoreFilters(today) ?: defaults
      return SearchRestoredInput(
          input.rawQuery,
          filters,
          Moneda.entries.firstOrNull { it.name == input.moneda },
      )
    }
  }
}

internal data class SearchRestoredInput(
    val rawQuery: String,
    val filters: SearchFilters,
    val moneda: Moneda? = null,
)

internal fun SearchFilters.savedInput(rawQuery: String, moneda: Moneda? = null): SearchSavedInput =
    SearchSavedInput(
        version = SEARCH_SAVED_VERSION,
        rawQuery = rawQuery,
        preset = presetTokens.getValue(preset),
        customStart = customStart.toString(),
        customEnd = customEnd.toString(),
        tipo = tipo?.let(typeTokens::getValue),
        categoryIds = categoryIds.sorted(),
        etiquetaIds = etiquetaIds.sorted(),
        cuentaIds = cuentaIds.sorted(),
        medios = medios.sorted().map { it.name },
        moneda = moneda?.name,
    )

private fun SearchSavedInput.restoreFilters(today: LocalDate): SearchFilters? {
  val restoredPreset = presetTokens.entries.firstOrNull { it.value == preset }?.key
  val restoredType = tipo?.let { token ->
    typeTokens.entries.firstOrNull { it.value == token }?.key
  }
  val validType = tipo == null || restoredType != null
  val dates =
      parseDate(customStart)?.let { start -> parseDate(customEnd)?.let { end -> start to end } }
  if (restoredPreset == null || !validType || dates == null) return null
  val (start, end) = dates
  val filters =
      SearchFilters(
          restoredPreset,
          start,
          end,
          restoredType,
          categoryIds.filterNot { it.isBlank() }.toSet(),
          etiquetaIds.filterNot { it.isBlank() }.toSet(),
          cuentaIds.filterNot { it.isBlank() }.toSet(),
          medios.mapNotNull { name -> MedioPago.entries.firstOrNull { it.name == name } }.toSet(),
      )
  return when (val resolution = resolveSearchFilters(filters, today)) {
    is SearchFilterResolution.Valid -> resolution.filters
    is SearchFilterResolution.Invalid -> null
  }
}

private fun parseDate(value: String): LocalDate? =
    try {
      LocalDate.parse(value)
    } catch (_: IllegalArgumentException) {
      null
    }
