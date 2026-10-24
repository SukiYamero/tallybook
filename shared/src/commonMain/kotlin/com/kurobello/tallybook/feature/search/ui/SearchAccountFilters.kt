package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.account.actividadCuentaLabel
import com.kurobello.tallybook.core.ui.component.TallybookChoicePill
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.activity_missing_account
import tallybook.shared.generated.resources.search_accounts_label
import tallybook.shared.generated.resources.search_remove_filter

/**
 * The viewed currency's accounts, archived included, plus any selected account of another currency
 * so it can be cleared. Selected IDs no account holds get a fallback with a remove action, like a
 * missing category.
 */
@Composable
@Suppress("LongParameterList")
internal fun SearchAccountFilters(
    cuentas: List<Cuenta>,
    moneda: Moneda,
    primary: Moneda,
    selectedIds: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val shown =
      remember(cuentas, moneda, selectedIds) {
        cuentas.filter { it.moneda == moneda || it.id in selectedIds }
      }
  val missingIds =
      remember(cuentas, selectedIds) { selectedIds - cuentas.mapTo(mutableSetOf()) { it.id } }
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    TallybookSectionHeading(stringResource(Res.string.search_accounts_label))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      shown.forEach { cuenta ->
        TallybookChoicePill(
            actividadCuentaLabel(cuenta, primary),
            cuenta.id in selectedIds,
            { onToggle(cuenta.id) },
            enabled = enabled,
            role = Role.Button,
        )
      }
    }
    missingIds.sorted().forEach { id ->
      val label = stringResource(Res.string.activity_missing_account)
      val removal = stringResource(Res.string.search_remove_filter, label)
      TallybookTextButton(
          { onToggle(id) },
          enabled = enabled,
          modifier =
              Modifier.semantics {
                contentDescription = removal
                selected = true
              },
      ) {
        TallybookText(label, style = TallybookTheme.typography.controlValue)
      }
    }
  }
}
