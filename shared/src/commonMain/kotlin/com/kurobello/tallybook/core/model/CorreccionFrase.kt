package com.kurobello.tallybook.core.model

import kotlin.time.Instant

enum class CampoCorreccion {
  TIPO,
  MONTO,
  FECHA,
  CATEGORIA,
  MEDIO,
  CUENTA,
  FRECUENCIA,
  TITULO,
  PARTES,
}

enum class OrigenFrase {
  DICTADO,
  ESCRITO,
}

/**
 * One field the user changed after the sentence parser filled it: [leido] is what the parser read,
 * [propuesto] what the form proposed, [guardado] what the user saved. [palabras] are the sentence's
 * words.
 */
data class CorreccionFrase(
    val id: String,
    val origen: OrigenFrase,
    val palabras: List<String>,
    val campo: CampoCorreccion,
    val leido: String?,
    val propuesto: String?,
    val guardado: String?,
    val createdAt: Instant,
)
