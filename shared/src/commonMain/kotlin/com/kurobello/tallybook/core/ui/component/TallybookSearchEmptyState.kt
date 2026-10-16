package com.kurobello.tallybook.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SearchX
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

/**
 * The shared "no results" state of a search: a [Lucide.SearchX] icon, [message] and an optional
 * [action] with an [error] under it. It stays top-aligned so, under a search field, the keyboard
 * never covers the action. The top padding matches the message-to-action gap: together with the
 * callers' `space16` content spacing, the search field sits as far above the state as the message
 * sits above the action.
 */
@Composable
internal fun TallybookSearchEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
  Column(
      modifier = modifier.padding(top = TallybookTheme.spacing.space16),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
        imageVector = Lucide.SearchX,
        contentDescription = null,
        modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
        colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
    )
    Spacer(Modifier.height(TallybookTheme.spacing.space8))
    TallybookText(
        text = message,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.body.copy(textAlign = TextAlign.Center),
    )
    if (action != null) {
      Spacer(Modifier.height(TallybookTheme.spacing.space32))
      action()
    }
    if (error != null) {
      Spacer(Modifier.height(TallybookTheme.spacing.space12))
      TallybookText(
          text = error,
          color = TallybookTheme.colors.status.danger.base,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          style = TallybookTheme.typography.body.copy(textAlign = TextAlign.Center),
      )
    }
  }
}

@Composable
private fun TallybookSearchEmptyStatePreviewContent() {
  TallybookSearchEmptyState(
      message = "No results for “Gym”",
      modifier = Modifier.fillMaxWidth(),
      error = "Couldn't save the tag",
      action = { TallybookTextButton(onClick = {}) { TallybookText("Create “Gym”") } },
  )
}

@Preview
@Composable
internal fun TallybookSearchEmptyStateLightPreview() {
  TallybookTheme(darkTheme = false) { TallybookSearchEmptyStatePreviewContent() }
}

@Preview
@Composable
internal fun TallybookSearchEmptyStateDarkPreview() {
  TallybookTheme(darkTheme = true) { TallybookSearchEmptyStatePreviewContent() }
}
