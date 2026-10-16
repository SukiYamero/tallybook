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
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.category.CategoryTagField
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
import tallybook.shared.generated.resources.movimiento_summary_description_label
import tallybook.shared.generated.resources.movimiento_summary_description_quote
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_summary_save
import tallybook.shared.generated.resources.movimiento_summary_subtitle
import tallybook.shared.generated.resources.movimiento_summary_title

// The review sheet opens tall instead of hugging its content, so it reads as a confirmation step.
private const val REVIEW_HEIGHT_FRACTION = 0.72f

internal data class MovimientoSummaryCallbacks(
    val onRemoveCategoria: (Categoria) -> Unit,
    val onAddCategoria: () -> Unit,
    val onSuggestion: (Categoria) -> Unit,
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
internal fun MovimientoSummaryStep(
    summary: MovimientoSummaryUi,
    callbacks: MovimientoSummaryCallbacks,
    today: LocalDate,
    modifier: Modifier = Modifier,
    messageContent: @Composable () -> Unit = {},
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      SummaryHeader()
      SummaryTipoAndDate(summary, today)
      SummarySection(stringResource(Res.string.amount)) { SummaryAmount(summary) }
      if (summary.descripcion.isNotBlank()) {
        SummarySection(stringResource(Res.string.movimiento_summary_description_label)) {
          TallybookText(
              text =
                  stringResource(
                      Res.string.movimiento_summary_description_quote,
                      summary.descripcion,
                  ),
              color = TallybookTheme.colors.foreground.secondary,
              style = TallybookTheme.typography.body,
          )
        }
      }
      CategoryTagField(
          categorias = summary.categorias,
          max = MAX_CATEGORIAS_POR_MOVIMIENTO,
          onRemove = callbacks.onRemoveCategoria,
          onAdd = callbacks.onAddCategoria,
          pending = summary.categoriasArePending,
          suggestions = summary.suggestions,
          onSuggestion = callbacks.onSuggestion,
          enabled = !summary.isSubmitting,
      )
      messageContent()
      Spacer(Modifier.height(TallybookTheme.spacing.space32))
    }
    SummaryFooter(
        isSubmitting = summary.isSubmitting,
        onEdit = callbacks.onEdit,
        onSave = callbacks.onSave,
    )
  }
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
private fun SummaryTipoAndDate(summary: MovimientoSummaryUi, today: LocalDate) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
  ) {
    SummarySection(stringResource(Res.string.movement_type), Modifier.weight(1f)) {
      MovimientoTypeBadge(summary.tipo, size = MovimientoBadgeSize.Large)
    }
    SummarySection(stringResource(Res.string.date), Modifier.weight(1f)) {
      MovimientoDateBadge(summary.fecha, today, size = MovimientoBadgeSize.Large)
    }
  }
}

@Composable
private fun SummarySection(
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

@Composable
private fun SummaryFooter(isSubmitting: Boolean, onEdit: () -> Unit, onSave: () -> Unit) {
  val saveLabel = stringResource(Res.string.movimiento_summary_save)
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
        onClick = onEdit,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        enabled = !isSubmitting,
        colors = TallybookButtonDefaults.secondaryColors(),
        contentPadding = buttonPadding,
    ) {
      TallybookText(
          text = stringResource(Res.string.movimiento_summary_edit),
          style = buttonLabelStyle,
      )
    }
    TallybookButton(
        onClick = onSave,
        modifier = Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = saveLabel },
        enabled = !isSubmitting,
        contentPadding = buttonPadding,
    ) {
      if (isSubmitting) {
        TallybookProgressIndicator(
            contentDescription = stringResource(Res.string.loading),
            modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
        )
      } else {
        TallybookText(text = saveLabel, style = buttonLabelStyle)
      }
    }
  }
}
