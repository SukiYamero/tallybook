package com.kurobello.tallybook.feature.statistics.viewmodel

import com.kurobello.tallybook.core.model.DimensionTotal
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Share
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.core.model.exclusiveShares
import com.kurobello.tallybook.core.model.overlappingShares

/**
 * The rows list every group so each one stays reachable; those the donut folds into "Otras" take
 * its color, so a row's dot always matches its slice.
 */
internal fun exclusiveDistribution(
    totals: List<DimensionTotal>,
    gastos: Money,
    labels: StatisticsLabels,
): StatisticsDistribution {
  val slices = exclusiveShares(totals)
  val named = slices.count { it.key != null }
  return StatisticsDistribution(
      rows =
          everyShare(totals).mapIndexed { rank, share ->
            share.toRow(
                labels.of(share.key as StatisticsKey),
                if (rank < named) StatisticsColor.Rank(rank) else StatisticsColor.Otras,
            )
          },
      donut =
          slices.mapIndexed { rank, share ->
            val key = share.key
            if (key == null)
                StatisticsSlice(StatisticsLabel.Otras, share.basisPoints, StatisticsColor.Otras)
            else StatisticsSlice(labels.of(key), share.basisPoints, StatisticsColor.Rank(rank))
          },
      total = gastos.format(),
  )
}

internal fun overlappingDistribution(
    totals: List<DimensionTotal>,
    gastos: Money,
    labels: StatisticsLabels,
): StatisticsDistribution =
    StatisticsDistribution(
        rows =
            overlappingShares(totals, gastos).mapIndexed { rank, share ->
              share.toRow(labels.of(share.key as StatisticsKey), StatisticsColor.Rank(rank))
            },
        donut = null,
        total = gastos.format(),
    )

internal fun everyShare(totals: List<DimensionTotal>): List<Share> =
    exclusiveShares(totals, maxSlices = maxOf(MIN_SLICES, totals.size))

internal fun Share.toRow(label: StatisticsLabel, color: StatisticsColor) =
    StatisticsShareRow(
        key = key as StatisticsKey,
        label = label,
        amount = amount.format(),
        basisPoints = basisPoints,
        change = change,
        color = color,
    )

internal class StatisticsLabels(private val catalog: StatisticsCatalog) {
  fun of(key: StatisticsKey): StatisticsLabel =
      when (key) {
        is StatisticsKey.Categoria ->
            catalog.categorias[key.id]?.let { StatisticsLabel.Named(it.nombre) }
                ?: StatisticsLabel.UnknownCategoria
        is StatisticsKey.Etiqueta ->
            catalog.etiquetas[key.id]?.let(StatisticsLabel::Named)
                ?: StatisticsLabel.UnknownEtiqueta
        is StatisticsKey.Cuenta ->
            catalog.cuentas[key.id]?.let(StatisticsLabel::Named) ?: StatisticsLabel.UnknownCuenta
        is StatisticsKey.Medio -> StatisticsLabel.Medio(key.medio)
      }
}

private const val MIN_SLICES = 2
