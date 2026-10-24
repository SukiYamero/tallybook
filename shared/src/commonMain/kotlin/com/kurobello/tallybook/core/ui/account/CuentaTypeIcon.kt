package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Landmark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Wallet
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.component.LocalTallybookContentColor
import org.jetbrains.compose.resources.stringResource

private val icons: Map<TipoCuenta, ImageVector> =
    mapOf(
        TipoCuenta.EFECTIVO to Lucide.Wallet,
        TipoCuenta.BANCO to Lucide.Landmark,
        TipoCuenta.CREDITO to Lucide.CreditCard,
    )

internal val TipoCuenta.icon: ImageVector
  get() = icons.getValue(this)

/** Pass a null [contentDescription] when the type's label is already shown next to the icon. */
@Composable
internal fun CuentaTypeIcon(
    tipo: TipoCuenta,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(tipo.labelResource),
    tint: Color = LocalTallybookContentColor.current,
) {
  Image(
      imageVector = tipo.icon,
      contentDescription = contentDescription,
      modifier = modifier,
      colorFilter = ColorFilter.tint(tint),
  )
}
