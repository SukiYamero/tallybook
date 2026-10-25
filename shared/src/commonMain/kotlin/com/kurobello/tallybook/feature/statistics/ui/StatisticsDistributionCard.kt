package com.kurobello.tallybook.feature.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.ui.chart.DonutSlice
import com.kurobello.tallybook.core.ui.chart.TallybookDistributionRow
import com.kurobello.tallybook.core.ui.chart.TallybookDonut
import com.kurobello.tallybook.core.ui.chart.shareFraction
import com.kurobello.tallybook.core.ui.chart.shareText
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsActions
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsDimension
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsDistribution
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsShareRow
import com.kurobello.tallybook.feature.statistics.viewmodel.StatisticsSlice
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.statistics_categories_hint
import tallybook.shared.generated.resources.statistics_count_categorias
import tallybook.shared.generated.resources.statistics_count_cuentas
import tallybook.shared.generated.resources.statistics_count_medios
import tallybook.shared.generated.resources.statistics_dimension_categorias
import tallybook.shared.generated.resources.statistics_dimension_cuentas
import tallybook.shared.generated.resources.statistics_dimension_etiquetas
import tallybook.shared.generated.resources.statistics_dimension_medios
import tallybook.shared.generated.resources.statistics_distribution_title
import tallybook.shared.generated.resources.statistics_donut_description
import tallybook.shared.generated.resources.statistics_no_gasto
import tallybook.shared.generated.resources.statistics_rows_hint
import tallybook.shared.generated.resources.statistics_share_item
import tallybook.shared.generated.resources.statistics_tags_note

private const val SHARE_SEPARATOR = ", "

private val DimensionLabels: Map<StatisticsDimension, StringResource> =
    mapOf(
        StatisticsDimension.Categorias to Res.string.statistics_dimension_categorias,
        StatisticsDimension.Etiquetas to Res.string.statistics_dimension_etiquetas,
        StatisticsDimension.Cuentas to Res.string.statistics_dimension_cuentas,
        StatisticsDimension.Medios to Res.string.statistics_dimension_medios,
    )

private val DonutCaptions: Map<StatisticsDimension, PluralStringResource> =
    mapOf(
        StatisticsDimension.Categorias to Res.plurals.statistics_count_categorias,
        StatisticsDimension.Cuentas to Res.plurals.statistics_count_cuentas,
        StatisticsDimension.Medios to Res.plurals.statistics_count_medios,
    )

private val RowHints: Map<StatisticsDimension, StringResource> =
    mapOf(
        StatisticsDimension.Categorias to Res.string.statistics_categories_hint,
        StatisticsDimension.Etiquetas to Res.string.statistics_rows_hint,
        StatisticsDimension.Cuentas to Res.string.statistics_rows_hint,
    )

/** Medio rows open nothing: Search filters by medio, but no request carries one yet. */
@Composable
internal fun StatisticsDistributionCard(
    distribution: StatisticsDistribution,
    dimension: StatisticsDimension,
    hasGasto: Boolean,
    actions: StatisticsActions,
    modifier: Modifier = Modifier,
) {
  StatisticsCard(stringResource(Res.string.statistics_distribution_title), modifier) {
    TallybookSegmentedControl(
        options =
            StatisticsDimension.entries.map {
              TallybookSegmentOption(it, stringResource(DimensionLabels.getValue(it)))
            },
        selected = dimension,
        onSelect = actions::onDimensionSelected,
        modifier = Modifier.fillMaxWidth(),
        style = TallybookSegmentedControlStyle.Neutral,
        labelStyle = TallybookTheme.typography.caption,
    )
    if (!hasGasto) {
      TallybookText(
          stringResource(Res.string.statistics_no_gasto),
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
      return@StatisticsCard
    }
    val donut = distribution.donut
    if (donut == null) {
      TallybookText(
          stringResource(Res.string.statistics_tags_note),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall,
      )
    } else {
      DistributionDonut(
          donut,
          distribution.total,
          DonutCaptions[dimension]?.let { caption ->
            val groups = distribution.rows.size
            pluralStringResource(caption, groups, groups)
          },
      )
    }
    Column {
      distribution.rows.forEach { row ->
        DistributionRow(
            row,
            if (dimension == StatisticsDimension.Medios) null
            else {
              { actions.onShareSelected(row.key) }
            },
        )
      }
    }
    RowHints[dimension]?.let { hint ->
      TallybookText(
          stringResource(hint),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
  }
}

@Composable
private fun DistributionDonut(slices: List<StatisticsSlice>, total: String, caption: String?) {
  val items = slices.map {
    stringResource(Res.string.statistics_share_item, it.label.text(), shareText(it.basisPoints))
  }
  val colors = slices.map { it.color.color() }
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    TallybookDonut(
        slices = slices.mapIndexed { index, slice -> DonutSlice(slice.basisPoints, colors[index]) },
        contentDescription =
            stringResource(
                Res.string.statistics_donut_description,
                items.joinToString(SHARE_SEPARATOR),
            ),
    ) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
      ) {
        TallybookText(total, style = TallybookTheme.typography.title)
        if (caption != null) {
          TallybookText(
              caption,
              color = TallybookTheme.colors.foreground.tertiary,
              style = TallybookTheme.typography.caption,
          )
        }
      }
    }
  }
}

@Composable
private fun DistributionRow(row: StatisticsShareRow, onClick: (() -> Unit)?) {
  TallybookDistributionRow(
      label = row.label.text(),
      amount = row.amount,
      share = shareText(row.basisPoints),
      fraction = shareFraction(row.basisPoints),
      color = row.color.color(),
      modifier = Modifier.fillMaxWidth(),
      change = row.change?.indicator(),
      onClick = onClick,
  )
}
