package com.kurobello.tallybook.core.model

sealed interface CategoriaWrite {

  data class Saved(val categoria: Categoria) : CategoriaWrite

  /** Nothing was written: [existing] already has the same [CategoriaRules.nombreKey]. */
  data class Duplicate(val existing: Categoria) : CategoriaWrite
}
