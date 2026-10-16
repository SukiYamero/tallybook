package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_phrase_placeholder

@Suppress("LongParameterList")
@Composable
internal fun MovimientoPhraseStep(
    text: String,
    preview: PhrasePreviewUi,
    moneda: Moneda,
    onTextChange: (String) -> Unit,
    onContinue: () -> Unit,
    onFillManually: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    today: LocalDate = deviceToday(),
    enabled: Boolean = true,
    requestFocusOnStart: Boolean = true,
) {
  if (requestFocusOnStart) {
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
  }
  val canContinue = enabled && text.isNotBlank()
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookTextField(
        value = text,
        onValueChange = { onTextChange(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) },
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(Res.string.movimiento_phrase_label),
        placeholder =
            stringResource(Res.string.movimiento_phrase_placeholder, phraseSampleAmount(moneda)),
        message =
            TallybookFieldMessage.Supporting("${text.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH"),
        enabled = enabled,
        singleLine = false,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { if (canContinue) onContinue() }),
        inputModifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
    MovimientoPhrasePreview(preview = preview, today = today)
    TallybookButton(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        enabled = canContinue,
    ) {
      TallybookText(stringResource(Res.string.movimiento_phrase_continue))
    }
    TallybookTextButton(
        onClick = onFillManually,
        modifier = Modifier.align(Alignment.CenterHorizontally),
        enabled = enabled,
    ) {
      TallybookText(stringResource(Res.string.movimiento_phrase_fill_manually))
    }
  }
}

@Composable
private fun MovimientoPhrasePreview(preview: PhrasePreviewUi, today: LocalDate) {
  val monto = preview.monto
  val fecha = preview.fecha
  val categoria = preview.categoria
  if (monto == null && fecha == null && categoria == null) return
  FlowRow(
      modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    if (monto != null) PhraseAmount(monto, preview.tipo)
    if (categoria != null) PhraseCategoryTag(categoria)
    if (fecha != null) {
      TallybookText(
          text = relativeDateLabel(fecha, today),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun PhraseAmount(monto: Money, tipo: TipoMovimiento?) {
  TallybookText(
      text = tipo?.let { signedMovimientoAmountText(monto, it) } ?: monto.format(),
      color =
          if (tipo == TipoMovimiento.INGRESO) TallybookTheme.colors.status.success.base
          else TallybookTheme.colors.foreground.primary,
      style = TallybookTheme.typography.bodyEmphasis,
  )
}

@Composable
private fun PhraseCategoryTag(categoria: Categoria) {
  val tint = categoria.color.tintColors()
  TallybookSurface(
      style =
          TallybookSurfaceStyle(
              color = tint.container,
              contentColor = tint.content,
              shape = RoundedCornerShape(TallybookTheme.radii.radius20),
              border = BorderStroke(TallybookTheme.dimensions.borderThin, tint.border),
          )
  ) {
    Row(
        modifier =
            Modifier.padding(
                start = TallybookTheme.spacing.space4,
                end = TallybookTheme.spacing.space12,
                top = TallybookTheme.spacing.space4,
                bottom = TallybookTheme.spacing.space4,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CategoryAvatar(categoria, Modifier.size(TallybookTheme.spacing.space28))
      TallybookText(text = categoria.nombre, style = TallybookTheme.typography.controlValue)
    }
  }
}
