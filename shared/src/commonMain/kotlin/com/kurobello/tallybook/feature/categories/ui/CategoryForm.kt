package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.CategoryTintColors
import com.kurobello.tallybook.core.ui.category.tintColors
import com.kurobello.tallybook.core.ui.category.toImageVector
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.component.resolveTallybookSurfaceContainerColor
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryNombreError
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_duplicate_message
import tallybook.shared.generated.resources.category_duplicate_otros
import tallybook.shared.generated.resources.category_form_color_label
import tallybook.shared.generated.resources.category_form_icon_collapse
import tallybook.shared.generated.resources.category_form_icon_expand
import tallybook.shared.generated.resources.category_form_icon_label
import tallybook.shared.generated.resources.category_form_icon_selected
import tallybook.shared.generated.resources.category_form_more_options
import tallybook.shared.generated.resources.category_form_name_blank
import tallybook.shared.generated.resources.category_form_name_label
import tallybook.shared.generated.resources.category_form_name_placeholder
import tallybook.shared.generated.resources.category_form_name_too_long
import tallybook.shared.generated.resources.description_optional

/** [COMPACT] hides tint and description behind "More options" until the draft reveals them. */
internal enum class CategoryFormMode {
  COMPACT,
  FULL,
}

/** What the caller offers for the categoria a write collided with ("Use X", "Open X"). */
@Immutable internal data class CategoryDuplicateAction(val label: String, val onClick: () -> Unit)

@Stable
internal interface CategoryFormActions {
  fun onNombreChanged(value: String)

  fun onIconoSelected(icono: CategoryIcon)

  fun onColorSelected(color: CategoryTint)

  fun onDescripcionChanged(value: String)

  fun onMoreOptions()
}

/**
 * The fields of one custom categoria. Saving, titles and dismissal belong to the caller.
 * [duplicateAction] is never shown for the Otros fallback, which can never be selected.
 */
@Composable
internal fun CategoryForm(
    state: CategoryDraftState,
    actions: CategoryFormActions,
    mode: CategoryFormMode,
    modifier: Modifier = Modifier,
    duplicateAction: CategoryDuplicateAction? = null,
) {
  val enabled = !state.saving
  Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
      CategoryNombreField(state, actions, enabled)
      val duplicate = state.duplicate
      if (duplicateAction != null && duplicate != null && duplicate.id != CATEGORIA_OTROS_ID) {
        TallybookTextButton(onClick = duplicateAction.onClick, enabled = enabled) {
          TallybookText(duplicateAction.label)
        }
      }
    }
    CategoryIconPicker(state.icono, state.color, actions::onIconoSelected, enabled)
    if (mode == CategoryFormMode.COMPACT && !state.moreOptions) {
      TallybookTextButton(onClick = actions::onMoreOptions, enabled = enabled) {
        TallybookText(stringResource(Res.string.category_form_more_options))
      }
    } else {
      CategoryTintPicker(state.color, actions::onColorSelected, enabled)
      CategoryDescripcionField(state.descripcion, actions::onDescripcionChanged, enabled)
    }
  }
}

@Composable
private fun CategoryNombreField(
    state: CategoryDraftState,
    actions: CategoryFormActions,
    enabled: Boolean,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = state.nombre,
      onValueChange = actions::onNombreChanged,
      modifier = Modifier.fillMaxWidth(),
      label = stringResource(Res.string.category_form_name_label),
      placeholder = stringResource(Res.string.category_form_name_placeholder),
      message = nombreMessage(state),
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done,
          ),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}

@Composable
private fun nombreMessage(state: CategoryDraftState): TallybookFieldMessage {
  val duplicate = state.duplicate
  return when {
    state.visibleNombreError == CategoryNombreError.BLANK ->
        TallybookFieldMessage.Error(stringResource(Res.string.category_form_name_blank))
    state.visibleNombreError == CategoryNombreError.TOO_LONG ->
        TallybookFieldMessage.Error(
            stringResource(Res.string.category_form_name_too_long, CategoriaRules.NOMBRE_MAX)
        )
    duplicate?.id == CATEGORIA_OTROS_ID ->
        TallybookFieldMessage.Error(
            stringResource(Res.string.category_duplicate_otros, duplicate.nombre)
        )
    duplicate != null ->
        TallybookFieldMessage.Error(
            stringResource(Res.string.category_duplicate_message, duplicate.nombre)
        )
    else -> TallybookFieldMessage.Supporting("${state.nombre.length}/${CategoriaRules.NOMBRE_MAX}")
  }
}

@Composable
private fun CategoryIconPicker(
    selected: CategoryIcon,
    tint: CategoryTint,
    onSelect: (CategoryIcon) -> Unit,
    enabled: Boolean,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val colors = tint.tintColors()
  val selectedDescription =
      stringResource(
          Res.string.category_form_icon_selected,
          stringResource(selected.labelResource()),
      )
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookInputLabel(stringResource(Res.string.category_form_icon_label))
    Row(
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CategoryAvatar(
          imageVector = selected.toImageVector(),
          colors = colors,
          modifier = Modifier.semantics { contentDescription = selectedDescription },
      )
      TallybookTextButton(onClick = { expanded = !expanded }, enabled = enabled) {
        TallybookText(
            stringResource(
                if (expanded) Res.string.category_form_icon_collapse
                else Res.string.category_form_icon_expand
            )
        )
      }
    }
    if (expanded) {
      FlowRow(
          modifier = Modifier.fillMaxWidth().selectableGroup(),
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
      ) {
        SelectableCategoryIcons.forEach { icono ->
          key(icono) {
            CategoryIconOption(
                icono = icono,
                selected = icono == selected,
                colors = colors,
                enabled = enabled,
                onClick = { onSelect(icono) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryIconOption(
    icono: CategoryIcon,
    selected: Boolean,
    colors: CategoryTintColors,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val themeColors = TallybookTheme.colors
  CategoryOptionTarget(
      label = stringResource(icono.labelResource()),
      selected = selected,
      enabled = enabled,
      onClick = onClick,
      visualSize = TallybookTheme.dimensions.categoryIconOptionVisualSize,
      containerColor = if (selected) colors.container else Color.Transparent,
      borderColor = if (selected) colors.content else themeColors.border.subtle,
  ) {
    CategoryOptionIcon(
        imageVector = icono.toImageVector(),
        color =
            when {
              !enabled -> themeColors.foreground.disabled
              selected -> colors.content
              else -> themeColors.foreground.secondary
            },
        size = TallybookTheme.dimensions.iconMedium,
    )
  }
}

@Composable
private fun CategoryTintPicker(
    selected: CategoryTint,
    onSelect: (CategoryTint) -> Unit,
    enabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
    TallybookInputLabel(stringResource(Res.string.category_form_color_label))
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      CategoryTint.entries.forEach { tint ->
        key(tint) {
          CategoryTintSwatch(
              tint = tint,
              selected = tint == selected,
              enabled = enabled,
              onClick = { onSelect(tint) },
          )
        }
      }
    }
  }
}

@Composable
private fun CategoryTintSwatch(
    tint: CategoryTint,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val accent = tint.tintColors().content
  CategoryOptionTarget(
      label = stringResource(tint.labelResource()),
      selected = selected,
      enabled = enabled,
      onClick = onClick,
      visualSize = TallybookTheme.dimensions.categoryTintSwatchVisualSize,
      containerColor = if (enabled) accent else TallybookTheme.colors.foreground.disabled,
      borderColor = if (selected) TallybookTheme.colors.foreground.primary else Color.Transparent,
  ) {
    if (selected) {
      CategoryOptionIcon(
          imageVector = Lucide.Check,
          color = TallybookTheme.colors.surface.card,
          size = TallybookTheme.dimensions.iconSmall,
      )
    }
  }
}

/** A circular choice drawn at [visualSize] inside a minimum touch target, announced by [label]. */
@Composable
@Suppress("LongParameterList")
private fun CategoryOptionTarget(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    visualSize: Dp,
    containerColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  Box(
      modifier =
          Modifier.size(TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.RadioButton,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              )
              .semantics { contentDescription = label },
      contentAlignment = Alignment.Center,
  ) {
    Box(
        modifier =
            Modifier.size(visualSize)
                .clip(CircleShape)
                .background(
                    resolveTallybookSurfaceContainerColor(
                        containerColor = containerColor,
                        pressedOverlay = TallybookTheme.colors.interaction.pressedOverlay,
                        isPressed = isPressed,
                        hasTapFeedback = tapFeedback.isActive,
                    )
                )
                .border(
                    width =
                        if (selected) TallybookTheme.dimensions.categoryOptionSelectedBorderWidth
                        else TallybookTheme.dimensions.borderThin,
                    color = borderColor,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
      content()
    }
  }
}

@Composable
private fun CategoryOptionIcon(imageVector: ImageVector, color: Color, size: Dp) {
  Image(
      imageVector = imageVector,
      contentDescription = null,
      modifier = Modifier.size(size),
      colorFilter = ColorFilter.tint(color),
  )
}

@Composable
private fun CategoryDescripcionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth(),
      label = stringResource(Res.string.description_optional),
      message =
          TallybookFieldMessage.Supporting("${value.length}/${CategoriaRules.DESCRIPCION_MAX}"),
      enabled = enabled,
      singleLine = false,
      minLines = DESCRIPCION_MIN_LINES,
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done,
          ),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}

private const val DESCRIPCION_MIN_LINES = 3

/** The dashed circle stays reserved for the Otros "to complete" treatment. */
internal val SelectableCategoryIcons: List<CategoryIcon> =
    CategoryIcon.entries.filter { it != CategoryIcon.CIRCLE_DASHED }
