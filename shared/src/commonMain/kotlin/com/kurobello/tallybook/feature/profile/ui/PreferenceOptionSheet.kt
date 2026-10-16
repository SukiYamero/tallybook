package com.kurobello.tallybook.feature.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

internal data class PreferenceOption<T>(val value: T, val label: String, val caption: String?)

@Composable
@Suppress("LongParameterList")
internal fun <T> PreferenceOptionSheet(
    visible: Boolean,
    title: String,
    note: String,
    options: List<PreferenceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    onHidden: () -> Unit,
) {
  TallybookModalBottomSheet(
      visible = visible,
      paneTitle = title,
      callbacks = ModalBottomSheetCallbacks(onDismiss = onDismiss, onHidden = onHidden),
  ) {
    PreferenceOptionContent(title, note, options, selected, onSelect)
  }
}

@Composable
@Suppress("LongParameterList")
internal fun <T> PreferenceOptionContent(
    title: String,
    note: String,
    options: List<PreferenceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .verticalScroll(rememberScrollState())
              .padding(
                  start = TallybookTheme.spacing.space20,
                  end = TallybookTheme.spacing.space20,
                  bottom = TallybookTheme.spacing.space16,
              ),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookSheetTitle(title)
    TallybookText(
        note,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
    Column(Modifier.fillMaxWidth().selectableGroup()) {
      options.forEach { option ->
        PreferenceOptionRow(option, option.value == selected) { onSelect(option.value) }
      }
    }
  }
}

@Composable
private fun PreferenceOptionRow(
    option: PreferenceOption<*>,
    selected: Boolean,
    onClick: () -> Unit,
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  role = Role.RadioButton,
                  onClick = onClick,
              )
              .padding(vertical = TallybookTheme.spacing.space8),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      TallybookText(
          option.label,
          style =
              if (selected) TallybookTheme.typography.bodyEmphasis
              else TallybookTheme.typography.body,
      )
      if (option.caption != null) {
        TallybookText(
            option.caption,
            color = TallybookTheme.colors.foreground.tertiary,
            style = TallybookTheme.typography.caption,
        )
      }
    }
    if (selected) {
      Image(
          imageVector = Lucide.Check,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
    }
  }
}
