package com.kurobello.tallybook.core.ui.account

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Banknote
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.WalletCards
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.ui.component.LocalTallybookContentColor
import org.jetbrains.compose.resources.stringResource

/** [MedioPago.TRANSFERENCIA] avoids the two-way arrows kept for transfers between own accounts. */
private val icons: Map<MedioPago, ImageVector> =
    mapOf(
        MedioPago.EFECTIVO to Lucide.Banknote,
        MedioPago.DEBITO to Lucide.WalletCards,
        MedioPago.TRANSFERENCIA to Lucide.Send,
        MedioPago.CREDITO to Lucide.CreditCard,
    )

internal val MedioPago.icon: ImageVector
  get() = icons.getValue(this)

/** Pass a null [contentDescription] when the medio's label is already shown next to the icon. */
@Composable
internal fun MedioPagoIcon(
    medio: MedioPago,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(medio.labelResource),
    tint: Color = LocalTallybookContentColor.current,
) {
  Image(
      imageVector = medio.icon,
      contentDescription = contentDescription,
      modifier = modifier,
      colorFilter = ColorFilter.tint(tint),
  )
}
