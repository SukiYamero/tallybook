package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.account.MedioPagoIcon
import com.kurobello.tallybook.core.ui.account.labelResource
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
import tallybook.shared.generated.resources.movimiento_open_detail
import tallybook.shared.generated.resources.movimiento_pending_label

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
    onClickLabel: String? = null,
) {
  val amountText = signedMovimientoAmountText(movimiento.monto, movimiento.tipo)
  val labels = movimientoRowLabels(movimiento, pending)
  val description = movimientoRowDescription(text, amountText, movimiento.monto.moneda, labels)
  val interactions = remember { MutableInteractionSource() }
  val isPressed by interactions.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val rowModifier =
      movimientoRowClickModifier(modifier, onClick, onClickLabel, interactions, tapFeedback)
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
        text.copy(secondary = pendingSubtitle(labels.pending, text.secondary)),
        pending,
        Modifier.padding(
            horizontal = TallybookTheme.spacing.space14,
            vertical = TallybookTheme.spacing.space12,
        ),
    ) {
      MovimientoRowAmountLine(icon = { MovimientoRowMedioIcon(movimiento.medio) }) {
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
}

/** Shared with the skeleton so loading reserves the medio slot and the amount does not shift. */
@Composable
internal fun MovimientoRowAmountLine(icon: @Composable () -> Unit, amount: @Composable () -> Unit) {
  Row(
      modifier = Modifier.padding(top = TallybookTheme.spacing.space4),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    icon()
    amount()
  }
}

/** The row's merged description already announces the medio, so the icon stays silent. */
@Composable
private fun MovimientoRowMedioIcon(medio: MedioPago) {
  MedioPagoIcon(
      medio = medio,
      modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
      contentDescription = null,
      tint = TallybookTheme.colors.foreground.secondary,
  )
}

@Composable
private fun movimientoRowLabels(movimiento: Movimiento, pending: Boolean) =
    MovimientoRowLabels(
        tipo = stringResource(movimiento.tipo.presentation.label),
        medio = stringResource(movimiento.medio.labelResource),
        pending = if (pending) stringResource(Res.string.movimiento_pending_label) else null,
    )

internal fun movimientoRowDescription(
    text: MovimientoRowText,
    amountText: String,
    moneda: Moneda,
    labels: MovimientoRowLabels,
): String =
    listOfNotNull(
            text.primary,
            text.secondary,
            text.categoryLabel.takeUnless { it == text.primary },
            labels.pending,
            labels.tipo,
            amountText,
            moneda.name,
            labels.medio,
        )
        .joinToString(", ")

@Composable
private fun movimientoRowClickModifier(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    interactions: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier {
  val label = onClickLabel ?: stringResource(Res.string.movimiento_open_detail)
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

@Composable
private fun MovimientoRowDescription(
    categoria: Categoria?,
    text: MovimientoRowText,
    pending: Boolean,
    modifier: Modifier,
    amount: @Composable () -> Unit,
) {
  Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (categoria == null) CategoryAvatar(Lucide.CircleQuestionMark, neutralCategoryTintColors())
    else CategoryAvatar(categoria, pending = pending)
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
