package com.kurobello.tallybook.core.model

sealed interface EtiquetaWrite {

  data class Saved(val etiqueta: Etiqueta) : EtiquetaWrite

  /** Nothing was written: [existing] already has the same [CategoriaRules.nombreKey]. */
  data class Duplicate(val existing: Etiqueta) : EtiquetaWrite
}
