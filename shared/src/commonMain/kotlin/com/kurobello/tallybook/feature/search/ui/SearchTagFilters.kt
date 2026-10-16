package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.tag.EtiquetaChip
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.search_tags_label

@Composable
internal fun SearchTagFilters(
    etiquetas: List<Etiqueta>,
    selectedIds: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSectionHeading(stringResource(Res.string.search_tags_label))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      etiquetas.forEach { etiqueta ->
        EtiquetaChip(
            etiqueta,
            etiqueta.id in selectedIds,
            { onToggle(etiqueta.id) },
            enabled = enabled,
        )
      }
    }
  }
}
