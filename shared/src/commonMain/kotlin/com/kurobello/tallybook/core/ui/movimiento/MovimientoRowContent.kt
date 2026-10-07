package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.core.ui.theme.TallybookTypeScale
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.movimiento_open_detail

internal data class MovimientoRowText(
    val primary: String,
    val secondary: String?,
    val categoryLabel: String,
)

@Composable
@Suppress("LongParameterList")
internal fun MovimientoRowContent(
    movimiento: Movimiento,
    categoria: Categoria?,
    text: MovimientoRowText,
    modifier: Modifier = Modifier,
    maxDescriptionLines: Int = 2,
    maxSubtitleLines: Int = 1,
    onClick: (() -> Unit)? = null,
) {
  val amountText = signedMovimientoAmountText(movimiento.monto, movimiento.tipo)
  val description = movimientoRowDescription(movimiento, text, amountText)
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
    FlowRow(
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space14,
                vertical = TallybookTheme.spacing.space12,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      MovimientoRowDescription(
          categoria,
          text,
          maxDescriptionLines,
          maxSubtitleLines,
          Modifier.weight(1f),
      )
      TallybookText(
          amountText,
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
): String {
  val typeLabel =
      stringResource(
          if (movimiento.tipo == TipoMovimiento.INGRESO) Res.string.income else Res.string.expense
      )
  return listOfNotNull(
          text.primary,
          text.secondary,
          text.categoryLabel.takeUnless { it == text.primary },
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
  val prefix = monto.moneda.symbol + monto.moneda.symbolSeparator
  return prefix + "+" + formatted.removePrefix(prefix)
}

@Composable
private fun MovimientoRowDescription(
    categoria: Categoria?,
    text: MovimientoRowText,
    maxDescriptionLines: Int,
    maxSubtitleLines: Int,
    modifier: Modifier,
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (categoria == null) CategoryAvatar(Lucide.CircleQuestionMark, neutralCategoryTintColors())
    else CategoryAvatar(categoria)
    Column(modifier = Modifier.weight(1f)) {
      TallybookText(
          text.primary,
          style = TallybookTheme.typography.bodyEmphasis,
          maxLines = maxDescriptionLines,
          overflow = TextOverflow.Ellipsis,
      )
      text.secondary?.let {
        TallybookText(
            it,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.bodySmall,
            maxLines = maxSubtitleLines,
            overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
