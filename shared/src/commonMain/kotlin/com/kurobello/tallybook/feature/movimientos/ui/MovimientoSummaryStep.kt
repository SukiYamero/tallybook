package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategorySingleField
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeSize
import com.kurobello.tallybook.core.ui.movimiento.MovimientoTypeBadge
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.amount
import tallybook.shared.generated.resources.date
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movement_type
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_summary_save
import tallybook.shared.generated.resources.movimiento_summary_subtitle
import tallybook.shared.generated.resources.movimiento_summary_title
import tallybook.shared.generated.resources.movimiento_summary_title_label
import tallybook.shared.generated.resources.movimiento_summary_title_quote

// The review sheet opens tall instead of hugging its content, so it reads as a confirmation step.
private const val REVIEW_HEIGHT_FRACTION = 0.72f

internal data class MovimientoSummaryCallbacks(
    val onDeselectCategoria: () -> Unit,
    val onChooseCategoria: () -> Unit,
    val onSuggestion: (Categoria) -> Unit,
    val etiquetas: MovimientoEtiquetasCallbacks,
    val cuenta: MovimientoCuentaCallbacks = MovimientoCuentaCallbacks.Inert,
    val onEdit: () -> Unit,
    val onSave: () -> Unit,
)

@Composable
internal fun rememberReviewMinimumHeight(): Dp {
  val windowHeight = LocalWindowInfo.current.containerSize.height
  return with(LocalDensity.current) { windowHeight.toDp() } * REVIEW_HEIGHT_FRACTION
}

/**
 * Review content: the labeled sections scroll if the screen is short while the two actions stay
 * anchored at the bottom, at least `space32` below the content. Callers give it a minimum height to
 * make the sheet tall.
 */
@Composable
@Suppress("LongParameterList")
internal fun MovimientoSummaryStep(
    summary: MovimientoSummaryUi,
    callbacks: MovimientoSummaryCallbacks,
    today: LocalDate,
    modifier: Modifier = Modifier,
    supportingContent: @Composable () -> Unit = {},
    messageContent: @Composable () -> Unit = {},
) {
  val scrollState = rememberScrollState()
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      SummaryHeader()
      SummaryTipoAndDate(summary.tipo, summary.fecha, today)
      SummarySection(stringResource(Res.string.amount)) { SummaryAmount(summary) }
      if (summary.titulo.isNotBlank()) {
        SummarySection(stringResource(Res.string.movimiento_summary_title_label)) {
          TallybookText(
              text = stringResource(Res.string.movimiento_summary_title_quote, summary.titulo),
              color = TallybookTheme.colors.foreground.secondary,
              style = TallybookTheme.typography.body,
          )
        }
      }
      SummarySelection(summary, callbacks)
      supportingContent()
      messageContent()
      Spacer(Modifier.height(TallybookTheme.spacing.space32))
    }
    SummaryFooter(
        secondary =
            SummaryFooterAction(
                label = stringResource(Res.string.movimiento_summary_edit),
                onClick = callbacks.onEdit,
                enabled = !summary.isSubmitting,
            ),
        save =
            SummaryFooterAction(
                label = stringResource(Res.string.movimiento_summary_save),
                onClick = callbacks.onSave,
                enabled = !summary.isSubmitting,
            ),
        isSubmitting = summary.isSubmitting,
    )
  }
}

@Composable
private fun SummarySelection(summary: MovimientoSummaryUi, callbacks: MovimientoSummaryCallbacks) {
  MovimientoCuentaField(
      field = summary.cuenta,
      callbacks = callbacks.cuenta,
      enabled = !summary.isSubmitting,
  )
  CategorySingleField(
      categoria = summary.categoria,
      deselectable = true,
      onChoose = callbacks.onChooseCategoria,
      onSuggestion = callbacks.onSuggestion,
      onDeselect = callbacks.onDeselectCategoria,
      pending = summary.categoriaIsPending,
      suggestions = summary.suggestions,
      enabled = !summary.isSubmitting,
  )
  MovimientoEtiquetasField(
      etiquetas = summary.etiquetas,
      load = summary.etiquetasLoad,
      callbacks = callbacks.etiquetas,
      enabled = !summary.isSubmitting,
  )
}

@Composable
private fun SummaryHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookSheetTitle(stringResource(Res.string.movimiento_summary_title))
    TallybookText(
        text = stringResource(Res.string.movimiento_summary_subtitle),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
  }
}

@Composable
internal fun SummaryTipoAndDate(tipo: TipoMovimiento, fecha: LocalDate, today: LocalDate) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
  ) {
    SummarySection(stringResource(Res.string.movement_type), Modifier.weight(1f)) {
      MovimientoTypeBadge(tipo, size = MovimientoBadgeSize.Large)
    }
    SummarySection(stringResource(Res.string.date), Modifier.weight(1f)) {
      MovimientoDateBadge(fecha, today, size = MovimientoBadgeSize.Large)
    }
  }
}

@Composable
internal fun SummarySection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSectionHeading(label)
    content()
  }
}

internal data class SummaryFooterAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean,
)

/** The secondary action on the left and the save action on the right, at equal widths. */
@Composable
internal fun SummaryFooter(
    secondary: SummaryFooterAction,
    save: SummaryFooterAction,
    isSubmitting: Boolean,
) {
  val buttonPadding =
      PaddingValues(
          horizontal = TallybookTheme.spacing.space12,
          vertical = TallybookTheme.spacing.space12,
      )
  val buttonLabelStyle = TallybookTheme.typography.body.copy(textAlign = TextAlign.Center)
  Row(
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookButton(
        onClick = secondary.onClick,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        enabled = secondary.enabled,
        colors = TallybookButtonDefaults.secondaryColors(),
        contentPadding = buttonPadding,
    ) {
      TallybookText(text = secondary.label, style = buttonLabelStyle)
    }
    TallybookButton(
        onClick = save.onClick,
        modifier =
            Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = save.label },
        enabled = save.enabled,
        contentPadding = buttonPadding,
    ) {
      if (isSubmitting) {
        TallybookProgressIndicator(
            contentDescription = stringResource(Res.string.loading),
            modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
        )
      } else {
        TallybookText(text = save.label, style = buttonLabelStyle)
      }
    }
  }
}
