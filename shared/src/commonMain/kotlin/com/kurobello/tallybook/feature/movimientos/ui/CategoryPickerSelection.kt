package com.kurobello.tallybook.feature.movimientos.ui

/** The single category a movimiento picker reports; [choose] skips the current one. */
internal data class CategoryPickerSelection(
    val selectedId: String?,
    val onSelect: (String) -> Unit,
)

internal fun CategoryPickerSelection.choose(id: String) {
  if (id != selectedId) onSelect(id)
}
