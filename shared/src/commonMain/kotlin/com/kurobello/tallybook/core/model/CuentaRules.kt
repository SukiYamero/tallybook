package com.kurobello.tallybook.core.model

enum class CuentaNombreError {
  BLANK,
  TOO_LONG,
}

enum class DiaCorteError {
  OUT_OF_RANGE,
  NOT_CREDITO,
}

data class CuentaErrors(
    val nombre: CuentaNombreError? = null,
    val diaCorte: DiaCorteError? = null,
) {
  val isValid: Boolean
    get() = nombre == null && diaCorte == null
}

object CuentaRules {
  const val NOMBRE_MAX: Int = CategoriaRules.NOMBRE_MAX
  const val DIA_CORTE_MAX: Int = 31
  val DIA_CORTE_RANGE: IntRange = 1..DIA_CORTE_MAX

  /** Names are unique per moneda, so the same name may exist once in each currency. */
  fun nombreKey(nombre: String, moneda: Moneda): String =
      "${moneda.name}:${CategoriaRules.nombreKey(nombre)}"

  fun validate(nombre: String, tipo: TipoCuenta, diaCorte: Int?): CuentaErrors {
    val trimmed = nombre.trim()
    return CuentaErrors(
        nombre =
            when {
              trimmed.isEmpty() -> CuentaNombreError.BLANK
              trimmed.length > NOMBRE_MAX -> CuentaNombreError.TOO_LONG
              else -> null
            },
        diaCorte =
            when {
              diaCorte == null -> null
              tipo != TipoCuenta.CREDITO -> DiaCorteError.NOT_CREDITO
              diaCorte !in DIA_CORTE_RANGE -> DiaCorteError.OUT_OF_RANGE
              else -> null
            },
    )
  }
}
