package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Wallet
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSkeletonLine
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.movimiento.BalanceCard
import com.kurobello.tallybook.core.ui.movimiento.BalanceCardSkeleton
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowSkeleton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.app_name
import tallybook.shared.generated.resources.home_add_first
import tallybook.shared.generated.resources.home_empty_body
import tallybook.shared.generated.resources.home_empty_title
import tallybook.shared.generated.resources.home_load_error_body
import tallybook.shared.generated.resources.home_load_error_title
import tallybook.shared.generated.resources.home_recent
import tallybook.shared.generated.resources.home_search_placeholder
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

@Composable
internal fun HomeHeader(modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().heightIn(min = TallybookTheme.dimensions.homeHeaderHeight),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        text = stringResource(Res.string.app_name),
        modifier = Modifier.semantics { heading() },
        style = TallybookTheme.typography.headline,
    )
  }
}

@Composable
internal fun HomeSearch(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          modifier
              .fillMaxWidth()
              .heightIn(min = TallybookTheme.dimensions.minimumTouchTarget)
              .then(
                  if (onClick != null)
                      Modifier.clickable(
                          interactionSource = interactionSource,
                          indication = null,
                          role = Role.Button,
                          onClick = {
                            tapFeedback.trigger()
                            onClick()
                          },
                      )
                  else Modifier
              ),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  contentColor = TallybookTheme.colors.foreground.tertiary,
                  shape = RoundedCornerShape(TallybookTheme.radii.radius18),
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.base,
                      ),
              ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier =
            Modifier.fillMaxSize().padding(horizontal = TallybookTheme.spacing.space14).semantics(
                mergeDescendants = true
            ) {},
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = Lucide.Search,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.homeSearchIcon),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
      )
      TallybookText(
          text = stringResource(Res.string.home_search_placeholder),
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

private const val HOME_SKELETON_ROW_COUNT = 3

internal fun LazyListScope.homeLoading(showLoading: Boolean) {
  item {
    val description = stringResource(Res.string.loading)
    val feedback =
        if (showLoading)
            Modifier.semantics {
              contentDescription = description
              progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
              liveRegion = LiveRegionMode.Polite
            }
        else Modifier
    Box(
        modifier =
            Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space18).then(feedback)
    ) {
      BalanceCardSkeleton(Modifier.fillMaxWidth().alpha(if (showLoading) 1f else 0f))
    }
  }
  if (showLoading) {
    item {
      TallybookSkeletonLine(
          stringResource(Res.string.home_recent),
          TallybookTheme.typography.title,
          Modifier.padding(bottom = TallybookTheme.spacing.space10),
      )
    }
    items(HOME_SKELETON_ROW_COUNT) {
      MovimientoRowSkeleton(
          Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space10)
      )
    }
  }
}

@Composable
internal fun HomeError(onRetry: () -> Unit) {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style = homeStateSurfaceStyle(),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = TallybookTheme.spacing.space24,
                    vertical = TallybookTheme.spacing.space48,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      TallybookText(
          text = stringResource(Res.string.home_load_error_title),
          modifier =
              Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
              },
          style = TallybookTheme.typography.title.copy(textAlign = TextAlign.Center),
      )
      TallybookText(
          text = stringResource(Res.string.home_load_error_body),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space6),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
      )
      TallybookButton(
          onClick = onRetry,
          modifier = Modifier.padding(top = TallybookTheme.spacing.space18),
      ) {
        TallybookText(stringResource(Res.string.retry))
      }
    }
  }
}

@Composable
internal fun HomeEmpty(onAddMovimiento: () -> Unit) {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style = homeStateSurfaceStyle(),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = TallybookTheme.spacing.space24,
                    vertical = TallybookTheme.spacing.space48,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
          modifier =
              Modifier.size(TallybookTheme.dimensions.homeEmptyIconContainer)
                  .background(TallybookTheme.colors.surface.raised, CircleShape),
          contentAlignment = Alignment.Center,
      ) {
        Image(
            imageVector = Lucide.Wallet,
            contentDescription = null,
            modifier = Modifier.size(TallybookTheme.dimensions.homeEmptyIcon),
            colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
        )
      }
      TallybookText(
          text = stringResource(Res.string.home_empty_title),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space14).semantics { heading() },
          style = TallybookTheme.typography.title.copy(textAlign = TextAlign.Center),
      )
      TallybookText(
          text = stringResource(Res.string.home_empty_body),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space6),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
      )
      TallybookButton(
          onClick = onAddMovimiento,
          modifier = Modifier.padding(top = TallybookTheme.spacing.space18),
      ) {
        TallybookText(stringResource(Res.string.home_add_first))
      }
    }
  }
}

internal fun LazyListScope.homePopulated(
    uiState: HomeUiState,
    onOpenMovimiento: ((String) -> Unit)? = null,
) {
  item {
    BalanceCard(
        summary = uiState.summary,
        modifier = Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space18),
    )
  }
  item {
    TallybookText(
        text = stringResource(Res.string.home_recent),
        modifier =
            Modifier.padding(bottom = TallybookTheme.spacing.space10).semantics { heading() },
        style = TallybookTheme.typography.title,
    )
  }
  items(items = uiState.recent, key = { it.id }) { movimiento ->
    MovimientoRow(
        movimiento = movimiento,
        categoria = uiState.categoriasById[movimiento.categoriaIds.first()],
        onClick = onOpenMovimiento?.let { open -> { open(movimiento.id) } },
        modifier = Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space10),
        pending = CATEGORIA_OTROS_ID in movimiento.categoriaIds,
    )
  }
}

@Composable
private fun homeStateSurfaceStyle(): TallybookSurfaceStyle =
    TallybookSurfaceDefaults.cardStyle()
        .copy(
            shape = RoundedCornerShape(TallybookTheme.radii.radius26),
            border =
                BorderStroke(
                    TallybookTheme.dimensions.borderThin,
                    TallybookTheme.colors.border.subtle,
                ),
        )
