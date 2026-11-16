package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.message
import com.kurobello.tallybook.core.ui.date.fullCivilDate
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoAmountField
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoAmountFieldCallbacks
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoAmountFieldState
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoDateField
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoDateFieldState
import com.kurobello.tallybook.feature.recurrentes.viewmodel.OccurrenceAction
import com.kurobello.tallybook.feature.recurrentes.viewmodel.OccurrenceError
import com.kurobello.tallybook.feature.recurrentes.viewmodel.OccurrenceSheet
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.asMovimiento
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.recurrencia_confirm_body
import tallybook.shared.generated.resources.recurrencia_confirm_error
import tallybook.shared.generated.resources.recurrencia_confirm_template_error
import tallybook.shared.generated.resources.recurrencia_edit
import tallybook.shared.generated.resources.recurrencia_not_yet
import tallybook.shared.generated.resources.recurrencia_paid
import tallybook.shared.generated.resources.recurrencia_pending_since
import tallybook.shared.generated.resources.recurrencia_skip
import tallybook.shared.generated.resources.recurrencia_skip_error
import tallybook.shared.generated.resources.unknown_category

/** Pagado takes the amount and fecha shown; the template's other fields change in its own sheet. */
@Composable
internal fun OccurrenceSheetContent(
    sheet: OccurrenceSheet,
    categoria: Categoria?,
    actions: RecurrentesActions,
) {
  val focusManager = LocalFocusManager.current
  val enabled = sheet.isEditable
  TallybookSheetForm(
      header = { OccurrenceHeader(sheet, categoria, enabled, actions) },
      footer = { OccurrenceFooter(sheet, actions) },
  ) {
    TallybookText(
        stringResource(Res.string.recurrencia_confirm_body),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
    MovimientoAmountField(
        state =
            MovimientoAmountFieldState(
                value = sheet.amountInput,
                moneda = sheet.moneda,
                error = sheet.amountError?.message(),
            ),
        enabled = enabled,
        callbacks =
            MovimientoAmountFieldCallbacks(
                onValueChange = actions::onOccurrenceAmountChanged,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                inputModifier = Modifier.revealWhenFocused(),
            ),
    )
    MovimientoDateField(
        state = MovimientoDateFieldState(sheet.fecha, enabled, sheet.today),
        onFechaChanged = actions::onOccurrenceFechaChanged,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    sheet.error?.let { OccurrenceErrorMessage(it, enabled, actions::onEditFromOccurrence) }
  }
}

@Composable
private fun OccurrenceHeader(
    sheet: OccurrenceSheet,
    categoria: Categoria?,
    enabled: Boolean,
    actions: RecurrentesActions,
) {
  val categoryLabel = categoria?.nombre ?: stringResource(Res.string.unknown_category)
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      TallybookSheetTitle(sheet.recurrencia.asMovimiento().rowTitle(categoryLabel))
      TallybookText(
          stringResource(Res.string.recurrencia_pending_since, fullCivilDate(sheet.dueDate)),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
    TallybookIconButton(
        onClick = actions::onEditFromOccurrence,
        icon = Lucide.Pencil,
        contentDescription = stringResource(Res.string.recurrencia_edit),
        enabled = enabled,
    )
  }
}

/** Pagado leads full width; Todavía no and Saltar share the row under it at any text size. */
@Composable
private fun OccurrenceFooter(sheet: OccurrenceSheet, actions: RecurrentesActions) {
  val enabled = sheet.isEditable
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookButton(
        onClick = actions::onPaid,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {
      BusyLabel(Res.string.recurrencia_paid, sheet.busy == OccurrenceAction.CONFIRM)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      TallybookTextButton(
          onClick = actions::onNotYet,
          modifier = Modifier.weight(1f),
          enabled = enabled,
      ) {
        TallybookText(stringResource(Res.string.recurrencia_not_yet))
      }
      TallybookButton(
          onClick = actions::onSkip,
          modifier = Modifier.weight(1f),
          enabled = enabled,
          colors = TallybookButtonDefaults.secondaryColors(),
      ) {
        BusyLabel(Res.string.recurrencia_skip, sheet.busy == OccurrenceAction.SKIP)
      }
    }
  }
}

@Composable
private fun BusyLabel(label: StringResource, busy: Boolean) {
  Box(contentAlignment = Alignment.Center) {
    if (busy)
        TallybookProgressIndicator(
            contentDescription = stringResource(Res.string.loading),
            modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
        )
    else TallybookText(stringResource(label))
  }
}

@Composable
private fun OccurrenceErrorMessage(
    error: OccurrenceError,
    enabled: Boolean,
    onEdit: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        stringResource(occurrenceErrors.getValue(error)),
        color = TallybookTheme.colors.status.danger.base,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = TallybookTheme.typography.body,
    )
    if (error == OccurrenceError.TEMPLATE_INVALID) {
      TallybookTextButton(onClick = onEdit, enabled = enabled) {
        TallybookText(stringResource(Res.string.recurrencia_edit))
      }
    }
  }
}

private val occurrenceErrors: Map<OccurrenceError, StringResource> =
    mapOf(
        OccurrenceError.CONFIRM_FAILED to Res.string.recurrencia_confirm_error,
        OccurrenceError.TEMPLATE_INVALID to Res.string.recurrencia_confirm_template_error,
        OccurrenceError.SKIP_FAILED to Res.string.recurrencia_skip_error,
    )
