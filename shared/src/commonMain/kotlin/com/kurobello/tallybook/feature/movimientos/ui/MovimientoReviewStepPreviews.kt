@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoReviewUi

@Preview
@Composable
private fun ReviewStepLightPreview() =
    ReviewStepPreview(MovimientoEntrySamples::review, darkTheme = false)

@Preview
@Composable
private fun ReviewStepDarkPreview() =
    ReviewStepPreview(MovimientoEntrySamples::review, darkTheme = true)

@Preview
@Composable
private fun ReviewStepDivergedPreview() =
    ReviewStepPreview(MovimientoEntrySamples::divergedReview, darkTheme = false)

@Preview
@Composable
private fun ReviewStepSubmittingPreview() =
    ReviewStepPreview({ it.review.copy(isSubmitting = true, canJoin = false) }, darkTheme = false)

@Composable
private fun ReviewStepPreview(
    review: (MovimientoEntrySamples) -> MovimientoReviewUi,
    darkTheme: Boolean,
) {
  TallybookTheme(darkTheme = darkTheme) {
    MovimientoReviewStep(
        review = review(rememberMovimientoEntrySamples()),
        callbacks =
            MovimientoReviewCallbacks(
                etiquetas = MovimientoEtiquetasCallbacks(onAdd = {}, onRemove = {}, onRetry = {}),
                onPartEdit = {},
                onPartCategoria = {},
                onPartRemoved = {},
                onJoin = {},
                onSave = {},
            ),
        today = MovimientoEntrySamples.today,
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
    )
  }
}
