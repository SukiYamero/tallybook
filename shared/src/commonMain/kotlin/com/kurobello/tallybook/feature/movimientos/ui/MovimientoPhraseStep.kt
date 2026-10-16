package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryTag
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
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

internal const val PHRASE_INPUT_TAG = "movimiento_phrase_input"

// The field and the preview row keep a constant size so typing never resizes the sheet; overflow
// scrolls inside the body instead of pushing Continue away.
private const val PHRASE_VISIBLE_LINES = 3

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
    today: LocalDate,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionsEnabled: Boolean = true,
    requestFocusOnStart: Boolean = true,
) {
  if (requestFocusOnStart) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusRequester) {
      // The modal window is still attaching on first composition; focusing a frame later lets the
      // keyboard open with it.
      withFrameNanos {}
      focusRequester.requestFocus()
      keyboard?.show()
    }
  }
  val canContinue = enabled && actionsEnabled && text.isNotBlank()
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
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
          minLines = PHRASE_VISIBLE_LINES,
          maxLines = PHRASE_VISIBLE_LINES,
          keyboardOptions =
              KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  imeAction = ImeAction.Done,
              ),
          keyboardActions = KeyboardActions(onDone = { if (canContinue) onContinue() }),
          inputModifier =
              Modifier.fillMaxWidth().focusRequester(focusRequester).testTag(PHRASE_INPUT_TAG),
      )
      MovimientoPhrasePreview(preview = preview, today = today)
    }
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
        enabled = enabled && actionsEnabled,
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
  Box(
      modifier = Modifier.fillMaxWidth().heightIn(min = TallybookTheme.spacing.space40),
      contentAlignment = Alignment.CenterStart,
  ) {
    if (monto != null || fecha != null || categoria != null) {
      PhrasePreviewRow(preview, today)
    }
  }
}

@Composable
private fun PhrasePreviewRow(preview: PhrasePreviewUi, today: LocalDate) {
  val monto = preview.monto
  val fecha = preview.fecha
  val categoria = preview.categoria
  FlowRow(
      modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    if (monto != null) PhraseAmount(monto, preview.tipo)
    if (categoria != null) CategoryTag(categoria)
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
