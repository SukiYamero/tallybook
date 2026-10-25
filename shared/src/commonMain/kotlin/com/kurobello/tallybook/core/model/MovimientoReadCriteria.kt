package com.kurobello.tallybook.core.model

private const val CRITERIA_HASH_MULTIPLIER = 31

/**
 * Search's applied dimensions; an empty set means no restriction. Transfers match only while no
 * category, tag or medio is selected and [tipo] is null or [SearchTipo.TRANSFERENCIA], and a cuenta
 * matches either of their sides.
 */
@Suppress("LongParameterList")
class MovimientoReadCriteria(
    val moneda: Moneda,
    val range: MovimientoDateRange? = null,
    val tipo: SearchTipo? = null,
    categoryIds: Set<String> = emptySet(),
    etiquetaIds: Set<String> = emptySet(),
    cuentaIds: Set<String> = emptySet(),
    medios: Set<MedioPago> = emptySet(),
) {
  private val capturedCategoryIds = categoryIds.toSet()
  private val capturedEtiquetaIds = etiquetaIds.toSet()
  private val capturedCuentaIds = cuentaIds.toSet()
  private val capturedMedios = medios.toSet()

  val categoryIds: Set<String>
    get() = capturedCategoryIds.toSet()

  val etiquetaIds: Set<String>
    get() = capturedEtiquetaIds.toSet()

  val cuentaIds: Set<String>
    get() = capturedCuentaIds.toSet()

  val medios: Set<MedioPago>
    get() = capturedMedios.toSet()

  val includesMovimientos: Boolean
    get() = tipo != SearchTipo.TRANSFERENCIA

  val includesTransferencias: Boolean
    get() =
        (tipo == null || tipo == SearchTipo.TRANSFERENCIA) &&
            capturedCategoryIds.isEmpty() &&
            capturedEtiquetaIds.isEmpty() &&
            capturedMedios.isEmpty()

  fun copy(
      moneda: Moneda = this.moneda,
      range: MovimientoDateRange? = this.range,
      tipo: SearchTipo? = this.tipo,
      categoryIds: Set<String> = capturedCategoryIds,
      etiquetaIds: Set<String> = capturedEtiquetaIds,
      cuentaIds: Set<String> = capturedCuentaIds,
      medios: Set<MedioPago> = capturedMedios,
  ): MovimientoReadCriteria =
      MovimientoReadCriteria(moneda, range, tipo, categoryIds, etiquetaIds, cuentaIds, medios)

  override fun equals(other: Any?): Boolean =
      other is MovimientoReadCriteria &&
          moneda == other.moneda &&
          range == other.range &&
          tipo == other.tipo &&
          capturedCategoryIds == other.capturedCategoryIds &&
          capturedEtiquetaIds == other.capturedEtiquetaIds &&
          capturedCuentaIds == other.capturedCuentaIds &&
          capturedMedios == other.capturedMedios

  override fun hashCode(): Int {
    var result = moneda.hashCode()
    result = CRITERIA_HASH_MULTIPLIER * result + (range?.hashCode() ?: 0)
    result = CRITERIA_HASH_MULTIPLIER * result + (tipo?.hashCode() ?: 0)
    result = CRITERIA_HASH_MULTIPLIER * result + capturedCategoryIds.hashCode()
    result = CRITERIA_HASH_MULTIPLIER * result + capturedEtiquetaIds.hashCode()
    result = CRITERIA_HASH_MULTIPLIER * result + capturedCuentaIds.hashCode()
    return CRITERIA_HASH_MULTIPLIER * result + capturedMedios.hashCode()
  }
}
