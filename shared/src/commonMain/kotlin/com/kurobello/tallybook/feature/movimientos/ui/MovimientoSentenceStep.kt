package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryTagField
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.movimiento.MovimientoTypeBadge
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSentenceUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_phrase_placeholder

internal const val PHRASE_INPUT_TAG = "movimiento_phrase_input"

private const val PHRASE_VISIBLE_LINES = 2

internal data class MovimientoSentenceCallbacks(
    val onTextChange: (String) -> Unit,
    val onContinue: () -> Unit,
    val onFillManually: () -> Unit,
    val onRemoveCategoria: (Categoria) -> Unit,
    val onAddCategoria: () -> Unit,
    val onSuggestion: (Categoria) -> Unit,
)

/**
 * Content of the sentence dialog. The title and the Continue footer stay put and the middle
 * scrolls, because the dialog keeps its anchor when the keyboard opens and only the height
 * available to it shrinks.
 */
@Composable
internal fun MovimientoSentenceStep(
    sentence: MovimientoSentenceUi,
    callbacks: MovimientoSentenceCallbacks,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
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
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(stringResource(Res.string.add_movimiento_pane_title))
    Column(
        modifier =
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      SentenceInput(sentence, callbacks, focusRequester)
      DetectedArea(sentence, callbacks)
      VoiceDictationPlaceholder()
      TallybookTextButton(
          onClick = callbacks.onFillManually,
          modifier = Modifier.align(Alignment.CenterHorizontally),
          enabled = sentence.isEnabled && sentence.actionsEnabled,
      ) {
        TallybookText(stringResource(Res.string.movimiento_phrase_fill_manually))
      }
    }
    TallybookButton(
        onClick = callbacks.onContinue,
        modifier = Modifier.fillMaxWidth(),
        enabled = sentence.canContinue,
    ) {
      TallybookText(stringResource(Res.string.movimiento_phrase_continue))
    }
  }
}

@Composable
private fun SentenceInput(
    sentence: MovimientoSentenceUi,
    callbacks: MovimientoSentenceCallbacks,
    focusRequester: FocusRequester,
) {
  TallybookTextField(
      value = sentence.text,
      onValueChange = { callbacks.onTextChange(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) },
      modifier = Modifier.fillMaxWidth(),
      label = stringResource(Res.string.movimiento_phrase_label),
      placeholder =
          stringResource(
              Res.string.movimiento_phrase_placeholder,
              phraseSampleAmount(sentence.moneda),
          ),
      message =
          TallybookFieldMessage.Supporting(
              "${sentence.text.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH"
          ),
      enabled = sentence.isEnabled,
      singleLine = false,
      minLines = PHRASE_VISIBLE_LINES,
      maxLines = PHRASE_VISIBLE_LINES,
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done,
          ),
      keyboardActions =
          KeyboardActions(onDone = { if (sentence.canContinue) callbacks.onContinue() }),
      inputModifier =
          Modifier.fillMaxWidth().focusRequester(focusRequester).testTag(PHRASE_INPUT_TAG),
  )
}

@Composable
private fun DetectedArea(sentence: MovimientoSentenceUi, callbacks: MovimientoSentenceCallbacks) {
  val detected = sentence.detected
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.card,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius16),
              border =
                  BorderStroke(
                      TallybookTheme.dimensions.borderThin,
                      TallybookTheme.colors.border.base,
                  ),
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(TallybookTheme.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      TallybookSectionHeading(stringResource(Res.string.movimiento_detected_label))
      FlowRow(
          modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
          itemVerticalAlignment = Alignment.CenterVertically,
      ) {
        MovimientoTypeBadge(detected.tipo)
        MovimientoDateBadge(detected.fecha, sentence.today)
        detected.monto?.let { DetectedAmount(it, detected.tipo) }
      }
      CategoryTagField(
          categorias = sentence.categorias,
          max = MAX_CATEGORIAS_POR_MOVIMIENTO,
          onRemove = callbacks.onRemoveCategoria,
          onAdd = callbacks.onAddCategoria,
          pending = sentence.categoriasArePending,
          suggestions = sentence.suggestions,
          onSuggestion = callbacks.onSuggestion,
          enabled = sentence.isEnabled,
      )
    }
  }
}

@Composable
private fun DetectedAmount(monto: Money, tipo: TipoMovimiento) {
  TallybookText(
      text = signedMovimientoAmountText(monto, tipo),
      color =
          if (tipo == TipoMovimiento.INGRESO) TallybookTheme.colors.status.success.base
          else TallybookTheme.colors.foreground.primary,
      style = TallybookTheme.typography.bodyEmphasis,
  )
}
