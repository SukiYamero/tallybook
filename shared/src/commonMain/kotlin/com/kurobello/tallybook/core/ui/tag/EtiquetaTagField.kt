package com.kurobello.tallybook.core.ui.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tag_field_suggestions
import tallybook.shared.generated.resources.movimiento_tags_section
import tallybook.shared.generated.resources.optional_marker

/**
 * The optional tags of a movimiento: the [EtiquetaSectionHeading], [etiquetas] as removable tags in
 * display order and an Add tag that disappears once [max] is reached. [suggestions] already
 * selected are skipped, and none is offered while the field is full.
 */
@Composable
@Suppress("LongParameterList")
internal fun EtiquetaTagField(
    etiquetas: List<Etiqueta>,
    max: Int,
    onRemove: (Etiqueta) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<Etiqueta> = emptyList(),
    onSuggestion: (Etiqueta) -> Unit = {},
    enabled: Boolean = true,
) {
  val isFull = etiquetas.size >= max
  val selectedIds = etiquetas.mapTo(mutableSetOf(), Etiqueta::id)
  val offered = if (isFull) emptyList() else suggestions.filter { it.id !in selectedIds }
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    EtiquetaSectionHeading()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      etiquetas.forEach { etiqueta ->
        key(etiqueta.id) {
          EtiquetaTag(etiqueta = etiqueta, enabled = enabled, onRemove = { onRemove(etiqueta) })
        }
      }
      if (!isFull) EtiquetaAddTag(onClick = onAdd, enabled = enabled)
    }
    if (offered.isNotEmpty()) {
      EtiquetaTagFieldSuggestions(offered, onSuggestion, enabled)
    }
  }
}

/** "Tags" followed by a barely perceptible "optional" marker, read as a single heading. */
@Composable
internal fun EtiquetaSectionHeading(modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.semantics(mergeDescendants = true) { heading() },
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    TallybookSectionHeading(
        text = stringResource(Res.string.movimiento_tags_section),
        modifier = Modifier.alignByBaseline(),
    )
    TallybookText(
        text = stringResource(Res.string.optional_marker),
        modifier = Modifier.alignByBaseline(),
        color = TallybookTheme.colors.foreground.faint,
        style = TallybookTheme.typography.caption,
    )
  }
}

@Composable
private fun EtiquetaTagFieldSuggestions(
    suggestions: List<Etiqueta>,
    onSuggestion: (Etiqueta) -> Unit,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = stringResource(Res.string.category_tag_field_suggestions),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.label,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      suggestions.forEach { suggestion ->
        key(suggestion.id) {
          EtiquetaSuggestionTag(
              etiqueta = suggestion,
              onClick = { onSuggestion(suggestion) },
              enabled = enabled,
          )
        }
      }
    }
  }
}

private val previewFieldEtiquetas =
    listOf("Empresa", "Viaje Chile", "Casa", "Regalos", "Salud", "Auto").map { nombre ->
      Etiqueta("tag_$nombre", nombre, previewEtiquetaCreatedAt)
    }

@Composable
private fun EtiquetaTagFieldPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24)) {
    EtiquetaTagField(
        etiquetas = emptyList(),
        max = MAX_ETIQUETAS_POR_MOVIMIENTO,
        onRemove = {},
        onAdd = {},
    )
    EtiquetaTagField(
        etiquetas = previewFieldEtiquetas.take(2),
        max = MAX_ETIQUETAS_POR_MOVIMIENTO,
        onRemove = {},
        onAdd = {},
        suggestions = previewFieldEtiquetas.drop(2).take(2),
    )
    EtiquetaTagField(
        etiquetas = previewFieldEtiquetas,
        max = MAX_ETIQUETAS_POR_MOVIMIENTO,
        onRemove = {},
        onAdd = {},
    )
  }
}

@Preview
@Composable
internal fun EtiquetaTagFieldLightPreview() {
  TallybookTheme(darkTheme = false) { EtiquetaTagFieldPreviewMatrix() }
}

@Preview
@Composable
internal fun EtiquetaTagFieldDarkPreview() {
  TallybookTheme(darkTheme = true) { EtiquetaTagFieldPreviewMatrix() }
}
