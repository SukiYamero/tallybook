package com.kurobello.tallybook.core.model

private const val CRITERIA_HASH_MULTIPLIER = 31

class MovimientoReadCriteria(
    val moneda: Moneda,
    val range: MovimientoDateRange? = null,
    val tipo: TipoMovimiento? = null,
    categoryIds: Set<String> = emptySet(),
    etiquetaIds: Set<String> = emptySet(),
) {
  private val capturedCategoryIds = categoryIds.toSet()
  private val capturedEtiquetaIds = etiquetaIds.toSet()

  val categoryIds: Set<String>
    get() = capturedCategoryIds.toSet()

  val etiquetaIds: Set<String>
    get() = capturedEtiquetaIds.toSet()

  fun copy(
      moneda: Moneda = this.moneda,
      range: MovimientoDateRange? = this.range,
      tipo: TipoMovimiento? = this.tipo,
      categoryIds: Set<String> = capturedCategoryIds,
      etiquetaIds: Set<String> = capturedEtiquetaIds,
  ): MovimientoReadCriteria = MovimientoReadCriteria(moneda, range, tipo, categoryIds, etiquetaIds)

  override fun equals(other: Any?): Boolean =
      other is MovimientoReadCriteria &&
          moneda == other.moneda &&
          range == other.range &&
          tipo == other.tipo &&
          capturedCategoryIds == other.capturedCategoryIds &&
          capturedEtiquetaIds == other.capturedEtiquetaIds

  override fun hashCode(): Int {
    var result = moneda.hashCode()
    result = CRITERIA_HASH_MULTIPLIER * result + (range?.hashCode() ?: 0)
    result = CRITERIA_HASH_MULTIPLIER * result + (tipo?.hashCode() ?: 0)
    result = CRITERIA_HASH_MULTIPLIER * result + capturedCategoryIds.hashCode()
    return CRITERIA_HASH_MULTIPLIER * result + capturedEtiquetaIds.hashCode()
  }
}
