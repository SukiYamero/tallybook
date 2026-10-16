package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.CircleQuestionMark
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.neutralCategoryTintColors
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.core.ui.theme.TallybookTypeScale
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.movimiento_open_detail
import tallybook.shared.generated.resources.movimiento_pending_label
import tallybook.shared.generated.resources.movimiento_row_extra_categories

private const val PENDING_SUBTITLE_SEPARATOR = " · "

@Composable
@Suppress("LongParameterList")
internal fun MovimientoRowContent(
    movimiento: Movimiento,
    categoria: Categoria?,
    text: MovimientoRowText,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    pending: Boolean = false,
) {
  val amountText = signedMovimientoAmountText(movimiento.monto, movimiento.tipo)
  val pendingLabel = if (pending) stringResource(Res.string.movimiento_pending_label) else null
  val extraCategories = movimiento.categoriaIds.size - 1
  val description =
      movimientoRowDescription(movimiento, text, amountText, pendingLabel, extraCategories)
  val interactions = remember { MutableInteractionSource() }
  val isPressed by interactions.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val rowModifier = movimientoRowClickModifier(modifier, onClick, interactions, tapFeedback)
  TallybookSurface(
      modifier =
          rowModifier.semantics(mergeDescendants = true) { contentDescription = description },
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
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
    MovimientoRowDescription(
        categoria,
        text.copy(secondary = pendingSubtitle(pendingLabel, text.secondary)),
        RowDescriptionLayout(pending, extraCategories),
        Modifier.padding(
            horizontal = TallybookTheme.spacing.space14,
            vertical = TallybookTheme.spacing.space12,
        ),
    ) {
      TallybookText(
          amountText,
          modifier = Modifier.padding(top = TallybookTheme.spacing.space4),
          color =
              if (movimiento.tipo == TipoMovimiento.INGRESO)
                  TallybookTheme.colors.status.success.base
              else TallybookTheme.colors.foreground.primary,
          style = movementRowAmountStyle(),
      )
    }
  }
}

@Composable
private fun movimientoRowDescription(
    movimiento: Movimiento,
    text: MovimientoRowText,
    amountText: String,
    pendingLabel: String?,
    extraCategories: Int,
): String {
  val typeLabel =
      stringResource(
          if (movimiento.tipo == TipoMovimiento.INGRESO) Res.string.income else Res.string.expense
      )
  return listOfNotNull(
          text.primary,
          text.secondary,
          text.categoryLabel.takeUnless { it == text.primary },
          pendingLabel,
          extraCategoriesLabel(extraCategories),
          typeLabel,
          amountText,
          movimiento.monto.moneda.name,
      )
      .joinToString(", ")
}

@Composable
private fun movimientoRowClickModifier(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    interactions: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier {
  val label = stringResource(Res.string.movimiento_open_detail)
  return if (onClick == null) modifier
  else
      modifier.clickable(
          interactionSource = interactions,
          indication = null,
          role = Role.Button,
          onClickLabel = label,
          onClick = {
            tapFeedback.trigger()
            onClick()
          },
      )
}

@Composable
@ReadOnlyComposable
internal fun movementRowAmountStyle(): TextStyle =
    TallybookTheme.typography.title.copy(fontSize = TallybookTypeScale.size14)

internal fun signedMovimientoAmountText(monto: Money, tipo: TipoMovimiento): String {
  val formatted = (if (tipo == TipoMovimiento.GASTO) -monto else monto).format()
  if (tipo != TipoMovimiento.INGRESO || monto.minorUnits == 0L) return formatted
  return "+$formatted"
}

internal fun pendingSubtitle(pendingLabel: String?, secondary: String?): String? =
    if (pendingLabel == null) secondary
    else listOfNotNull(pendingLabel, secondary).joinToString(PENDING_SUBTITLE_SEPARATOR)

private const val ROW_TITLE_MAX_LINES = 2
private const val ROW_DESCRIPTION_MAX_LINES = 1

private data class RowDescriptionLayout(
    val pending: Boolean,
    val extraCategories: Int,
)

@Composable
private fun MovimientoRowDescription(
    categoria: Categoria?,
    text: MovimientoRowText,
    layout: RowDescriptionLayout,
    modifier: Modifier,
    amount: @Composable () -> Unit,
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Box {
      if (categoria == null) CategoryAvatar(Lucide.CircleQuestionMark, neutralCategoryTintColors())
      else CategoryAvatar(categoria, pending = layout.pending)
      if (layout.extraCategories > 0) {
        ExtraCategoriesBadge(
            count = layout.extraCategories,
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = TallybookTheme.spacing.space6, y = TallybookTheme.spacing.space4),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      TallybookText(
          text.primary,
          style = TallybookTheme.typography.cardTitle,
          maxLines = ROW_TITLE_MAX_LINES,
          overflow = TextOverflow.Ellipsis,
      )
      text.secondary?.let {
        TallybookText(
            it,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.caption,
            maxLines = ROW_DESCRIPTION_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
      }
      amount()
    }
  }
}

@Composable
private fun extraCategoriesLabel(count: Int): String? =
    if (count > 0) pluralStringResource(Res.plurals.movimiento_row_extra_categories, count, count)
    else null

// The ring uses the card color so the badge reads as cut out of the avatar it overlaps.
@Composable
private fun ExtraCategoriesBadge(count: Int, modifier: Modifier = Modifier) {
  TallybookSurface(
      modifier = modifier.clearAndSetSemantics {},
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.highest,
              contentColor = TallybookTheme.colors.foreground.secondary,
              shape = RoundedCornerShape(percent = 50),
              border =
                  BorderStroke(TallybookTheme.spacing.space2, TallybookTheme.colors.surface.card),
          ),
  ) {
    TallybookText(
        text = "+$count",
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space4,
                vertical = TallybookTheme.spacing.space2,
            ),
        style = TallybookTheme.typography.caption,
    )
  }
}
