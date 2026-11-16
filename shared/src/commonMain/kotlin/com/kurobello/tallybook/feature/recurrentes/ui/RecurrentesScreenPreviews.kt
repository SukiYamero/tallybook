package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.recurrentes.viewmodel.OccurrenceSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesContent
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesUiState
import com.kurobello.tallybook.feature.recurrentes.viewmodel.toReady
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_sueldo

@Suppress("TooManyFunctions")
internal object PreviewRecurrentesActions : RecurrentesActions {
  override fun onRetry() = Unit

  override fun onOpenOccurrence(recurrenciaId: String) = false

  override fun onOpenRecurrencia(recurrenciaId: String) = false

  override fun onOccurrenceAmountChanged(value: String) = Unit

  override fun onOccurrenceFechaChanged(value: LocalDate) = Unit

  override fun onPaid() = Unit

  override fun onNotYet() = Unit

  override fun onSkip() = Unit

  override fun onEditFromOccurrence() = Unit

  override fun onFrecuenciaChanged(frecuencia: Frecuencia) = Unit

  override fun onRequestDelete() = Unit

  override fun onConfirmDelete() = Unit

  override fun onCancelDelete() = Unit

  override fun onRequestCloseSheet() = Unit

  override fun onSheetHidden() = Unit

  override fun onTipoChanged(tipo: TipoMovimiento) = Unit

  override fun onAmountChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onCategoriaSelected(id: String) = Unit

  override fun onTituloChanged(value: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onRetryLoad() = Unit

  override fun onSubmit() = Unit

  override fun onEtiquetasChanged(ids: List<String>) = Unit

  override fun onEtiquetaRemoved(id: String) = Unit

  override fun onCreateEtiqueta(nombre: String) = Unit

  override fun onRetryEtiquetas() = Unit

  override fun onMedioSelected(medio: MedioPago) = Unit

  override fun onCuentaSelected(id: String) = Unit
}

@Suppress("LongParameterList")
private fun previewRecurrencia(
    id: String,
    tipo: TipoMovimiento,
    minorUnits: Long,
    categoriaId: String,
    titulo: String?,
    fechaAncla: LocalDate,
    proximaFecha: LocalDate,
) =
    Recurrencia(
        id = id,
        tipo = tipo,
        monto = Money(minorUnits, Moneda.COP),
        categoriaId = categoriaId,
        titulo = titulo,
        descripcion = null,
        cuentaId = Cuenta.defaultCashId(Moneda.COP),
        medio = if (tipo == TipoMovimiento.GASTO) MedioPago.DEBITO else MedioPago.TRANSFERENCIA,
        etiquetaIds = emptyList(),
        frecuencia = Frecuencia.MENSUAL,
        fechaAncla = fechaAncla,
        proximaFecha = proximaFecha,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

@Composable
@Suppress("MagicNumber")
private fun previewState(): RecurrentesUiState {
  val today = LocalDate(2026, 11, 15)
  val categorias =
      listOf(
          Categoria(
              "cat_hogar",
              stringResource(Res.string.category_hogar),
              null,
              CategoryIcon.HOUSE,
              CategoryTint.BLUE,
          ),
          Categoria(
              "cat_sueldo",
              stringResource(Res.string.category_sueldo),
              null,
              CategoryIcon.BRIEFCASE,
              CategoryTint.EMERALD,
          ),
      )
  val rent =
      previewRecurrencia(
          "rent",
          TipoMovimiento.GASTO,
          120_000_000,
          "cat_hogar",
          "Arriendo",
          LocalDate(2026, 9, 5),
          LocalDate(2026, 10, 5),
      )
  val salary =
      previewRecurrencia(
          "salary",
          TipoMovimiento.INGRESO,
          450_000_000,
          "cat_sueldo",
          null,
          LocalDate(2026, 9, 30),
          LocalDate(2026, 11, 30),
      )
  val live = listOf(rent, salary).map { RecurrenciaWithPending(it, it.pendingThrough(today)) }
  return RecurrentesUiState(today = today, content = live.toReady(categorias))
}

@Preview
@Composable
internal fun RecurrentesScreenLightPreview() {
  TallybookTheme(darkTheme = false) {
    RecurrentesScreen(previewState(), PreviewRecurrentesActions, onBack = {})
  }
}

@Preview
@Composable
internal fun RecurrentesScreenDarkPreview() {
  TallybookTheme(darkTheme = true) {
    RecurrentesScreen(previewState(), PreviewRecurrentesActions, onBack = {})
  }
}

@Preview
@Composable
internal fun RecurrentesScreenEmptyPreview() {
  TallybookTheme {
    RecurrentesScreen(
        previewState()
            .copy(content = RecurrentesContent.Ready(emptyList(), emptyList(), emptyMap())),
        PreviewRecurrentesActions,
        onBack = {},
    )
  }
}

@Preview
@Composable
internal fun OccurrenceSheetPreview() {
  val state = previewState()
  val pending = requireNotNull(state.ready).pending.first()
  TallybookTheme {
    OccurrenceSheetContent(
        sheet = OccurrenceSheet(pending.recurrencia, pending.dueDate, state.today),
        categoria = state.ready?.categoriasById?.get(pending.recurrencia.categoriaId),
        actions = PreviewRecurrentesActions,
    )
  }
}
