package com.kurobello.tallybook.core.ui.tag

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.category.CategoryTagAppearance
import com.kurobello.tallybook.core.ui.category.CategoryTagIcon
import com.kurobello.tallybook.core.ui.category.CategoryTagLabel
import com.kurobello.tallybook.core.ui.category.CategoryTagSurface
import com.kurobello.tallybook.core.ui.category.avatarTagPadding
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tag_movimiento_count
import tallybook.shared.generated.resources.tag_no_movimientos

/**
 * A selectable tag for pickers, filters and the Tags screen. [count] adds the number of movimientos
 * after the name, announced as the chip's state. A `null` [onClick] renders non-interactive content
 * that keeps the selected semantics.
 */
@Composable
@Suppress("LongParameterList")
internal fun EtiquetaChip(
    etiqueta: Etiqueta,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    count: Long? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val appearance = etiquetaChipAppearance(selected = selected, enabled = enabled)
  val countLabel = count?.let { movimientoCountLabel(it) }
  Box(
      modifier =
          modifier
              .defaultMinSize(
                  minWidth = TallybookTheme.dimensions.minimumTouchTarget,
                  minHeight = TallybookTheme.dimensions.minimumTouchTarget,
              )
              .then(
                  if (onClick == null) {
                    Modifier.semantics(mergeDescendants = true) { this.selected = selected }
                  } else {
                    Modifier.selectable(
                        selected = selected,
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                          tapFeedback.trigger()
                          onClick()
                        },
                    )
                  }
              )
              .then(
                  if (countLabel == null) Modifier
                  else Modifier.semantics { stateDescription = countLabel }
              ),
      contentAlignment = Alignment.Center,
  ) {
    CategoryTagSurface(
        appearance = appearance,
        isPressed = isPressed,
        hasTapFeedback = tapFeedback.isActive,
    ) {
      CategoryTagIcon(Lucide.Hash, appearance.accent)
      CategoryTagLabel(etiqueta.nombre)
      if (count != null) {
        TallybookText(
            text = count.toString(),
            modifier = Modifier.clearAndSetSemantics {},
            color = appearance.accent,
            style = TallybookTheme.typography.controlValue,
        )
      }
    }
  }
}

@Composable
private fun movimientoCountLabel(count: Long): String =
    if (count == 0L) stringResource(Res.string.tag_no_movimientos)
    else
        pluralStringResource(
            Res.plurals.tag_movimiento_count,
            count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            count,
        )

@Composable
private fun etiquetaChipAppearance(selected: Boolean, enabled: Boolean): CategoryTagAppearance {
  val colors = TallybookTheme.colors
  val padding = avatarTagPadding(leading = true, trailing = true)
  return when {
    !enabled ->
        CategoryTagAppearance(
            container = colors.surface.card,
            border = colors.border.subtle,
            text = colors.foreground.disabled,
            accent = colors.foreground.disabled,
            padding = padding,
        )
    selected ->
        CategoryTagAppearance(
            container = colors.surface.deep,
            border = colors.brand.secondary,
            text = colors.foreground.primary,
            accent = colors.foreground.primary,
            padding = padding,
        )
    else ->
        CategoryTagAppearance(
            container = colors.surface.card,
            border = colors.border.base,
            text = colors.foreground.primary,
            accent = colors.foreground.tertiary,
            padding = padding,
        )
  }
}

private val previewEtiquetas =
    listOf(
        Etiqueta("tag_empresa", "Empresa", previewEtiquetaCreatedAt),
        Etiqueta("tag_viaje", "Viaje Chile", previewEtiquetaCreatedAt),
    )

@Composable
private fun EtiquetaChipPreviewMatrix() {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      EtiquetaChip(previewEtiquetas[0], selected = true, onClick = {})
      EtiquetaChip(previewEtiquetas[1], selected = false, onClick = {})
      EtiquetaChip(previewEtiquetas[1], selected = false, onClick = {}, enabled = false)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      EtiquetaChip(previewEtiquetas[0], selected = false, onClick = {}, count = PREVIEW_COUNT)
      EtiquetaChip(previewEtiquetas[1], selected = false, onClick = {}, count = 0)
    }
  }
}

private const val PREVIEW_COUNT = 12L

@Preview
@Composable
internal fun EtiquetaChipLightPreview() {
  TallybookTheme(darkTheme = false) { EtiquetaChipPreviewMatrix() }
}

@Preview
@Composable
internal fun EtiquetaChipDarkPreview() {
  TallybookTheme(darkTheme = true) { EtiquetaChipPreviewMatrix() }
}
