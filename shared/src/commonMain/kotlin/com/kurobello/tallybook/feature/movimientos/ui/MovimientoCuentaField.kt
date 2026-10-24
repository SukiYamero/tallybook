package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.ui.account.CuentaSelectorField
import com.kurobello.tallybook.core.ui.account.CuentaTypeIcon
import com.kurobello.tallybook.core.ui.account.MedioPagoControl
import com.kurobello.tallybook.core.ui.account.cuentaDisplayName
import com.kurobello.tallybook.core.ui.account.labelResource
import com.kurobello.tallybook.core.ui.category.CategoryTagAppearance
import com.kurobello.tallybook.core.ui.category.CategoryTagButton
import com.kurobello.tallybook.core.ui.category.CategoryTagIcon
import com.kurobello.tallybook.core.ui.category.CategoryTagLabel
import com.kurobello.tallybook.core.ui.category.avatarTagPadding
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.CuentaFieldError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.entry_cuenta_also
import tallybook.shared.generated.resources.entry_cuenta_create
import tallybook.shared.generated.resources.entry_cuenta_empty
import tallybook.shared.generated.resources.entry_cuenta_empty_any
import tallybook.shared.generated.resources.entry_cuenta_label
import tallybook.shared.generated.resources.entry_cuenta_required
import tallybook.shared.generated.resources.entry_cuenta_unavailable
import tallybook.shared.generated.resources.entry_medio_label
import tallybook.shared.generated.resources.entry_medio_required

internal const val CUENTA_SELECTOR_TAG = "movimiento_cuenta_selector"

/** [onCreateCuenta] offers creating a cuenta in place when no compatible one exists. */
internal data class MovimientoCuentaCallbacks(
    val onMedioSelected: (MedioPago) -> Unit,
    val onCuentaSelected: (String) -> Unit,
    val onOpenPicker: () -> Unit,
    val onCreateCuenta: (() -> Unit)? = null,
) {
  companion object {
    /** For catalog samples and previews, where nothing is persisted. */
    val Inert =
        MovimientoCuentaCallbacks(onMedioSelected = {}, onCuentaSelected = {}, onOpenPicker = {})
  }
}

/**
 * Field callbacks backed by [MovimientoCuentaActions]; [onOpenPicker] opens the caller's picker.
 */
internal fun MovimientoCuentaActions.cuentaCallbacks(
    onOpenPicker: () -> Unit,
    onCreateCuenta: (() -> Unit)? = null,
) =
    MovimientoCuentaCallbacks(
        onMedioSelected = ::onMedioSelected,
        onCuentaSelected = ::onCuentaSelected,
        onOpenPicker = onOpenPicker,
        onCreateCuenta = onCreateCuenta,
    )

/**
 * The Medio section followed by the Account section of every movimiento surface. An error scrolls
 * the account into view the way a failed amount takes focus.
 */
@Composable
internal fun MovimientoCuentaField(
    field: MovimientoCuentaUi,
    callbacks: MovimientoCuentaCallbacks,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  val bringIntoView = remember { BringIntoViewRequester() }
  LaunchedEffect(field.cuentaError, field.isMedioMissing) {
    if (field.cuentaError != null || field.isMedioMissing) bringIntoView.bringIntoView()
  }
  Column(
      modifier = modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    MedioSection(field, callbacks, enabled)
    CuentaSection(field, callbacks, enabled)
  }
}

@Composable
private fun MedioSection(
    field: MovimientoCuentaUi,
    callbacks: MovimientoCuentaCallbacks,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.entry_medio_label))
    MedioPagoControl(
        selected = field.medio,
        options = field.medioOptions,
        onSelect = callbacks.onMedioSelected,
        enabled = enabled,
    )
    if (field.isMedioMissing) FieldError(stringResource(Res.string.entry_medio_required))
  }
}

@Composable
private fun CuentaSection(
    field: MovimientoCuentaUi,
    callbacks: MovimientoCuentaCallbacks,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookSectionHeading(stringResource(Res.string.entry_cuenta_label))
    if (field.options.isEmpty()) {
      CuentaEmptyState(field.medio, callbacks.onCreateCuenta, enabled)
    } else {
      CuentaSelectorField(
          selected = field.selected,
          compatibleCount = field.options.size,
          onOpenPicker = callbacks.onOpenPicker,
          modifier = Modifier.fillMaxWidth().testTag(CUENTA_SELECTOR_TAG),
          primaryMoneda = field.primaryMoneda,
          enabled = enabled,
          message = field.cuentaError?.let { TallybookFieldMessage.Error(it.message()) },
      )
    }
    if (field.suggestions.isNotEmpty()) {
      CuentaSuggestions(field, callbacks.onCuentaSelected, enabled)
    }
  }
}

@Composable
private fun CuentaFieldError.message(): String =
    when (this) {
      CuentaFieldError.MISSING -> stringResource(Res.string.entry_cuenta_required)
      CuentaFieldError.UNAVAILABLE -> stringResource(Res.string.entry_cuenta_unavailable)
    }

@Composable
private fun CuentaEmptyState(medio: MedioPago?, onCreate: (() -> Unit)?, enabled: Boolean) {
  val message =
      medio?.compatibleTipos?.firstOrNull()?.let {
        stringResource(Res.string.entry_cuenta_empty, stringResource(it.labelResource))
      } ?: stringResource(Res.string.entry_cuenta_empty_any)
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = message,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
    if (onCreate != null) {
      TallybookTextButton(onClick = onCreate, enabled = enabled) {
        TallybookText(stringResource(Res.string.entry_cuenta_create))
      }
    }
  }
}

@Composable
private fun CuentaSuggestions(
    field: MovimientoCuentaUi,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = stringResource(Res.string.entry_cuenta_also),
        color = TallybookTheme.colors.foreground.tertiary,
        style = TallybookTheme.typography.caption,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
      field.suggestions.forEach { cuenta ->
        CuentaSuggestionTag(
            cuenta = cuenta,
            name = cuentaDisplayName(cuenta, field.primaryMoneda),
            onClick = { onSelect(cuenta.id) },
            enabled = enabled,
        )
      }
    }
  }
}

@Composable
private fun CuentaSuggestionTag(
    cuenta: Cuenta,
    name: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
  val colors = TallybookTheme.colors
  val appearance =
      CategoryTagAppearance(
          container = colors.surface.card,
          border = if (enabled) colors.border.base else colors.border.subtle,
          text = if (enabled) colors.foreground.primary else colors.foreground.disabled,
          accent = if (enabled) colors.foreground.secondary else colors.foreground.disabled,
          padding = avatarTagPadding(leading = true),
      )
  CategoryTagButton(onClick = onClick, enabled = enabled, appearance = appearance) {
    CategoryTagIcon(Lucide.Plus, appearance.accent)
    CuentaTypeIcon(
        tipo = cuenta.tipo,
        contentDescription = null,
        modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
        tint = appearance.accent,
    )
    CategoryTagLabel(name)
  }
}

@Composable
private fun FieldError(text: String) {
  TallybookText(
      text = text,
      color = TallybookTheme.colors.status.danger.base,
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      style = TallybookTheme.typography.bodySmall,
  )
}
