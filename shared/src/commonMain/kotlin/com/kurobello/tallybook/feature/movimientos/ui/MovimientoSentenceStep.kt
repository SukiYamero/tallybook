package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
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
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_phrase_continue
import tallybook.shared.generated.resources.movimiento_phrase_fill_manually
import tallybook.shared.generated.resources.movimiento_phrase_label
import tallybook.shared.generated.resources.movimiento_phrase_placeholder

internal const val PHRASE_INPUT_TAG = "movimiento_phrase_input"

private const val PHRASE_VISIBLE_LINES = 3

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
 * scrolls, because the height available above the keyboard bounds the dialog. The Detected card
 * fades in once the sentence has text; the dialog animates the height it adds.
 */
@Composable
internal fun MovimientoSentenceStep(
    sentence: MovimientoSentenceUi,
    callbacks: MovimientoSentenceCallbacks,
    modifier: Modifier = Modifier,
) {
  // A new window hands initial focus to its first focusable node and opens the keyboard with it;
  // the container takes that focus instead, so the input only focuses when the user taps it.
  Column(
      modifier = modifier.fillMaxWidth().focusable(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSheetTitle(stringResource(Res.string.add_movimiento_pane_title))
    Column(
        modifier =
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
    ) {
      // No spacing here: the card owns its top padding so a hidden card leaves no extra gap.
      Column(modifier = Modifier.fillMaxWidth()) {
        SentenceInput(sentence, callbacks)
        AnimatedVisibility(
            visible = sentence.text.isNotBlank(),
            enter = fadeIn(tween(TallybookTheme.motion.fadeMillis)),
            exit = fadeOut(tween(TallybookTheme.motion.fadeMillis)),
        ) {
          DetectedArea(sentence, callbacks, Modifier.padding(top = TallybookTheme.spacing.space16))
        }
      }
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
) {
  TallybookTextField(
      value = sentence.text,
      onValueChange = { callbacks.onTextChange(it.take(MOVIMIENTO_TITLE_MAX_LENGTH)) },
      modifier = Modifier.fillMaxWidth(),
      label = stringResource(Res.string.movimiento_phrase_label),
      placeholder =
          stringResource(
              Res.string.movimiento_phrase_placeholder,
              phraseSampleAmount(sentence.moneda),
          ),
      message =
          TallybookFieldMessage.Supporting("${sentence.text.length}/$MOVIMIENTO_TITLE_MAX_LENGTH"),
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
      inputModifier = Modifier.fillMaxWidth().testTag(PHRASE_INPUT_TAG),
  )
}

@Composable
private fun DetectedArea(
    sentence: MovimientoSentenceUi,
    callbacks: MovimientoSentenceCallbacks,
    modifier: Modifier = Modifier,
) {
  val detected = sentence.detected
  TallybookSurface(
      modifier = modifier.fillMaxWidth(),
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
        val monto = detected.monto
        if (monto == null) AmountNotDetected() else DetectedAmount(monto, detected.tipo)
      }
      CategoryTagField(
          categorias = sentence.categorias,
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
private fun AmountNotDetected() {
  TallybookText(
      text = stringResource(Res.string.movimiento_amount_not_detected),
      color = TallybookTheme.colors.foreground.tertiary,
      style = TallybookTheme.typography.bodySmall,
  )
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
