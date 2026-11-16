package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessageContent
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.entry_cuenta_placeholder
import tallybook.shared.generated.resources.entry_cuenta_with_moneda

/** A cuenta's name, with its currency code when it is not in [primaryMoneda]. */
@Composable
internal fun cuentaDisplayName(cuenta: Cuenta, primaryMoneda: Moneda?): String =
    if (primaryMoneda == null || cuenta.moneda == primaryMoneda) cuenta.nombre
    else stringResource(Res.string.entry_cuenta_with_moneda, cuenta.nombre, cuenta.moneda.name)

/**
 * The selected cuenta of a movimiento, under a section heading the caller renders. With
 * [compatibleCount] up to one there is nothing to choose, so it shows the cuenta without opening a
 * picker; with none the caller shows its empty state instead.
 */
@Composable
@Suppress("LongParameterList")
internal fun CuentaSelectorField(
    selected: Cuenta?,
    compatibleCount: Int,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
    primaryMoneda: Moneda? = null,
    enabled: Boolean = true,
    message: TallybookFieldMessage? = null,
) {
  val name = selected?.let { cuentaDisplayName(it, primaryMoneda) }
  val icon: @Composable () -> Unit = {
    selected?.let {
      CuentaTypeIcon(
          tipo = it.tipo,
          modifier = Modifier.size(TallybookTheme.dimensions.fieldIconSize),
          tint = TallybookTheme.colors.foreground.tertiary,
      )
    }
  }
  if (selected != null && compatibleCount <= 1) {
    FixedCuenta(name.orEmpty(), message, modifier, icon)
  } else {
    TallybookSelectionField(
        label = "",
        value = name,
        placeholder = stringResource(Res.string.entry_cuenta_placeholder),
        onClick = onOpenPicker,
        modifier = modifier,
        enabled = enabled,
        message = message,
        leadingContent = if (selected != null) icon else null,
        labelContent = {},
    )
  }
}

@Composable
private fun FixedCuenta(
    name: String,
    message: TallybookFieldMessage?,
    modifier: Modifier,
    icon: @Composable () -> Unit,
) {
  Column(
      modifier = modifier.semantics(mergeDescendants = true) {},
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing),
  ) {
    TallybookSurface(
        modifier = Modifier.fillMaxWidth(),
        style =
            TallybookSurfaceStyle(
                color = TallybookTheme.colors.surface.deep,
                contentColor = TallybookTheme.colors.foreground.primary,
                shape = RoundedCornerShape(TallybookTheme.radii.radius12),
                border =
                    BorderStroke(
                        TallybookTheme.dimensions.fieldBorderWidth,
                        TallybookTheme.colors.border.subtle,
                    ),
            ),
    ) {
      Row(
          modifier =
              Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.fieldMinimumHeight)
                  .padding(
                      horizontal = TallybookTheme.dimensions.fieldHorizontalPadding,
                      vertical = TallybookTheme.dimensions.fieldVerticalPadding,
                  ),
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        icon()
        TallybookText(name, style = TallybookTheme.typography.body)
      }
    }
    if (message != null) TallybookFieldMessageContent(message = message)
  }
}

private fun previewCuenta(nombre: String, tipo: TipoCuenta, moneda: Moneda) =
    Cuenta(
        nombre,
        nombre,
        tipo,
        Money(0, moneda),
        null,
        false,
        Instant.parse("2026-01-01T00:00:00Z"),
        color = tipo.defaultColor,
    )

@Composable
private fun CuentaSelectorFieldPreviewContent() {
  Column(
      modifier = Modifier.padding(TallybookTheme.spacing.space16),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    CuentaSelectorField(
        selected = previewCuenta("NU", TipoCuenta.CREDITO, Moneda.COP),
        compatibleCount = 2,
        onOpenPicker = {},
        primaryMoneda = Moneda.COP,
    )
    CuentaSelectorField(
        selected = previewCuenta("Visa", TipoCuenta.CREDITO, Moneda.USD),
        compatibleCount = 2,
        onOpenPicker = {},
        primaryMoneda = Moneda.COP,
    )
    CuentaSelectorField(
        selected = previewCuenta("Efectivo", TipoCuenta.EFECTIVO, Moneda.COP),
        compatibleCount = 1,
        onOpenPicker = {},
    )
    CuentaSelectorField(
        selected = null,
        compatibleCount = 3,
        onOpenPicker = {},
        message = TallybookFieldMessage.Error("Choose an account to save."),
    )
  }
}

@Preview
@Composable
internal fun CuentaSelectorFieldLightPreview() {
  TallybookTheme(darkTheme = false) { CuentaSelectorFieldPreviewContent() }
}

@Preview
@Composable
internal fun CuentaSelectorFieldDarkPreview() {
  TallybookTheme(darkTheme = true) { CuentaSelectorFieldPreviewContent() }
}
