@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSentenceUi

@Preview
@Composable
private fun SentenceStepEmptyLightPreview() =
    SentenceStepPreview(MovimientoEntrySamples::emptySentence, darkTheme = false)

@Preview
@Composable
private fun SentenceStepEmptyDarkPreview() =
    SentenceStepPreview(MovimientoEntrySamples::emptySentence, darkTheme = true)

@Preview
@Composable
private fun SentenceStepPartialLightPreview() =
    SentenceStepPreview(MovimientoEntrySamples::partialSentence, darkTheme = false)

@Preview
@Composable
private fun SentenceStepCompleteLightPreview() =
    SentenceStepPreview(MovimientoEntrySamples::completeSentence, darkTheme = false)

@Preview
@Composable
private fun SentenceStepCompleteDarkPreview() =
    SentenceStepPreview(MovimientoEntrySamples::completeSentence, darkTheme = true)

@Preview
@Composable
private fun SentenceStepIncomeLightPreview() =
    SentenceStepPreview(MovimientoEntrySamples::incomeSentence, darkTheme = false)

@Composable
private fun SentenceStepPreview(
    sentence: (MovimientoEntrySamples) -> MovimientoSentenceUi,
    darkTheme: Boolean,
) {
  TallybookTheme(darkTheme = darkTheme) {
    MovimientoSentenceStep(
        sentence = sentence(rememberMovimientoEntrySamples()),
        callbacks = noOpSentenceCallbacks,
        focusRequester = remember { FocusRequester() },
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        requestFocusOnStart = false,
    )
  }
}

private val noOpSentenceCallbacks =
    MovimientoSentenceCallbacks(
        onTextChange = {},
        onContinue = {},
        onFillManually = {},
        onRemoveCategoria = {},
        onAddCategoria = {},
        onSuggestion = {},
    )
