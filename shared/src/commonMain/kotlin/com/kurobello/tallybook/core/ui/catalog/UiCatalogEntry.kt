package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.CategorySuggestionChip
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoEntrySamples
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoPhraseStep
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoSummaryStep
import com.kurobello.tallybook.feature.movimientos.ui.rememberMovimientoEntrySamples
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_entry_phrase_step
import tallybook.shared.generated.resources.ui_catalog_entry_sentence
import tallybook.shared.generated.resources.ui_catalog_entry_summary_step
import tallybook.shared.generated.resources.ui_catalog_pending_avatar
import tallybook.shared.generated.resources.ui_catalog_pending_row
import tallybook.shared.generated.resources.ui_catalog_pending_treatment
import tallybook.shared.generated.resources.ui_catalog_state_ambiguous_amount
import tallybook.shared.generated.resources.ui_catalog_state_complete
import tallybook.shared.generated.resources.ui_catalog_state_disabled
import tallybook.shared.generated.resources.ui_catalog_state_empty
import tallybook.shared.generated.resources.ui_catalog_state_missing_amount
import tallybook.shared.generated.resources.ui_catalog_state_partial
import tallybook.shared.generated.resources.ui_catalog_state_pending_category
import tallybook.shared.generated.resources.ui_catalog_state_selected
import tallybook.shared.generated.resources.ui_catalog_state_submitting
import tallybook.shared.generated.resources.ui_catalog_state_suggested
import tallybook.shared.generated.resources.ui_catalog_suggestion_chips

internal fun LazyListScope.catalogEntryComponents(fixtures: UiCatalogFixtures) {
  catalogControlSection(
      "suggestion-chips",
      { stringResource(Res.string.ui_catalog_suggestion_chips) },
  ) {
    CatalogSuggestionChipMatrix()
  }
  catalogControlSection(
      "pending-treatment",
      { stringResource(Res.string.ui_catalog_pending_treatment) },
  ) {
    CatalogPendingMatrix(fixtures)
  }
  catalogControlSection(
      "entry-phrase-step",
      { stringResource(Res.string.ui_catalog_entry_phrase_step) },
  ) {
    CatalogPhraseStepMatrix()
  }
  catalogControlSection(
      "entry-summary-step",
      { stringResource(Res.string.ui_catalog_entry_summary_step) },
  ) {
    CatalogSummaryStepMatrix()
  }
}

@Composable
private fun CatalogSuggestionChipMatrix() {
  val category = rememberMovimientoEntrySamples().cinema
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_suggested)) {
      CategorySuggestionChip(category, selected = false, onClick = {})
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_selected)) {
      CategorySuggestionChip(category, selected = true, onClick = {})
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_disabled)) {
      CategorySuggestionChip(category, selected = false, onClick = {}, enabled = false)
    }
  }
}

@Composable
private fun CatalogPendingMatrix(fixtures: UiCatalogFixtures) {
  val pendingCategory = rememberMovimientoEntrySamples().other
  CatalogSurface {
    CatalogSample(label = stringResource(Res.string.ui_catalog_pending_avatar)) {
      Row(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
        CategoryAvatar(categoria = pendingCategory)
        CategoryAvatar(categoria = pendingCategory, pending = true)
      }
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_pending_row)) {
      MovimientoRow(
          movimiento = fixtures.movimiento.copy(categoriaId = pendingCategory.id),
          categoria = pendingCategory,
          modifier = Modifier.fillMaxWidth(),
          pending = true,
      )
    }
  }
}

@Composable
private fun CatalogPhraseStepMatrix() {
  val samples = rememberMovimientoEntrySamples()
  val sentence = stringResource(Res.string.ui_catalog_entry_sentence)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_empty)) {
      CatalogPhraseStep(initialText = "", preview = samples.emptyPreview)
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_partial)) {
      CatalogPhraseStep(initialText = sentence, preview = samples.partialPreview)
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_complete)) {
      CatalogPhraseStep(initialText = sentence, preview = samples.completePreview)
    }
  }
}

@Composable
private fun CatalogPhraseStep(initialText: String, preview: PhrasePreviewUi) {
  var text by rememberSaveable { mutableStateOf(initialText) }
  MovimientoPhraseStep(
      text = text,
      preview = preview,
      moneda = MovimientoEntrySamples.moneda,
      onTextChange = { text = it },
      onContinue = {},
      onFillManually = {},
      focusRequester = remember { FocusRequester() },
      today = MovimientoEntrySamples.today,
      requestFocusOnStart = false,
  )
}

@Composable
private fun CatalogSummaryStepMatrix() {
  val samples = rememberMovimientoEntrySamples()
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CatalogSummarySample(
        stringResource(Res.string.ui_catalog_state_complete),
        samples.completeSummary,
    )
    CatalogSummarySample(
        stringResource(Res.string.ui_catalog_state_missing_amount),
        samples.missingAmountSummary,
    )
    CatalogSummarySample(
        stringResource(Res.string.ui_catalog_state_ambiguous_amount),
        samples.ambiguousAmountSummary,
    )
    CatalogSummarySample(
        stringResource(Res.string.ui_catalog_state_pending_category),
        samples.pendingSummary,
    )
    CatalogSummarySample(
        stringResource(Res.string.ui_catalog_state_submitting),
        samples.submittingSummary,
    )
  }
}

@Composable
private fun CatalogSummarySample(label: String, summary: MovimientoSummaryUi) {
  CatalogSample(label = label) {
    var current by remember(summary) { mutableStateOf(summary) }
    MovimientoSummaryStep(
        summary = current,
        onSelectSuggestion = { chosen ->
          current =
              current.copy(
                  categoria = chosen,
                  categoriaIsPending = false,
                  suggestions =
                      current.suggestions - chosen +
                          listOfNotNull(
                              current.categoria.takeUnless { current.categoriaIsPending }
                          ),
              )
        },
        onEdit = {},
        onSave = {},
        today = MovimientoEntrySamples.today,
    )
  }
}
