package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategoryHistoryRow
import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.MovimientoUpdateResult
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackViewModel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

internal class ManageMovimientoFixture(scope: CoroutineScope) {
  val repository = ManagementRepository()
  val categorias = ManagementCategorias()
  val etiquetas = FakeEtiquetaRepository()
  val cuentas = FakeCuentaRepository(listOf(USD_CASH, USD_BANK, USD_CARD, COP_CASH, NU))
  val clock = ManagementClock()
  val feedback = ManagementFeedbackViewModel(repository, FakeTransferenciaRepository(), scope).queue
  val viewModel =
      ManageMovimientoViewModel(
          ManageMovimientoRepositories(repository, categorias, etiquetas, cuentas),
          clock,
          TimeZoneProvider { TimeZone.UTC },
          feedback,
          scope,
      )
}

internal class ManagementClock : Clock {
  var instant: Instant = Instant.parse("2026-09-15T12:00:00Z")

  var reads = 0

  override fun now(): Instant {
    reads++
    return instant
  }
}

internal class ManagementCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  val values =
      MutableStateFlow<DataOutcome<List<Categoria>>>(
          DataOutcome.Success(listOf(managementCategoria()))
      )

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = values

  override suspend fun getAll(): DataOutcome<List<Categoria>> = values.value

  override suspend fun localizeSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)

  override suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit> =
      DataOutcome.Success(Unit)
}

internal class ManagementRepository : MovimientoRepository {
  /** Answers per medio; the editor reads them with the movimiento's currency as primary. */
  val medioSelections = mutableMapOf<MedioPago?, CuentaSelection?>()
  val selectionRequests = mutableListOf<Pair<Moneda, MedioPago?>>()

  override suspend fun defaultSelection(
      primary: Moneda,
      tipo: TipoMovimiento,
      medio: MedioPago?,
  ): DataOutcome<CuentaSelection?> {
    selectionRequests += primary to medio
    return DataOutcome.Success(medioSelections[medio])
  }

  var current: Movimiento? = managementMovimiento()
  var readResult: DataOutcome<Movimiento?>? = null
  var updateResult: DataOutcome<MovimientoUpdateResult> =
      DataOutcome.Success(MovimientoUpdateResult.Updated)
  var deleteResult: DataOutcome<Movimiento?>? = null
  var restoreResult: DataOutcome<RestoreResult> = DataOutcome.Success(RestoreResult.Restored)
  var readGate: CompletableDeferred<Unit>? = null
  var updateGate: CompletableDeferred<Unit>? = null
  var restoreGate: CompletableDeferred<Unit>? = null
  var deleteGate: CompletableDeferred<Unit>? = null
  var reads = 0
  var updates = 0
  var deletes = 0
  var restores = 0
  var restored: Movimiento? = null
  var changes: MovimientoChanges? = null

  override suspend fun getById(id: String): DataOutcome<Movimiento?> {
    reads++
    readGate?.await()
    return readResult ?: DataOutcome.Success(current?.takeIf { it.id == id })
  }

  override suspend fun update(
      id: String,
      changes: MovimientoChanges,
  ): DataOutcome<MovimientoUpdateResult> {
    updates++
    this.changes = changes
    updateGate?.await()
    if (updateResult == DataOutcome.Success(MovimientoUpdateResult.Updated)) {
      current =
          current?.copy(
              tipo = changes.tipo,
              monto = changes.monto,
              fecha = changes.fecha,
              categoriaId = changes.categoriaId,
              etiquetaIds = changes.etiquetaIds,
              titulo = changes.titulo,
              descripcion = changes.descripcion,
              cuentaId = changes.cuentaId,
              medio = changes.medio,
          )
    }
    return updateResult
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Movimiento?> {
    deletes++
    deleteGate?.await()
    val result = deleteResult ?: DataOutcome.Success(current)
    if (result is DataOutcome.Success) current = null
    return result
  }

  override suspend fun restoreDeleted(movimiento: Movimiento): DataOutcome<RestoreResult> {
    restores++
    restored = movimiento
    restoreGate?.await()
    return restoreResult
  }

  override fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>> = emptyFlow()

  override fun observeRecent(moneda: Moneda, limit: Int): Flow<DataOutcome<List<Movimiento>>> =
      emptyFlow()

  override fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

  override fun observeSummary(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<MovimientoSummary>> = emptyFlow()

  override fun observeRange(
      moneda: Moneda,
      range: MovimientoDateRange,
  ): Flow<DataOutcome<List<Movimiento>>> = emptyFlow()

  override fun observeYears(moneda: Moneda): Flow<DataOutcome<List<Int>>> = emptyFlow()

  override fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>> =
      emptyFlow()

  override suspend fun insert(movimiento: Movimiento): DataOutcome<Unit> =
      error("Management cannot insert")

  override suspend fun getAll(): DataOutcome<List<Movimiento>> =
      DataOutcome.Success(listOfNotNull(current))

  override suspend fun delete(id: String): DataOutcome<Unit> =
      error("Management must capture deletion")
}

internal fun managementCategoria(id: String = "food", parent: String? = null) =
    Categoria(id, "Food", parent, CategoryIcon.UTENSILS, CategoryTint.AMBER)

internal fun managementMovimiento() =
    Movimiento(
        "movement",
        TipoMovimiento.GASTO,
        Money(1234, Moneda.USD),
        LocalDate(2026, 9, 15),
        "food",
        "Lunch",
        null,
        Instant.parse("2026-09-15T10:00:00Z"),
        Cuenta.defaultCashId(Moneda.USD),
        MedioPago.EFECTIVO,
    )

internal fun managementFailure() = DataOutcome.Failure(DataError.Local(IllegalStateException()))
