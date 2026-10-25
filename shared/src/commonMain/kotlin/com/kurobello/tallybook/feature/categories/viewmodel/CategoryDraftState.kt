package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.text.capTo

internal enum class CategoryNombreError {
  BLANK,
  TOO_LONG,
}

/**
 * The editable draft of one custom categoria, owned by the ViewModel that hosts the form.
 *
 * [icono] and [color] follow the name's suggestion until the user picks one by hand; each field
 * stops following on its own. [duplicate] is the existing categoria the last write collided with.
 */
internal data class CategoryDraftState(
    val nombre: String = "",
    val icono: CategoryIcon = CategoryIcon.TAG,
    val color: CategoryTint = CategoryTint.NEUTRAL,
    val descripcion: String = "",
    val iconoTouched: Boolean = false,
    val colorTouched: Boolean = false,
    val nombreEdited: Boolean = false,
    val moreOptions: Boolean = false,
    val duplicate: Categoria? = null,
    val saving: Boolean = false,
) {
  val nombreError: CategoryNombreError?
    get() {
      val trimmed = nombre.trim()
      return when {
        trimmed.isEmpty() -> CategoryNombreError.BLANK
        trimmed.length > CategoriaRules.NOMBRE_MAX -> CategoryNombreError.TOO_LONG
        else -> null
      }
    }

  /** A pristine empty name is not an error yet: the form opens without shouting at the user. */
  val visibleNombreError: CategoryNombreError?
    get() = nombreError?.takeIf { nombreEdited || it != CategoryNombreError.BLANK }

  /** A duplicate blocks saving because the same name would collide again. */
  val canSave: Boolean
    get() = nombreError == null && duplicate == null && !saving

  fun toDraft(): CategoriaDraft =
      CategoriaDraft(
          nombre = nombre.trim(),
          icono = icono,
          color = color,
          descripcion = descripcion.trim().ifEmpty { null },
      )

  companion object {
    fun forNew(nombre: String, suggester: CategoryAppearanceSuggester): CategoryDraftState =
        CategoryDraftState().followNombre(nombre.capTo(CategoriaRules.NOMBRE_MAX), suggester)

    /** Editing keeps the stored look, so renaming never overwrites a chosen icon or tint. */
    fun forEdit(categoria: Categoria): CategoryDraftState =
        CategoryDraftState(
            nombre = categoria.nombre,
            icono = categoria.icono,
            color = categoria.color,
            descripcion = categoria.descripcion.orEmpty(),
            iconoTouched = true,
            colorTouched = true,
            moreOptions = true,
        )
  }
}

internal fun CategoryDraftState.onNombre(
    value: String,
    suggester: CategoryAppearanceSuggester,
): CategoryDraftState =
    followNombre(value.capTo(CategoriaRules.NOMBRE_MAX), suggester)
        .copy(nombreEdited = true, duplicate = null)

internal fun CategoryDraftState.onIcono(icono: CategoryIcon): CategoryDraftState =
    copy(icono = icono, iconoTouched = true)

internal fun CategoryDraftState.onColor(color: CategoryTint): CategoryDraftState =
    copy(color = color, colorTouched = true)

internal fun CategoryDraftState.onDescripcion(value: String): CategoryDraftState =
    copy(descripcion = value.capTo(CategoriaRules.DESCRIPCION_MAX))

internal fun CategoryDraftState.onMoreOptions(): CategoryDraftState = copy(moreOptions = true)

private fun CategoryDraftState.followNombre(
    nombre: String,
    suggester: CategoryAppearanceSuggester,
): CategoryDraftState {
  if (iconoTouched && colorTouched) return copy(nombre = nombre)
  val suggestion = suggester.suggest(nombre)
  return copy(
      nombre = nombre,
      icono = if (iconoTouched) icono else suggestion.icono,
      color = if (colorTouched) color else suggestion.color,
  )
}
