package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.ui.component.TallybookSwitchRow
import com.kurobello.tallybook.core.ui.recurrencia.FrecuenciaField
import com.kurobello.tallybook.core.ui.recurrencia.RepetirField
import com.kurobello.tallybook.feature.home.HomeRecurrentesRow
import com.kurobello.tallybook.feature.recurrentes.ui.PendingOccurrenceRow
import com.kurobello.tallybook.feature.recurrentes.ui.ScheduledRecurrenciaRow
import com.kurobello.tallybook.feature.recurrentes.viewmodel.PendingOccurrence
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_recurrentes_pending
import tallybook.shared.generated.resources.recurrencia_frequency_label
import tallybook.shared.generated.resources.recurrencia_repeat_label
import tallybook.shared.generated.resources.recurrentes_pending_heading
import tallybook.shared.generated.resources.recurrentes_scheduled_heading
import tallybook.shared.generated.resources.recurrentes_title

private const val CATALOG_PENDING_COUNT = 2

internal fun LazyListScope.catalogRecurrentes(fixtures: UiCatalogFixtures) {
  catalogControlSection("recurrentes", { stringResource(Res.string.recurrentes_title) }) {
    CatalogRecurrentes(fixtures)
  }
}

@Composable
private fun CatalogRecurrentes(fixtures: UiCatalogFixtures) {
  CatalogSurface {
    CatalogRepetirControls()
    CatalogRecurrenciaRows(fixtures)
  }
}

@Composable
private fun CatalogRepetirControls() {
  var frecuencia by remember { mutableStateOf<Frecuencia?>(Frecuencia.MENSUAL) }
  CatalogSample(label = stringResource(Res.string.recurrencia_repeat_label)) {
    RepetirField(
        frecuencia = frecuencia,
        onRepetirChanged = { frecuencia = if (it) Frecuencia.MENSUAL else null },
        onFrecuenciaChanged = { frecuencia = it },
    )
    TallybookSwitchRow(
        label = stringResource(Res.string.recurrencia_repeat_label),
        checked = false,
        onCheckedChange = {},
        enabled = false,
    )
  }
  CatalogSample(label = stringResource(Res.string.recurrencia_frequency_label)) {
    FrecuenciaField(selected = Frecuencia.ANUAL, onSelect = {}, enabled = false)
  }
}

@Composable
private fun CatalogRecurrenciaRows(fixtures: UiCatalogFixtures) {
  val recurrencia = remember(fixtures) { fixtures.recurrencia() }
  CatalogSample(
      label =
          pluralStringResource(
              Res.plurals.home_recurrentes_pending,
              CATALOG_PENDING_COUNT,
              CATALOG_PENDING_COUNT,
          )
  ) {
    HomeRecurrentesRow(count = CATALOG_PENDING_COUNT, onClick = {}, Modifier.fillMaxWidth())
  }
  CatalogSample(
      label =
          pluralStringResource(
              Res.plurals.recurrentes_pending_heading,
              CATALOG_PENDING_COUNT,
              CATALOG_PENDING_COUNT,
          )
  ) {
    PendingOccurrenceRow(
        occurrence = PendingOccurrence(recurrencia, recurrencia.proximaFecha, isNext = true),
        categoria = fixtures.categoria,
        onOpen = {},
        modifier = Modifier.fillMaxWidth(),
    )
    PendingOccurrenceRow(
        occurrence =
            PendingOccurrence(
                recurrencia,
                recurrencia.proximaFecha.plus(1, DateTimeUnit.MONTH),
                isNext = false,
            ),
        categoria = fixtures.categoria,
        onOpen = {},
        modifier = Modifier.fillMaxWidth(),
    )
  }
  CatalogSample(label = stringResource(Res.string.recurrentes_scheduled_heading)) {
    ScheduledRecurrenciaRow(
        recurrencia = recurrencia,
        categoria = fixtures.categoria,
        onOpen = {},
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

private fun UiCatalogFixtures.recurrencia(): Recurrencia =
    with(movimiento) {
      Recurrencia(
          id = "catalog-recurrencia",
          tipo = tipo,
          monto = monto,
          categoriaId = categoria.id,
          titulo = titulo,
          descripcion = descripcion,
          cuentaId = cuentaId,
          medio = medio,
          etiquetaIds = etiquetaIds,
          frecuencia = Frecuencia.MENSUAL,
          fechaAncla = fecha.minus(1, DateTimeUnit.MONTH),
          proximaFecha = fecha,
          createdAt = createdAt,
      )
    }
