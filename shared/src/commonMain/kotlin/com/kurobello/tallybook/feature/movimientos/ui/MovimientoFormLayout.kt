package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.save_expense
import tallybook.shared.generated.resources.save_income

@Composable
internal fun MovimientoFormHeader(title: String, headerAction: (@Composable () -> Unit)?) {
  if (headerAction == null) TallybookSheetTitle(title)
  else {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookSheetTitle(title, modifier = Modifier.weight(1f))
      headerAction()
    }
  }
}

@Composable
internal fun MovimientoFormSubmit(
    uiState: MovimientoFormState,
    onSubmit: () -> Unit,
    presentation: MovimientoFormPresentation?,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
  val submitLabel = presentation?.submitLabel ?: uiState.tipo.saveLabel()
  val isReady = presentation == null || (!uiState.isLoading && presentation.isSubmissionEnabled)
  TallybookButton(
      onClick = onSubmit,
      modifier = modifier.fillMaxWidth().semantics { contentDescription = submitLabel },
      enabled = interactionEnabled && !uiState.isSubmitting && isReady,
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (uiState.isSubmitting)
          TallybookProgressIndicator(
              contentDescription = stringResource(Res.string.loading),
              modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
          )
      else TallybookText(text = submitLabel)
    }
  }
}

@Composable
private fun TipoMovimiento.saveLabel(): String =
    when (this) {
      TipoMovimiento.GASTO -> stringResource(Res.string.save_expense)
      TipoMovimiento.INGRESO -> stringResource(Res.string.save_income)
    }
