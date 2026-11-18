package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.account.cuentaDisplayName
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.CategoryTagButton
import com.kurobello.tallybook.core.ui.category.CategoryTagIcon
import com.kurobello.tallybook.core.ui.category.CategoryTagLabel
import com.kurobello.tallybook.core.ui.category.addTagAppearance
import com.kurobello.tallybook.core.ui.category.selectedTagAppearance
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.movimiento.presentation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoReviewUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.ReviewPartUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_part_category
import tallybook.shared.generated.resources.movimiento_part_no_category
import tallybook.shared.generated.resources.movimiento_parts_join
import tallybook.shared.generated.resources.movimiento_parts_remove
import tallybook.shared.generated.resources.movimiento_parts_save
import tallybook.shared.generated.resources.movimiento_parts_title
import tallybook.shared.generated.resources.movimiento_parts_total
import tallybook.shared.generated.resources.movimiento_parts_total_excluded
import tallybook.shared.generated.resources.movimiento_summary_edit

internal const val REVIEW_STEP_TAG = "add-movimiento-review"

private const val DIFFERENCE_SEPARATOR = " · "
private const val ROW_TITULO_MAX_LINES = 2

internal data class MovimientoReviewCallbacks(
    val etiquetas: MovimientoEtiquetasCallbacks,
    val cuenta: MovimientoCuentaCallbacks = MovimientoCuentaCallbacks.Inert,
    val onPartEdit: (Int) -> Unit,
    val onPartCategoria: (Int) -> Unit,
    val onPartRemoved: (Int) -> Unit,
    val onJoin: () -> Unit,
    val onSave: () -> Unit,
)

/**
 * The parts of a split sentence under their total, then what they share (tipo, fecha, medio,
 * account, tags and, through [supportingContent], Repetir). Like the summary, the content scrolls
 * while Join and Save stay anchored at the bottom.
 */
@Composable
@Suppress("LongParameterList")
internal fun MovimientoReviewStep(
    review: MovimientoReviewUi,
    callbacks: MovimientoReviewCallbacks,
    today: LocalDate,
    modifier: Modifier = Modifier,
    supportingContent: @Composable () -> Unit = {},
    messageContent: @Composable () -> Unit = {},
) {
  val enabled = !review.isSubmitting
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
    Column(
        modifier =
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      ReviewHeader(review)
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
        review.parts.forEach { part ->
          key(part.index) {
            ReviewPartRow(part = part, review = review, today = today, callbacks = callbacks)
          }
        }
      }
      SummaryTipoAndDate(review.tipo, review.fecha, today)
      MovimientoCuentaField(field = review.cuenta, callbacks = callbacks.cuenta, enabled = enabled)
      MovimientoEtiquetasField(
          etiquetas = review.etiquetas,
          load = review.etiquetasLoad,
          callbacks = callbacks.etiquetas,
          enabled = enabled,
      )
      supportingContent()
      messageContent()
      Spacer(Modifier.height(TallybookTheme.spacing.space32))
    }
    SummaryFooter(
        secondary =
            SummaryFooterAction(
                label = stringResource(Res.string.movimiento_parts_join),
                onClick = callbacks.onJoin,
                enabled = review.canJoin,
            ),
        save =
            SummaryFooterAction(
                label = stringResource(Res.string.movimiento_parts_save, review.parts.size),
                onClick = callbacks.onSave,
                enabled = enabled,
            ),
        isSubmitting = review.isSubmitting,
    )
  }
}

@Composable
private fun ReviewHeader(review: MovimientoReviewUi) {
  val count = review.parts.size
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookSheetTitle(pluralStringResource(Res.plurals.movimiento_parts_title, count, count))
    review.total?.let { total ->
      TallybookText(
          text = stringResource(Res.string.movimiento_parts_total, signedAmountText(total)),
          color = amountColor(total),
          style = TallybookTheme.typography.bodyEmphasis,
      )
    }
    if (review.excludedFromTotal > 0) {
      TallybookText(
          text =
              pluralStringResource(
                  Res.plurals.movimiento_parts_total_excluded,
                  review.excludedFromTotal,
                  review.excludedFromTotal,
              ),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

private data class ReviewPartText(
    val amount: String,
    val categoryLabel: String,
    val differences: String?,
)

/**
 * One part as a card that opens its form. The category tag, the remove action and the amount review
 * hint are their own nodes; the card is announced as one unit with its category.
 */
@Composable
private fun ReviewPartRow(
    part: ReviewPartUi,
    review: MovimientoReviewUi,
    today: LocalDate,
    callbacks: MovimientoReviewCallbacks,
) {
  val enabled = !review.isSubmitting
  val categoria = part.categoria?.takeUnless { part.categoriaIsPending }
  val text =
      ReviewPartText(
          amount =
              part.monto?.let(::signedAmountText)
                  ?: stringResource(Res.string.movimiento_amount_not_detected),
          categoryLabel =
              categoria?.nombre ?: stringResource(Res.string.movimiento_part_no_category),
          differences = partDifferences(part, review, today),
      )
  ReviewPartCard(
      description =
          listOfNotNull(text.amount, part.titulo, text.categoryLabel, text.differences)
              .joinToString(),
      enabled = enabled,
      onClick = { callbacks.onPartEdit(part.index) },
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          text = text.amount,
          modifier = Modifier.weight(1f),
          color = part.monto?.let { amountColor(it) } ?: TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodyEmphasis,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      PartCategoryTag(
          categoria = categoria,
          label = text.categoryLabel,
          titulo = part.titulo,
          enabled = enabled,
          onClick = { callbacks.onPartCategoria(part.index) },
      )
      TallybookIconButton(
          onClick = { callbacks.onPartRemoved(part.index) },
          icon = Lucide.X,
          contentDescription = stringResource(Res.string.movimiento_parts_remove, part.titulo),
          enabled = enabled,
          style = TallybookIconButtonStyle.Ghost,
      )
    }
    ReviewPartDetails(part, text.differences)
  }
}

@Composable
private fun ReviewPartCard(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(
                  enabled = enabled,
                  onClickLabel = stringResource(Res.string.movimiento_summary_edit),
                  role = Role.Button,
                  interactionSource = interactionSource,
                  indication = null,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              )
              .semantics { contentDescription = description },
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
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = TallybookTheme.spacing.space16,
                    end = TallybookTheme.spacing.space4,
                    top = TallybookTheme.spacing.space4,
                    bottom = TallybookTheme.spacing.space12,
                )
    ) {
      content()
    }
  }
}

@Composable
private fun ReviewPartDetails(part: ReviewPartUi, differences: String?) {
  Column(
      modifier = Modifier.padding(end = TallybookTheme.spacing.space12),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
  ) {
    TallybookText(
        text = part.titulo,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
        maxLines = ROW_TITULO_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
    differences?.let {
      TallybookText(
          text = it,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
    if (part.monto != null && part.amountNeedsReview) AmountReviewHint()
  }
}

/** The part's category as a tag that opens its choice; a missing one reads "no category". */
@Composable
private fun PartCategoryTag(
    categoria: Categoria?,
    label: String,
    titulo: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val appearance =
      if (categoria == null) addTagAppearance(enabled)
      else selectedTagAppearance(categoria, pending = false, removable = true)
  val description = stringResource(Res.string.movimiento_part_category, titulo, label)
  CategoryTagButton(
      onClick = onClick,
      enabled = enabled,
      appearance = appearance,
      modifier = Modifier.semantics { contentDescription = description },
  ) {
    if (categoria != null) CategoryAvatar(categoria, Modifier.size(TallybookTheme.spacing.space28))
    CategoryTagLabel(label)
    CategoryTagIcon(Lucide.ChevronDown, appearance.accent)
  }
}

/** What the part's form moved away from the shared values, or null when it follows them all. */
@Composable
private fun partDifferences(
    part: ReviewPartUi,
    review: MovimientoReviewUi,
    today: LocalDate,
): String? {
  val labels =
      listOfNotNull(
          part.tipo.takeIf { it != review.tipo }?.let { stringResource(it.presentation.label) },
          part.fecha?.let { relativeDateLabel(it, today) },
          part.medio?.let { stringResource(it.labelResource) },
          part.cuenta?.let { cuentaDisplayName(it, review.cuenta.primaryMoneda) },
      )
  return labels.takeIf { it.isNotEmpty() }?.joinToString(DIFFERENCE_SEPARATOR)
}

private fun signedAmountText(monto: Money): String =
    if (monto.minorUnits > 0) "+${monto.format()}" else monto.format()

@Composable
private fun amountColor(monto: Money) =
    if (monto.minorUnits > 0) TallybookTheme.colors.status.success.base
    else TallybookTheme.colors.foreground.primary
