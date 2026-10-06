package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
@Suppress("LongParameterList")
internal fun TallybookSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClear: () -> Unit,
    inputModifier: Modifier = Modifier,
) {
  TallybookTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = modifier,
      placeholder = placeholder,
      enabled = enabled,
      inputModifier = inputModifier,
      leadingContent = {
        Image(
            imageVector = Lucide.Search,
            contentDescription = null,
            modifier = Modifier.size(TallybookTheme.dimensions.fieldIconSize),
            colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
        )
      },
      trailingContent =
          if (query.isNotEmpty()) {
            {
              TallybookIconButton(
                  onClick = onClear,
                  icon = Lucide.X,
                  contentDescription = clearContentDescription,
                  enabled = enabled,
              )
            }
          } else {
            null
          },
  )
}
