package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.TriangleAlert
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.category.CategorySuggestionChip
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.core.ui.theme.TallybookTypeScale
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_pending_label
import tallybook.shared.generated.resources.movimiento_summary_amount_missing
import tallybook.shared.generated.resources.movimiento_summary_amount_review
import tallybook.shared.generated.resources.movimiento_summary_description_quote
import tallybook.shared.generated.resources.movimiento_summary_edit
import tallybook.shared.generated.resources.movimiento_summary_save

private const val HEADER_SEPARATOR = " · "

@Suppress("LongParameterList")
@Composable
internal fun MovimientoSummaryStep(
    summary: MovimientoSummaryUi,
    onSelectSuggestion: (Categoria) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = deviceToday(),
) {
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    TallybookSurface(
        modifier = Modifier.fillMaxWidth(),
        style =
            TallybookSurfaceDefaults.cardStyle()
                .copy(
                    border =
                        BorderStroke(
                            TallybookTheme.dimensions.borderThin,
                            TallybookTheme.colors.border.subtle,
                        )
                ),
    ) {
      SummaryCardContent(summary, today, onSelectSuggestion)
    }
    SummaryFooter(isSubmitting = summary.isSubmitting, onEdit = onEdit, onSave = onSave)
  }
}

@Composable
private fun SummaryCardContent(
    summary: MovimientoSummaryUi,
    today: LocalDate,
    onSelectSuggestion: (Categoria) -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(TallybookTheme.spacing.space16),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookText(
        text = summaryHeader(summary, today),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.label,
    )
    SummaryAmount(summary)
    if (summary.descripcion.isNotBlank()) {
      TallybookText(
          text =
              stringResource(Res.string.movimiento_summary_description_quote, summary.descripcion),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.body,
      )
    }
    SummaryCategories(summary, onSelectSuggestion)
  }
}

@Composable
private fun summaryHeader(summary: MovimientoSummaryUi, today: LocalDate): String {
  val tipo =
      stringResource(
          if (summary.tipo == TipoMovimiento.INGRESO) Res.string.income else Res.string.expense
      )
  return tipo + HEADER_SEPARATOR + relativeDateLabel(summary.fecha, today)
}

@Composable
private fun SummaryAmount(summary: MovimientoSummaryUi) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    val monto = summary.monto
    if (monto == null) MissingAmountMessage() else SummaryAmountText(monto, summary.tipo)
    if (monto != null && summary.amountNeedsReview) AmountReviewHint()
  }
}

@Composable
private fun SummaryAmountText(monto: Money, tipo: TipoMovimiento) {
  TallybookText(
      text = signedMovimientoAmountText(monto, tipo),
      color =
          if (tipo == TipoMovimiento.INGRESO) TallybookTheme.colors.status.success.base
          else TallybookTheme.colors.foreground.primary,
      style = summaryAmountStyle(),
  )
}

@Composable
private fun MissingAmountMessage() {
  val message = stringResource(Res.string.movimiento_summary_amount_missing)
  TallybookText(
      text = message,
      modifier =
          Modifier.semantics {
            error(message)
            liveRegion = LiveRegionMode.Polite
          },
      color = TallybookTheme.colors.status.danger.base,
      style = TallybookTheme.typography.title,
  )
}

@Composable
private fun AmountReviewHint() {
  val hint = stringResource(Res.string.movimiento_summary_amount_review)
  Row(
      modifier =
          Modifier.semantics(mergeDescendants = true) {
            contentDescription = hint
            liveRegion = LiveRegionMode.Polite
          },
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.Top,
  ) {
    Image(
        imageVector = Lucide.TriangleAlert,
        contentDescription = null,
        modifier =
            Modifier.padding(top = TallybookTheme.spacing.space2)
                .size(TallybookTheme.dimensions.iconSmall),
        colorFilter = ColorFilter.tint(TallybookTheme.colors.status.warning),
    )
    TallybookText(
        text = hint,
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
  }
}

@Composable
private fun SummaryCategories(
    summary: MovimientoSummaryUi,
    onSelectSuggestion: (Categoria) -> Unit,
) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    CategoryChip(
        categoria = summary.categoria,
        selected = true,
        onClick = {},
        enabled = false,
        pending = summary.categoriaIsPending,
    )
    if (summary.categoriaIsPending) {
      TallybookText(
          text = stringResource(Res.string.movimiento_pending_label),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
    summary.suggestions.forEach { suggestion ->
      CategorySuggestionChip(
          categoria = suggestion,
          selected = false,
          onClick = { onSelectSuggestion(suggestion) },
          enabled = !summary.isSubmitting,
      )
    }
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

@Composable
@ReadOnlyComposable
private fun summaryAmountStyle(): TextStyle =
    TallybookTheme.typography.headline.copy(
        fontSize = TallybookTypeScale.size28,
        lineHeight = TallybookTypeScale.size30,
    )
