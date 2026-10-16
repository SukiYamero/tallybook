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
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoEntrySamples
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoSentenceCallbacks
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoSentenceStep
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoSummaryCallbacks
import com.kurobello.tallybook.feature.movimientos.ui.MovimientoSummaryStep
import com.kurobello.tallybook.feature.movimientos.ui.rememberMovimientoEntrySamples
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSentenceUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_entry_phrase_step
import tallybook.shared.generated.resources.ui_catalog_entry_summary_step
import tallybook.shared.generated.resources.ui_catalog_pending_avatar
import tallybook.shared.generated.resources.ui_catalog_pending_row
import tallybook.shared.generated.resources.ui_catalog_pending_treatment
import tallybook.shared.generated.resources.ui_catalog_state_ambiguous_amount
import tallybook.shared.generated.resources.ui_catalog_state_at_limit
import tallybook.shared.generated.resources.ui_catalog_state_complete
import tallybook.shared.generated.resources.ui_catalog_state_empty
import tallybook.shared.generated.resources.ui_catalog_state_missing_amount
import tallybook.shared.generated.resources.ui_catalog_state_partial
import tallybook.shared.generated.resources.ui_catalog_state_pending_category
import tallybook.shared.generated.resources.ui_catalog_state_submitting

internal fun LazyListScope.catalogEntryComponents(fixtures: UiCatalogFixtures) {
  catalogTagComponents()
  catalogCategoryForm()
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
    CatalogSentenceStepMatrix()
  }
  catalogControlSection(
      "entry-summary-step",
      { stringResource(Res.string.ui_catalog_entry_summary_step) },
  ) {
    CatalogSummaryStepMatrix()
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
private fun CatalogSentenceStepMatrix() {
  val samples = rememberMovimientoEntrySamples()
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_empty)) {
      CatalogSentenceStep(samples.emptySentence)
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_partial)) {
      CatalogSentenceStep(samples.partialSentence)
    }
    CatalogSample(label = stringResource(Res.string.ui_catalog_state_complete)) {
      CatalogSentenceStep(samples.completeSentence)
    }
  }
}

@Composable
private fun CatalogSentenceStep(sentence: MovimientoSentenceUi) {
  var current by rememberSaveable(sentence) { mutableStateOf(sentence.text) }
  MovimientoSentenceStep(
      sentence = sentence.copy(text = current),
      callbacks =
          MovimientoSentenceCallbacks(
              onTextChange = { current = it },
              onContinue = {},
              onFillManually = {},
              onRemoveCategoria = {},
              onAddCategoria = {},
              onSuggestion = {},
          ),
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
        stringResource(Res.string.ui_catalog_state_at_limit),
        samples.fullSummary,
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
    val catalog = rememberMovimientoEntrySamples().all
    MovimientoSummaryStep(
        summary = current,
        callbacks =
            MovimientoSummaryCallbacks(
                onRemoveCategoria = {
                  current = current.withCategorias(listOf(CATEGORIA_OTROS_ID), catalog)
                },
                onAddCategoria = {},
                onSuggestion = { chosen ->
                  current = current.withCategorias(listOf(chosen.id), catalog)
                },
                onDescripcionChange = { current = current.copy(descripcion = it) },
                onEdit = {},
                onSave = {},
            ),
        today = MovimientoEntrySamples.today,
    )
  }
}

private fun MovimientoSummaryUi.withCategorias(
    ids: List<String>,
    catalog: List<Categoria>,
): MovimientoSummaryUi {
  val selected = ids.mapNotNull { id -> catalog.firstOrNull { it.id == id } }
  return copy(
      categorias = selected,
      categoriasArePending = selected.singleOrNull()?.id == CATEGORIA_OTROS_ID,
      suggestions = catalog.filter { it.id != CATEGORIA_OTROS_ID && it.id !in ids },
  )
}
