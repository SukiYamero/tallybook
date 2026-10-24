package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.neutralCategoryTintColors
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.movimiento.movementRowAmountStyle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.transfer_row_description
import tallybook.shared.generated.resources.transfer_row_route

/**
 * A transfer between the user's own accounts as a neutral row: neither income nor expense, so the
 * amount takes no sign or status color. Callers choose which side's [amount] to show (History shows
 * the selected currency's side) and what [supporting] line, if any, sits under the route.
 */
@Composable
@Suppress("LongParameterList")
internal fun TransferenciaRowContent(
    origen: String,
    destino: String,
    amount: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
  val description =
      listOfNotNull(
              stringResource(Res.string.transfer_row_description, origen, destino),
              supporting,
              amount,
          )
          .joinToString(", ")
  val interactions = remember { MutableInteractionSource() }
  val isPressed by interactions.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          modifier.then(rowClick(onClick, onClickLabel, interactions, tapFeedback)).semantics(
              mergeDescendants = true
          ) {
            contentDescription = description
          },
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
    TransferenciaRowBody(
        stringResource(Res.string.transfer_row_route, origen, destino),
        amount,
        supporting,
    )
  }
}

@Composable
private fun TransferenciaRowBody(route: String, amount: String, supporting: String?) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal = TallybookTheme.spacing.space14,
                  vertical = TallybookTheme.spacing.space12,
              ),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    CategoryAvatar(Lucide.ArrowLeftRight, neutralCategoryTintColors())
    Column(modifier = Modifier.weight(1f)) {
      TallybookText(
          route,
          style = TallybookTheme.typography.cardTitle,
          maxLines = ROUTE_MAX_LINES,
          overflow = TextOverflow.Ellipsis,
      )
      supporting?.let {
        TallybookText(
            it,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.caption,
            maxLines = SUPPORTING_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
      }
      TallybookText(
          amount,
          modifier = Modifier.padding(top = TallybookTheme.spacing.space4),
          color = TallybookTheme.colors.foreground.primary,
          style = movementRowAmountStyle(),
      )
    }
  }
}

private fun rowClick(
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    interactions: MutableInteractionSource,
    tapFeedback: TallybookTapFeedback,
): Modifier =
    if (onClick == null) Modifier
    else
        Modifier.clickable(
            interactionSource = interactions,
            indication = null,
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = {
              tapFeedback.trigger()
              onClick()
            },
        )

private const val ROUTE_MAX_LINES = 2
private const val SUPPORTING_MAX_LINES = 1

@Preview
@Composable
internal fun TransferenciaRowContentLightPreview() {
  TallybookTheme(darkTheme = false) {
    TransferenciaRowContent(
        origen = "Nequi",
        destino = "NU",
        amount = "$ 350.000,00",
        supporting = "16 de octubre de 2026",
        modifier = Modifier.widthIn(max = 420.dp),
    )
  }
}

@Preview
@Composable
internal fun TransferenciaRowContentDarkPreview() {
  TallybookTheme(darkTheme = true) {
    TransferenciaRowContent(
        origen = "Dólares (USD)",
        destino = "Nequi",
        amount = "$100.00 → $ 400.000,00",
        modifier = Modifier.widthIn(max = 420.dp),
    )
  }
}
