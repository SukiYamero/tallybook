package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_delete
import tallybook.shared.generated.resources.movimiento_detail_title
import tallybook.shared.generated.resources.movimiento_edit
import tallybook.shared.generated.resources.unknown_category

@Composable
internal fun movimientoDetailDate(movimiento: Movimiento): String =
    stringResource(
        Res.string.calendar_date_content_description,
        movimiento.fecha.day,
        stringArrayResource(Res.array.date_picker_months)[movimiento.fecha.month.ordinal],
        movimiento.fecha.year,
    )

@Suppress("LongParameterList")
@Composable
internal fun MovimientoDetailContent(
    movimiento: Movimiento,
    categoria: Categoria?,
    isSubmitting: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    interactionEnabled: Boolean = true,
) {
  Column(modifier.fillMaxWidth().padding(horizontal = TallybookTheme.dimensions.screenHorizontal)) {
    TallybookSheetTitle(stringResource(Res.string.movimiento_detail_title))
    Spacer(Modifier.height(TallybookTheme.spacing.space16))
    Column(
        Modifier.weight(1f, fill = false)
            .heightIn(max = TallybookTheme.dimensions.movimientoFormMaximumHeight)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      TallybookText(
          signedMovimientoAmountText(movimiento.monto, movimiento.tipo),
          style = TallybookTheme.typography.title,
      )
      TallybookText(movimiento.monto.moneda.name)
      TallybookText(
          stringResource(
              if (movimiento.tipo == TipoMovimiento.GASTO) Res.string.expense else Res.string.income
          )
      )
      TallybookText(movimientoDetailDate(movimiento))
      TallybookText(categoria?.nombre ?: stringResource(Res.string.unknown_category))
      movimiento.descripcion?.let { TallybookText(it) }
      error?.let { TallybookText(it, color = TallybookTheme.colors.status.danger.base) }
    }
    Spacer(Modifier.height(TallybookTheme.spacing.space18))
    if (isSubmitting)
        TallybookProgressIndicator(contentDescription = stringResource(Res.string.loading))
    TallybookButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        enabled = interactionEnabled && !isSubmitting,
    ) {
      TallybookText(stringResource(Res.string.movimiento_edit))
    }
    TallybookTextButton(
        onClick = onDelete,
        modifier = Modifier.fillMaxWidth(),
        enabled = interactionEnabled && !isSubmitting,
    ) {
      TallybookText(
          stringResource(Res.string.movimiento_delete),
          color = TallybookTheme.colors.status.danger.base,
      )
    }
  }
}
