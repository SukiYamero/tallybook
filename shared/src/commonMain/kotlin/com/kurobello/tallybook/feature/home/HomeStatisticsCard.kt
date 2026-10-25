package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Change
import com.kurobello.tallybook.core.model.Direction
import com.kurobello.tallybook.core.ui.chart.chartRankColor
import com.kurobello.tallybook.core.ui.chart.chartRestColor
import com.kurobello.tallybook.core.ui.chart.shareText
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.home_statistics_more
import tallybook.shared.generated.resources.home_statistics_open
import tallybook.shared.generated.resources.home_statistics_title
import tallybook.shared.generated.resources.statistics_pace_above
import tallybook.shared.generated.resources.statistics_pace_below
import tallybook.shared.generated.resources.statistics_pace_even
import tallybook.shared.generated.resources.statistics_share_item
import tallybook.shared.generated.resources.unknown_category

/** Between the balance and the recent movimientos, only while this month has gasto. */
internal fun LazyListScope.homeStatistics(uiState: HomeUiState, onOpen: (() -> Unit)?) {
  val statistics = uiState.statistics ?: return
  item(key = "statistics") {
    HomeStatisticsCard(
        statistics = statistics,
        categoriasById = uiState.categoriasById,
        onOpen = onOpen,
        modifier = Modifier.padding(bottom = TallybookTheme.spacing.space18),
    )
  }
}

/**
 * The whole card opens Statistics; its bar only repeats the legend, so it stays out of semantics.
 */
@Composable
internal fun HomeStatisticsCard(
    statistics: HomeStatistics,
    categoriasById: Map<String, Categoria>,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val openLabel = stringResource(Res.string.home_statistics_open)
  TallybookSurface(
      modifier =
          modifier
              .fillMaxWidth()
              .then(
                  if (onOpen != null)
                      Modifier.clickable(
                          interactionSource = interactionSource,
                          indication = null,
                          onClickLabel = openLabel,
                          role = Role.Button,
                          onClick = {
                            tapFeedback.trigger()
                            onOpen()
                          },
                      )
                  else Modifier
              ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      HomeStatisticsHeader()
      val shares =
          statistics.top.mapIndexed { rank, share ->
            HomeShareUi(
                label =
                    categoriasById[share.categoriaId]?.nombre
                        ?: stringResource(Res.string.unknown_category),
                basisPoints = share.basisPoints,
                color = chartRankColor(rank),
            )
          }
      HomeShareBar(shares, statistics.otherBasisPoints)
      HomeShareLegend(shares)
      statistics.pace?.let { HomePaceLine(it) }
    }
  }
}

private class HomeShareUi(val label: String, val basisPoints: Int, val color: Color)

@Composable
private fun HomeStatisticsHeader() {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookText(
        text = stringResource(Res.string.home_statistics_title),
        modifier = Modifier.weight(1f).semantics { heading() },
        style = TallybookTheme.typography.bodyEmphasis,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          text = stringResource(Res.string.home_statistics_more),
          color = TallybookTheme.colors.brand.primary,
          style = TallybookTheme.typography.label,
      )
      Image(
          imageVector = Lucide.ChevronRight,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
    }
  }
}

@Composable
private fun HomeShareBar(shares: List<HomeShareUi>, otherBasisPoints: Int) {
  val restColor = chartRestColor()
  val segments =
      shares.map { it.basisPoints to it.color } +
          listOfNotNull((otherBasisPoints to restColor).takeIf { otherBasisPoints > 0 })
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(TallybookTheme.dimensions.chartStackedBarHeight)
              .clip(RoundedCornerShape(TallybookTheme.dimensions.chartStackedBarHeight / 2))
              .clearAndSetSemantics {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.chartStackedBarGap),
  ) {
    segments
        .filter { (basisPoints, _) -> basisPoints > 0 }
        .forEach { (basisPoints, color) ->
          Spacer(
              Modifier.weight(basisPoints.toFloat())
                  .height(TallybookTheme.dimensions.chartStackedBarHeight)
                  .background(color)
          )
        }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeShareLegend(shares: List<HomeShareUi>) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space14),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    shares.forEach { share ->
      Row(
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
            Modifier.size(TallybookTheme.dimensions.chartLegendDot)
                .background(share.color, CircleShape)
        )
        TallybookText(
            text =
                stringResource(
                    Res.string.statistics_share_item,
                    share.label,
                    shareText(share.basisPoints),
                ),
            color = TallybookTheme.colors.foreground.secondary,
            style = TallybookTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun HomePaceLine(change: Change) {
  val presentation = paceTone(change.direction)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10)) {
    Spacer(
        Modifier.fillMaxWidth()
            .height(TallybookTheme.dimensions.borderThin)
            .background(TallybookTheme.colors.border.subtle)
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = presentation.icon,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
          colorFilter = ColorFilter.tint(presentation.color),
      )
      TallybookText(
          text =
              when (change.direction) {
                Direction.Up -> stringResource(Res.string.statistics_pace_above, change.percent)
                Direction.Down -> stringResource(Res.string.statistics_pace_below, change.percent)
                Direction.Flat -> stringResource(Res.string.statistics_pace_even)
              },
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

private class PaceTone(val icon: ImageVector, val color: Color)

/** Spending above the usual pace is unfavorable, like a gasto that grows. */
@Composable
private fun paceTone(direction: Direction): PaceTone =
    when (direction) {
      Direction.Up -> PaceTone(Lucide.TrendingUp, TallybookTheme.colors.status.danger.base)
      Direction.Down -> PaceTone(Lucide.TrendingDown, TallybookTheme.colors.status.success.base)
      Direction.Flat -> PaceTone(Lucide.Minus, TallybookTheme.colors.foreground.tertiary)
    }

@Preview
@Composable
@Suppress("MagicNumber")
internal fun HomeStatisticsCardPreview() {
  val categorias =
      listOf(
              "Comida" to CategoryIcon.UTENSILS,
              "Hogar" to CategoryIcon.HOUSE,
              "Transporte" to CategoryIcon.CAR,
          )
          .mapIndexed { index, (nombre, icono) ->
            Categoria("c$index", nombre, null, icono, CategoryTint.AMBER)
          }
  TallybookTheme {
    HomeStatisticsCard(
        statistics =
            HomeStatistics(
                top =
                    listOf(
                        HomeCategoryShare("c0", 2_950),
                        HomeCategoryShare("c1", 2_210),
                        HomeCategoryShare("c2", 1_330),
                    ),
                otherBasisPoints = 3_510,
                pace = Change(15, Direction.Up),
            ),
        categoriasById = categorias.associateBy { it.id },
        onOpen = {},
    )
  }
}
