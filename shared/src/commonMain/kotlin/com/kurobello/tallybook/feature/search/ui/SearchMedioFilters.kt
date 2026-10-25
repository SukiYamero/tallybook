package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.ui.account.MedioPagoFilterControl
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.entry_medio_label

@Composable
internal fun SearchMedioFilters(
    selected: Set<MedioPago>,
    enabled: Boolean,
    onToggle: (MedioPago) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSectionHeading(stringResource(Res.string.entry_medio_label))
    MedioPagoFilterControl(selected, onToggle, enabled = enabled)
  }
}
