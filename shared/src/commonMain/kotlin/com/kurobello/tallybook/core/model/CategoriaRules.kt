package com.kurobello.tallybook.core.model

import com.kurobello.tallybook.core.text.normalizeSearchText

object CategoriaRules {
  const val NOMBRE_MAX: Int = 30
  const val DESCRIPCION_MAX: Int = 340

  /** Two names with the same key are the same categoria, whatever their case or accents. */
  fun nombreKey(nombre: String): String = normalizeSearchText(nombre.trim())
}
