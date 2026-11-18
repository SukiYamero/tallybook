package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.movimiento.signedMovimientoAmountText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.PartPreviewUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_part_no_category

private const val PART_TITULO_MAX_LINES = 2

/** One compact, read-only row per movimiento the sentence states; categories are chosen later. */
@Composable
internal fun DetectedPartList(parts: List<PartPreviewUi>, tipo: TipoMovimiento) {
  Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    Box(
        Modifier.fillMaxWidth()
            .height(TallybookTheme.dimensions.borderThin)
            .background(TallybookTheme.colors.border.subtle)
    )
    parts.forEach { part -> DetectedPartRow(part, tipo) }
  }
}

@Composable
private fun DetectedPartRow(part: PartPreviewUi, tipo: TipoMovimiento) {
  Column(
      modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      val monto = part.monto
      if (monto == null) {
        TallybookText(
            text = stringResource(Res.string.movimiento_amount_not_detected),
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.bodySmall,
        )
      } else {
        // Regular weight keeps the total above as the one emphasized amount of the card.
        TallybookText(
            text = signedMovimientoAmountText(monto, tipo),
            color =
                if (tipo == TipoMovimiento.INGRESO) TallybookTheme.colors.status.success.base
                else TallybookTheme.colors.foreground.primary,
            style = TallybookTheme.typography.body,
        )
      }
      Spacer(Modifier.weight(1f))
      DetectedPartCategory(part.categoria)
    }
    TallybookText(
        text = part.titulo,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
        maxLines = PART_TITULO_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun DetectedPartCategory(categoria: Categoria?) {
  Row(
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (categoria != null) {
      CategoryAvatar(categoria, Modifier.size(TallybookTheme.dimensions.iconMedium))
    }
    TallybookText(
        text = categoria?.nombre ?: stringResource(Res.string.movimiento_part_no_category),
        color =
            if (categoria == null) TallybookTheme.colors.foreground.tertiary
            else TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}
