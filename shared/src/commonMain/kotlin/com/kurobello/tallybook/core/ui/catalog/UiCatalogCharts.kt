@file:Suppress("MagicNumber", "TooManyFunctions") // Fixed sample data for the catalog.

package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.ui.chart.ChangeIndicator
import com.kurobello.tallybook.core.ui.chart.ChangeTone
import com.kurobello.tallybook.core.ui.chart.DonutSlice
import com.kurobello.tallybook.core.ui.chart.TallybookCategoryTrendChart
import com.kurobello.tallybook.core.ui.chart.TallybookDistributionRow
import com.kurobello.tallybook.core.ui.chart.TallybookDonut
import com.kurobello.tallybook.core.ui.chart.TallybookPaceChart
import com.kurobello.tallybook.core.ui.chart.TallybookTrendChart
import com.kurobello.tallybook.core.ui.chart.TrendPoint
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.math.pow
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_ocio
import tallybook.shared.generated.resources.category_transporte
import tallybook.shared.generated.resources.date_picker_months_short
import tallybook.shared.generated.resources.ui_catalog_chart_all_zero
import tallybook.shared.generated.resources.ui_catalog_chart_category_trend
import tallybook.shared.generated.resources.ui_catalog_chart_change_down
import tallybook.shared.generated.resources.ui_catalog_chart_change_flat
import tallybook.shared.generated.resources.ui_catalog_chart_change_up
import tallybook.shared.generated.resources.ui_catalog_chart_distribution
import tallybook.shared.generated.resources.ui_catalog_chart_donut
import tallybook.shared.generated.resources.ui_catalog_chart_donut_caption
import tallybook.shared.generated.resources.ui_catalog_chart_long_label
import tallybook.shared.generated.resources.ui_catalog_chart_long_text
import tallybook.shared.generated.resources.ui_catalog_chart_nine_slices
import tallybook.shared.generated.resources.ui_catalog_chart_pace
import tallybook.shared.generated.resources.ui_catalog_chart_sample_summary
import tallybook.shared.generated.resources.ui_catalog_chart_share
import tallybook.shared.generated.resources.ui_catalog_chart_single_value
import tallybook.shared.generated.resources.ui_catalog_chart_today
import tallybook.shared.generated.resources.ui_catalog_chart_trend
import tallybook.shared.generated.resources.ui_catalog_charts
import tallybook.shared.generated.resources.ui_catalog_state_empty
import tallybook.shared.generated.resources.ui_catalog_state_partial

private val SampleMoneda = Moneda.COP
private val NineSliceBasisPoints = listOf(2200, 1700, 1400, 1100, 1000, 900, 700, 600, 400)
private val SampleGastos = listOf(705_000.0, 742_000.0, 720_000.0, 690_000.0, 660_000.0, 612_400.0)
private val SampleIngresos =
    listOf(1_180_000.0, 1_180_000.0, 1_250_000.0, 1_250_000.0, 1_250_000.0, 1_250_000.0)
private val SampleDailyGastos =
    listOf(9, 35, 16, 7, 22, 46, 13, 18, 29, 70, 20, 11, 38, 8, 25, 15, 32, 14, 22, 53, 19, 28, 42)
        .map { it * 1_000.0 } + 20_400.0
private val SampleCategoryMonths =
    listOf(162_000.0, 175_500.0, 158_000.0, 181_200.0, 170_300.0, 180_700.0)
private const val SAMPLE_FIRST_MONTH = 4
private const val SAMPLE_DAYS_IN_MONTH = 31
private const val SAMPLE_AVERAGE_MONTH = 690_000.0
private const val SAMPLE_PROJECTION = 791_000.0
private const val SAMPLE_CATEGORY_AVERAGE = 171_000.0

internal fun LazyListScope.catalogCharts() {
  catalogControlSection("charts", { stringResource(Res.string.ui_catalog_charts) }) {
    CatalogCharts()
  }
}

@Composable
private fun CatalogCharts() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    CatalogDonuts()
    CatalogDistributionRows()
    CatalogTrendCharts()
    CatalogPaceCharts()
    CatalogCategoryTrendCharts()
  }
}

@Composable
private fun sampleSummary(variant: String): String =
    stringResource(Res.string.ui_catalog_chart_sample_summary, variant)

private fun money(value: Double): String =
    Money((value * 10.0.pow(SampleMoneda.scale)).roundToLong(), SampleMoneda).format()

@Composable
private fun sampleMonths(count: Int): List<String> {
  val months = stringArrayResource(Res.array.date_picker_months_short)
  return List(count) { index -> months[(SAMPLE_FIRST_MONTH + index) % months.size] }
}

@Composable
private fun CatalogDonuts() {
  val palette = TallybookTheme.colors.chart
  val nine = stringResource(Res.string.ui_catalog_chart_nine_slices)
  val single = stringResource(Res.string.ui_catalog_chart_single_value)
  val zero = stringResource(Res.string.ui_catalog_chart_all_zero)
  val empty = stringResource(Res.string.ui_catalog_state_empty)
  val caption = stringResource(Res.string.ui_catalog_chart_donut_caption)
  CatalogSurface {
    TallybookText(
        text = stringResource(Res.string.ui_catalog_chart_donut),
        style = TallybookTheme.typography.bodyEmphasis,
    )
    CatalogSample(label = nine) {
      CenteredDonut(
          slices = NineSliceBasisPoints.mapIndexed { i, bp -> DonutSlice(bp, palette[i]) },
          contentDescription = sampleSummary(nine),
      ) {
        DonutCenter(amount = money(612_400.0), caption = caption)
      }
    }
    CatalogSample(label = single) {
      CenteredDonut(
          slices = listOf(DonutSlice(10_000, palette.first())),
          contentDescription = sampleSummary(single),
      )
    }
    CatalogSample(label = zero) {
      CenteredDonut(
          slices = palette.take(3).map { DonutSlice(0, it) },
          contentDescription = sampleSummary(zero),
      )
    }
    CatalogSample(label = empty) {
      CenteredDonut(
          slices = emptyList(),
          contentDescription = sampleSummary(empty),
      )
    }
  }
}

@Composable
private fun CenteredDonut(
    slices: List<DonutSlice>,
    contentDescription: String,
    center: @Composable () -> Unit = {},
) {
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    TallybookDonut(slices = slices, contentDescription = contentDescription, center = center)
  }
}

@Composable
private fun DonutCenter(amount: String, caption: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    TallybookText(text = amount, style = TallybookTheme.typography.title)
    TallybookText(
        text = caption,
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
  }
}

@Composable
private fun CatalogDistributionRows() {
  val palette = TallybookTheme.colors.chart
  CatalogSurface {
    TallybookText(
        text = stringResource(Res.string.ui_catalog_chart_distribution),
        style = TallybookTheme.typography.bodyEmphasis,
    )
    CatalogSampleDistributionRows()
    CatalogSample(label = stringResource(Res.string.ui_catalog_chart_long_text)) {
      TallybookDistributionRow(
          label = stringResource(Res.string.ui_catalog_chart_long_label),
          amount = money(1_234_567_890.0),
          share = stringResource(Res.string.ui_catalog_chart_share, 100),
          fraction = 1f,
          color = palette[4],
          change =
              ChangeIndicator(
                  stringResource(Res.string.ui_catalog_chart_change_up, 1_250),
                  ChangeTone.Unfavorable,
              ),
          onClick = {},
      )
    }
  }
}

@Composable
private fun CatalogSampleDistributionRows() {
  val palette = TallybookTheme.colors.chart
  Column {
    TallybookDistributionRow(
        label = stringResource(Res.string.category_comida),
        amount = money(180_700.0),
        share = stringResource(Res.string.ui_catalog_chart_share, 30),
        fraction = 0.295f,
        color = palette[2],
        change =
            ChangeIndicator(
                stringResource(Res.string.ui_catalog_chart_change_up, 18),
                ChangeTone.Unfavorable,
            ),
        onClick = {},
    )
    TallybookDistributionRow(
        label = stringResource(Res.string.category_hogar),
        amount = money(135_300.0),
        share = stringResource(Res.string.ui_catalog_chart_share, 22),
        fraction = 0.221f,
        color = palette[1],
        change =
            ChangeIndicator(
                stringResource(Res.string.ui_catalog_chart_change_flat),
                ChangeTone.Neutral,
            ),
        onClick = {},
    )
    TallybookDistributionRow(
        label = stringResource(Res.string.category_transporte),
        amount = money(81_500.0),
        share = stringResource(Res.string.ui_catalog_chart_share, 13),
        fraction = 0.133f,
        color = palette[5],
        change =
            ChangeIndicator(
                stringResource(Res.string.ui_catalog_chart_change_down, 9),
                ChangeTone.Favorable,
            ),
    )
    TallybookDistributionRow(
        label = stringResource(Res.string.category_ocio),
        amount = money(612_400.0),
        share = stringResource(Res.string.ui_catalog_chart_share, 100),
        fraction = 1f,
        color = palette[8],
    )
    TallybookDistributionRow(
        label = stringResource(Res.string.ui_catalog_chart_all_zero),
        amount = money(0.0),
        share = stringResource(Res.string.ui_catalog_chart_share, 0),
        fraction = 0f,
        color = TallybookTheme.colors.foreground.faint,
    )
  }
}

@Composable
private fun CatalogTrendCharts() {
  val months = sampleMonths(SampleGastos.size)
  val points =
      remember(months) {
        months.mapIndexed { index, label ->
          TrendPoint(
              label = label,
              gastos = SampleGastos[index],
              ingresos = SampleIngresos[index],
              partial = index == months.lastIndex,
          )
        }
      }
  var selectedIndex by rememberSaveable { mutableIntStateOf(points.lastIndex) }
  val partial = stringResource(Res.string.ui_catalog_state_partial)
  val single = stringResource(Res.string.ui_catalog_chart_single_value)
  CatalogSurface {
    TallybookText(
        text = stringResource(Res.string.ui_catalog_chart_trend),
        style = TallybookTheme.typography.bodyEmphasis,
    )
    CatalogSample(label = partial) {
      TallybookTrendChart(
          points = points,
          selectedIndex = selectedIndex,
          onSelect = { selectedIndex = it },
          contentDescription = sampleSummary(partial),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = single) {
      TallybookTrendChart(
          points = points.take(1),
          selectedIndex = 0,
          onSelect = {},
          contentDescription = sampleSummary(single),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogEdgeTrendCharts(points)
  }
}

@Composable
private fun CatalogEdgeTrendCharts(points: List<TrendPoint>) {
  val zero = stringResource(Res.string.ui_catalog_chart_all_zero)
  val empty = stringResource(Res.string.ui_catalog_state_empty)
  CatalogSample(label = zero) {
    TallybookTrendChart(
        points = points.map { it.copy(gastos = 0.0, ingresos = 0.0, partial = false) },
        selectedIndex = -1,
        onSelect = {},
        contentDescription = sampleSummary(zero),
        modifier = Modifier.fillMaxWidth(),
    )
  }
  CatalogSample(label = empty) {
    TallybookTrendChart(
        points = emptyList(),
        selectedIndex = -1,
        onSelect = {},
        contentDescription = sampleSummary(empty),
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun CatalogPaceCharts() {
  val cumulative = remember { SampleDailyGastos.runningReduce(Double::plus) }
  val pace = stringResource(Res.string.ui_catalog_chart_pace)
  val single = stringResource(Res.string.ui_catalog_chart_single_value)
  val empty = stringResource(Res.string.ui_catalog_state_empty)
  CatalogSurface {
    TallybookText(text = pace, style = TallybookTheme.typography.bodyEmphasis)
    CatalogSample(label = pace) {
      TallybookPaceChart(
          cumulative = cumulative,
          averageMonth = SAMPLE_AVERAGE_MONTH,
          projection = SAMPLE_PROJECTION,
          daysInMonth = SAMPLE_DAYS_IN_MONTH,
          todayLabel = stringResource(Res.string.ui_catalog_chart_today, money(cumulative.last())),
          contentDescription = sampleSummary(pace),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = single) {
      TallybookPaceChart(
          cumulative = cumulative.take(1),
          averageMonth = SAMPLE_AVERAGE_MONTH,
          projection = cumulative.first() * SAMPLE_DAYS_IN_MONTH,
          daysInMonth = SAMPLE_DAYS_IN_MONTH,
          todayLabel = stringResource(Res.string.ui_catalog_chart_today, money(cumulative.first())),
          contentDescription = sampleSummary(single),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = empty) {
      TallybookPaceChart(
          cumulative = emptyList(),
          averageMonth = 0.0,
          projection = 0.0,
          daysInMonth = SAMPLE_DAYS_IN_MONTH,
          todayLabel = stringResource(Res.string.ui_catalog_chart_today, money(0.0)),
          contentDescription = sampleSummary(empty),
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun CatalogCategoryTrendCharts() {
  val months = sampleMonths(SampleCategoryMonths.size)
  val color = TallybookTheme.colors.chart[2]
  val partial = stringResource(Res.string.ui_catalog_state_partial)
  val zero = stringResource(Res.string.ui_catalog_chart_all_zero)
  val empty = stringResource(Res.string.ui_catalog_state_empty)
  CatalogSurface {
    TallybookText(
        text = stringResource(Res.string.ui_catalog_chart_category_trend),
        style = TallybookTheme.typography.bodyEmphasis,
    )
    CatalogSample(label = partial) {
      TallybookCategoryTrendChart(
          values = SampleCategoryMonths,
          labels = months,
          average = SAMPLE_CATEGORY_AVERAGE,
          partialLast = true,
          color = color,
          contentDescription = sampleSummary(partial),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = zero) {
      TallybookCategoryTrendChart(
          values = SampleCategoryMonths.map { 0.0 },
          labels = months,
          average = 0.0,
          partialLast = false,
          color = color,
          contentDescription = sampleSummary(zero),
          modifier = Modifier.fillMaxWidth(),
      )
    }
    CatalogSample(label = empty) {
      TallybookCategoryTrendChart(
          values = emptyList(),
          labels = emptyList(),
          average = 0.0,
          partialLast = false,
          color = color,
          contentDescription = sampleSummary(empty),
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Preview
@Composable
internal fun CatalogChartsLightPreview() {
  TallybookTheme(darkTheme = false) {
    Column(Modifier.padding(TallybookTheme.spacing.space16)) { CatalogCharts() }
  }
}

@Preview
@Composable
internal fun CatalogChartsDarkPreview() {
  TallybookTheme(darkTheme = true) {
    Column(Modifier.padding(TallybookTheme.spacing.space16)) { CatalogCharts() }
  }
}
