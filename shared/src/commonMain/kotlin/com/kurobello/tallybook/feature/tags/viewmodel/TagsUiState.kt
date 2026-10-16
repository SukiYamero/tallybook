package com.kurobello.tallybook.feature.tags.viewmodel

import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.Etiqueta

internal data class TagsUiState(
    val query: String = "",
    val content: TagsContent = TagsContent.Loading,
    val sheet: TagSheet? = null,
)

internal sealed interface TagsContent {
  data object Loading : TagsContent

  data object Failed : TagsContent

  /** Every etiqueta sorted by name; empty when the user has none yet. */
  data class Browse(val rows: List<TagRow>) : TagsContent

  data class Search(val query: String, val matches: List<TagRow>) : TagsContent
}

/** [count] is the primary moneda's movimientos carrying the etiqueta. */
internal data class TagRow(val etiqueta: Etiqueta, val count: Long)

internal sealed interface TagSheetRequest {
  data class New(val nombre: String) : TagSheetRequest

  data class Open(val id: String) : TagSheetRequest
}

internal enum class TagNombreError {
  BLANK,
  TOO_LONG,
}

internal enum class TagSheetError {
  SAVE,
  DELETE,
}

internal sealed interface TagDeletion {
  val isBusy: Boolean

  /** Counting movimientos in every moneda before asking. */
  data object Checking : TagDeletion {
    override val isBusy: Boolean = true
  }

  data class Confirm(val count: Long, val deleting: Boolean = false) : TagDeletion {
    override val isBusy: Boolean
      get() = deleting
  }
}

/**
 * The one open tag sheet: a new etiqueta when [editing] is null, otherwise its rename form. Its
 * [editing] and [count] follow the source while open. A closing sheet keeps the shell modal-owned
 * until it reports it hid; [opensSearch] then shows the etiqueta's movimientos in Search.
 * [duplicate] is the stored etiqueta the last write collided with.
 */
internal data class TagSheet(
    val editing: Etiqueta?,
    val nombre: String,
    val nombreEdited: Boolean = false,
    val count: Long = 0L,
    val saving: Boolean = false,
    val duplicate: Etiqueta? = null,
    val error: TagSheetError? = null,
    val deletion: TagDeletion? = null,
    val isClosing: Boolean = false,
    val opensSearch: Boolean = false,
) {
  val nombreError: TagNombreError?
    get() {
      val trimmed = nombre.trim()
      return when {
        trimmed.isEmpty() -> TagNombreError.BLANK
        trimmed.length > CategoriaRules.NOMBRE_MAX -> TagNombreError.TOO_LONG
        else -> null
      }
    }

  /** A pristine empty name is not an error yet: the sheet opens without shouting at the user. */
  val visibleNombreError: TagNombreError?
    get() = nombreError?.takeIf { nombreEdited || it != TagNombreError.BLANK }

  val isBusy: Boolean
    get() = saving || deletion?.isBusy == true

  val isEditable: Boolean
    get() = !isClosing && !isBusy && deletion == null

  /** A duplicate blocks saving because the same name would collide again. */
  val canSave: Boolean
    get() = isEditable && nombreError == null && duplicate == null

  companion object {
    fun forNew(nombre: String): TagSheet = TagSheet(editing = null, nombre = nombre.trim())

    fun forEdit(etiqueta: Etiqueta, count: Long): TagSheet =
        TagSheet(editing = etiqueta, nombre = etiqueta.nombre, count = count)
  }
}
