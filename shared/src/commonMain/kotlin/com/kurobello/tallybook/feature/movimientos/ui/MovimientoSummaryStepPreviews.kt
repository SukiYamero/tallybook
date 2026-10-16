@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi

@Preview
@Composable
private fun SummaryStepCompleteLightPreview() =
    SummaryStepPreview(MovimientoEntrySamples.completeSummary, darkTheme = false)

@Preview
@Composable
private fun SummaryStepCompleteDarkPreview() =
    SummaryStepPreview(MovimientoEntrySamples.completeSummary, darkTheme = true)

@Preview
@Composable
private fun SummaryStepMissingAmountPreview() =
    SummaryStepPreview(MovimientoEntrySamples.missingAmountSummary, darkTheme = false)

@Preview
@Composable
private fun SummaryStepAmbiguousAmountPreview() =
    SummaryStepPreview(MovimientoEntrySamples.ambiguousAmountSummary, darkTheme = false)

@Preview
@Composable
private fun SummaryStepPendingLightPreview() =
    SummaryStepPreview(MovimientoEntrySamples.pendingSummary, darkTheme = false)

@Preview
@Composable
private fun SummaryStepPendingDarkPreview() =
    SummaryStepPreview(MovimientoEntrySamples.pendingSummary, darkTheme = true)

@Preview
@Composable
private fun SummaryStepSubmittingPreview() =
    SummaryStepPreview(MovimientoEntrySamples.submittingSummary, darkTheme = false)

@Preview
@Composable
private fun SummaryStepIncomePreview() =
    SummaryStepPreview(MovimientoEntrySamples.incomeSummary, darkTheme = false)

@Composable
private fun SummaryStepPreview(summary: MovimientoSummaryUi, darkTheme: Boolean) {
  TallybookTheme(darkTheme = darkTheme) {
    MovimientoSummaryStep(
        summary = summary,
        onSelectSuggestion = {},
        onEdit = {},
        onSave = {},
        today = MovimientoEntrySamples.today,
    )
  }
}
