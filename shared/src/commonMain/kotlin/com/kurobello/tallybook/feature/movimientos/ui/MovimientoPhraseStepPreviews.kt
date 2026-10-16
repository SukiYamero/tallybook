@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi

@Preview
@Composable
private fun PhraseStepEmptyLightPreview() =
    PhraseStepPreview(text = "", preview = MovimientoEntrySamples::emptyPreview, darkTheme = false)

@Preview
@Composable
private fun PhraseStepEmptyDarkPreview() =
    PhraseStepPreview(text = "", preview = MovimientoEntrySamples::emptyPreview, darkTheme = true)

@Preview
@Composable
private fun PhraseStepPartialLightPreview() =
    PhraseStepPreview(
        text = "Spent 20.000",
        preview = MovimientoEntrySamples::partialPreview,
        darkTheme = false,
    )

@Preview
@Composable
private fun PhraseStepCompleteLightPreview() =
    PhraseStepPreview(
        text = MovimientoEntrySamples.SENTENCE,
        preview = MovimientoEntrySamples::completePreview,
        darkTheme = false,
    )

@Preview
@Composable
private fun PhraseStepCompleteDarkPreview() =
    PhraseStepPreview(
        text = MovimientoEntrySamples.SENTENCE,
        preview = MovimientoEntrySamples::completePreview,
        darkTheme = true,
    )

@Composable
private fun PhraseStepPreview(
    text: String,
    preview: (MovimientoEntrySamples) -> PhrasePreviewUi,
    darkTheme: Boolean,
) {
  TallybookTheme(darkTheme = darkTheme) {
    MovimientoPhraseStep(
        text = text,
        preview = preview(rememberMovimientoEntrySamples()),
        moneda = MovimientoEntrySamples.moneda,
        onTextChange = {},
        onContinue = {},
        onFillManually = {},
        focusRequester = remember { FocusRequester() },
        today = MovimientoEntrySamples.today,
        requestFocusOnStart = false,
    )
  }
}
