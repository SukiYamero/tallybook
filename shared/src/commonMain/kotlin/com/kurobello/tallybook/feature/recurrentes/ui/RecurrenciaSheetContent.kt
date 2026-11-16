package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.ui.component.TallybookDateChipPicker
import com.kurobello.tallybook.core.ui.component.TallybookDateChipSize
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.recurrencia.FrecuenciaField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoForm
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoFormPresentation
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoFormSubmit
import com.kurobello.tallybook.feature.movimientos.viewmodel.EditMovimientoUiState
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrenciaSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesUiState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_cancel
import tallybook.shared.generated.resources.movimiento_save_changes
import tallybook.shared.generated.resources.recurrencia_delete
import tallybook.shared.generated.resources.recurrencia_delete_error
import tallybook.shared.generated.resources.recurrencia_next_date
import tallybook.shared.generated.resources.recurrencia_schedule_locked

/**
 * The movimiento form edits the template; its date field gives way to the schedule, which stays
 * disabled with its reason while an occurrence is pending.
 */
@Composable
internal fun RecurrenciaSheetContent(
    sheet: RecurrenciaSheet,
    uiState: RecurrentesUiState,
    title: String,
    actions: RecurrentesActions,
) {
  val categorias = uiState.ready?.categoriasById?.values?.toList().orEmpty()
  val form = sheet.form(uiState.catalog, categorias)
  val enabled = sheet.canInteract
  val presentation =
      MovimientoFormPresentation(title, stringResource(Res.string.movimiento_save_changes))
  MovimientoForm(
      uiState = form,
      actions = actions,
      presentation = presentation,
      headerAction = {
        TallybookIconButton(
            onClick = actions::onRequestDelete,
            icon = Lucide.Trash2,
            contentDescription = stringResource(Res.string.recurrencia_delete),
            colors = TallybookIconButtonDefaults.destructiveColors(),
            enabled = enabled,
        )
      },
      supportingContent = { if (sheet.deleteFailed) DeleteError() },
      footerContent = { RecurrenciaFooter(form, enabled, presentation, actions) },
      dateContent = {
        RecurrenciaSchedule(
            frecuencia = sheet.frecuencia,
            nextDate = form.fecha,
            maxDate = sheet.edit.maxSelectableDate,
            locked = uiState.isScheduleLocked(sheet),
            enabled = enabled && !form.isLoading,
            actions = actions,
        )
      },
      interactionEnabled = enabled,
  )
}

@Composable
@Suppress("LongParameterList")
private fun RecurrenciaSchedule(
    frecuencia: Frecuencia,
    nextDate: LocalDate,
    maxDate: LocalDate,
    locked: Boolean,
    enabled: Boolean,
    actions: RecurrentesActions,
) {
  val nextDateLabel = stringResource(Res.string.recurrencia_next_date)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16)) {
    FrecuenciaField(
        selected = frecuencia,
        onSelect = actions::onFrecuenciaChanged,
        enabled = enabled && !locked,
    )
    Column(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing),
    ) {
      TallybookInputLabel(text = nextDateLabel)
      TallybookDateChipPicker(
          value = nextDate,
          onValueChange = actions::onFechaChanged,
          label = nextDateLabel,
          size = TallybookDateChipSize.M,
          enabled = enabled && !locked,
          maxDate = maxDate,
      )
    }
    if (locked) {
      TallybookText(
          stringResource(Res.string.recurrencia_schedule_locked),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}

@Composable
private fun RecurrenciaFooter(
    form: EditMovimientoUiState,
    enabled: Boolean,
    presentation: MovimientoFormPresentation,
    actions: RecurrentesActions,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookTextButton(
        onClick = actions::onRequestCloseSheet,
        modifier = Modifier.weight(1f),
        enabled = enabled,
    ) {
      TallybookText(stringResource(Res.string.movimiento_cancel))
    }
    MovimientoFormSubmit(
        uiState = form,
        onSubmit = actions::onSubmit,
        presentation = presentation,
        interactionEnabled = enabled,
        modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun DeleteError() {
  TallybookText(
      stringResource(Res.string.recurrencia_delete_error),
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}
