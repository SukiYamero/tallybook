package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.StatisticsScope
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.chart.TallybookCategoryTrendChart
import com.kurobello.tallybook.core.ui.chart.TallybookDistributionRow
import com.kurobello.tallybook.core.ui.chart.shareFraction
import com.kurobello.tallybook.core.ui.chart.shareText
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.CategoryDetailContent
import com.kurobello.tallybook.feature.statistics.viewmodel.CategoryDetailState
import com.kurobello.tallybook.feature.statistics.viewmodel.CategoryLargest
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsContent
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsLabel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.history_movement_count
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.statistics_detail_average
import tallybook.shared.generated.resources.statistics_detail_close
import tallybook.shared.generated.resources.statistics_detail_largest
import tallybook.shared.generated.resources.statistics_detail_largest_line
import tallybook.shared.generated.resources.statistics_detail_month_item
import tallybook.shared.generated.resources.statistics_detail_months_description
import tallybook.shared.generated.resources.statistics_detail_months_month
import tallybook.shared.generated.resources.statistics_detail_months_year
import tallybook.shared.generated.resources.statistics_detail_share_month
import tallybook.shared.generated.resources.statistics_detail_share_year
import tallybook.shared.generated.resources.statistics_detail_subcategories
import tallybook.shared.generated.resources.statistics_detail_subtitle
import tallybook.shared.generated.resources.statistics_detail_view
import tallybook.shared.generated.resources.statistics_vs_previous

private const val ITEM_SEPARATOR = ", "
private const val FACT_SEPARATOR = " · "

/**
 * The detail of a root categoria. [main] names the categoria until its own read arrives; while
 * closing, the sheet keeps rendering the last content it showed.
 */
@Composable
internal fun CategoryStatisticsSheet(
    detail: CategoryDetailState?,
    main: StatisticsContent?,
    actions: StatisticsActions,
) {
  if (detail == null) return
  val content = detail.content.cachedValue
  val label =
      content?.label
          ?: main
              ?.distributions
              ?.categorias
              ?.rows
              ?.firstOrNull { it.key == StatisticsKey.Categoria(detail.rootId) }
              ?.label
          ?: StatisticsLabel.UnknownCategoria
  val title = label.text()
  TallybookModalBottomSheet(
      visible = !detail.isClosing,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = actions::onDetailDismissed,
              onDismissRequest = {
                actions.onDetailDismissed()
                false
              },
              onHidden = actions::onDetailHidden,
          ),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
                .padding(bottom = TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
    ) {
      SheetHeader(title, content, actions::onDetailDismissed)
      when {
        content != null -> DetailBody(content, detail.isClosing, actions)
        detail.content is RetainedLoadState.Failed ->
            StatisticsError(detail.content.failure, actions::onRetry)
        else ->
            TallybookText(
                stringResource(Res.string.loading),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = TallybookTheme.colors.foreground.secondary,
            )
      }
    }
  }
}

@Composable
private fun SheetHeader(title: String, content: CategoryDetailContent?, onClose: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
    ) {
      TallybookSheetTitle(title)
      if (content != null) {
        val period = statisticsPeriodTitle(content.scope, content.period)
        TallybookText(
            if (content.partial)
                stringResource(
                    Res.string.statistics_detail_subtitle,
                    period,
                    statisticsUntil(content.period.end),
                )
            else period,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.bodySmall,
        )
      }
    }
    TallybookIconButton(
        onClick = onClose,
        icon = Lucide.X,
        contentDescription = stringResource(Res.string.statistics_detail_close),
        style = TallybookIconButtonStyle.Ghost,
    )
  }
}

@Composable
private fun DetailBody(
    content: CategoryDetailContent,
    closing: Boolean,
    actions: StatisticsActions,
) {
  DetailTotal(content)
  DetailMonths(content)
  if (content.subcategorias.size > 1) DetailSubcategorias(content)
  if (content.largest.isNotEmpty()) DetailLargest(content.largest)
  val count = content.summary.count
  if (count > 0) {
    TallybookButton(
        onClick = actions::onViewMovimientos,
        modifier = Modifier.fillMaxWidth(),
        enabled = !closing,
    ) {
      TallybookText(
          pluralStringResource(
              Res.plurals.statistics_detail_view,
              count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
              count,
          )
      )
    }
  }
}

@Composable
private fun DetailTotal(content: CategoryDetailContent) {
  val summary = content.summary
  val count = summary.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
  val facts =
      listOfNotNull(
              summary.shareBasisPoints?.let { basisPoints ->
                stringResource(
                    when (content.scope) {
                      StatisticsScope.Month -> Res.string.statistics_detail_share_month
                      StatisticsScope.Year -> Res.string.statistics_detail_share_year
                    },
                    shareText(basisPoints),
                )
              },
              pluralStringResource(Res.plurals.history_movement_count, count, count),
          )
          .joinToString(FACT_SEPARATOR)
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style =
          TallybookSurfaceDefaults.cardStyle().copy(color = TallybookTheme.colors.surface.sunken),
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        content.categoria?.let { CategoryAvatar(it, pending = it.id == CATEGORIA_OTROS_ID) }
        Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2)) {
          TallybookText(summary.total, style = TallybookTheme.typography.headline)
          TallybookText(
              facts,
              color = TallybookTheme.colors.foreground.secondary,
              style = TallybookTheme.typography.bodySmall,
          )
        }
      }
      DetailChange(content)
    }
  }
}

@Composable
private fun DetailChange(content: CategoryDetailContent) {
  val summary = content.summary
  val change = summary.change
  val previous = summary.previous
  val comparison = summary.comparison
  if (change == null || previous == null || comparison == null) return
  val indicator = change.indicator()
  FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookText(
        indicator.label,
        color = indicator.tone.color(),
        style = TallybookTheme.typography.bodyEmphasis,
    )
    TallybookText(
        stringResource(Res.string.statistics_vs_previous, previous, comparison.phrase()),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun DetailMonths(content: CategoryDetailContent) {
  val labels = content.months.map { shortMonthName(it.month.month) }
  val items =
      content.months.mapIndexed { index, month ->
        stringResource(Res.string.statistics_detail_month_item, labels[index], month.text)
      }
  val average =
      content.average?.let { stringResource(Res.string.statistics_detail_average, it.text) }
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(
        when (content.scope) {
          StatisticsScope.Month -> stringResource(Res.string.statistics_detail_months_month)
          StatisticsScope.Year ->
              stringResource(Res.string.statistics_detail_months_year, content.period.start.year)
        }
    )
    TallybookCategoryTrendChart(
        values = content.months.map { it.value },
        labels = labels,
        average = content.average?.value ?: 0.0,
        partialLast = content.months.lastOrNull()?.partial == true,
        color = content.color.color(),
        contentDescription =
            stringResource(
                Res.string.statistics_detail_months_description,
                content.label.text(),
                items.joinToString(ITEM_SEPARATOR),
                average.orEmpty(),
            ),
        modifier = Modifier.fillMaxWidth(),
    )
    if (average != null) {
      TallybookText(
          average,
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}

@Composable
private fun DetailSubcategorias(content: CategoryDetailContent) {
  val color = content.color.color()
  Column {
    TallybookSectionHeading(stringResource(Res.string.statistics_detail_subcategories))
    content.subcategorias.forEach { row ->
      TallybookDistributionRow(
          label = row.label.text(),
          amount = row.amount,
          share = shareText(row.basisPoints),
          fraction = shareFraction(row.basisPoints),
          color = color,
          modifier = Modifier.fillMaxWidth(),
          change = row.change?.indicator(),
      )
    }
  }
}

@Composable
private fun DetailLargest(largest: List<CategoryLargest>) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.statistics_detail_largest))
    largest.forEach { item ->
      val movimiento = item.movimiento
      val categoryLabel = item.categoria?.nombre ?: StatisticsLabel.UnknownCategoria.text()
      MovimientoRowContent(
          movimiento = movimiento,
          categoria = item.categoria,
          text =
              MovimientoRowText(
                  primary = movimiento.rowTitle(categoryLabel),
                  secondary =
                      stringResource(
                          Res.string.statistics_detail_largest_line,
                          movimiento.fecha.day,
                          shortMonthName(movimiento.fecha.month),
                          categoryLabel,
                      ),
                  categoryLabel = categoryLabel,
              ),
          modifier = Modifier.fillMaxWidth(),
          pending = movimiento.categoriaId == CATEGORIA_OTROS_ID,
      )
    }
  }
}
