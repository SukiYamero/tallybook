package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.History
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.User
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceStyle
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.navigation_home
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.navigation_search

@Composable
internal fun TallybookBottomNavigation(
    onAddMovement: () -> Unit,
    modifier: Modifier = Modifier,
    selectedDestination: Destination? = Home,
    onSelectDestination: ((Destination) -> Unit)? = null,
) {
  Column(
      modifier = modifier.fillMaxWidth().background(TallybookTheme.colors.navigation.surface),
  ) {
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .height(TallybookTheme.dimensions.borderThin)
                .background(TallybookTheme.colors.navigation.divider)
    )
    Row(
        modifier =
            Modifier.align(Alignment.CenterHorizontally)
                .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                .fillMaxWidth()
                .height(TallybookTheme.dimensions.bottomNavigationContentHeight)
                .padding(horizontal = TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      leadingTabs.forEach { PrimaryDestinationIcon(it, selectedDestination, onSelectDestination) }
      BottomNavigationAdd(
          label = stringResource(Res.string.navigation_add_movement),
          onClick = onAddMovement,
          modifier = Modifier.weight(1f),
      )
      trailingTabs.forEach { PrimaryDestinationIcon(it, selectedDestination, onSelectDestination) }
    }
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    )
  }
}

@Composable
private fun RowScope.PrimaryDestinationIcon(
    tab: PrimaryTab,
    selectedDestination: Destination?,
    onSelectDestination: ((Destination) -> Unit)?,
) {
  BottomNavigationIcon(
      icon = tab.icon,
      label = stringResource(tab.label),
      selected = selectedDestination == tab.destination,
      onClick = onSelectDestination?.let { { it(tab.destination) } },
      modifier = Modifier.weight(1f),
  )
}

private class PrimaryTab(
    val destination: Destination,
    val icon: ImageVector,
    val label: StringResource,
)

private val leadingTabs =
    listOf(
        PrimaryTab(Home, Lucide.House, Res.string.navigation_home),
        PrimaryTab(History, Lucide.History, Res.string.navigation_history),
    )

private val trailingTabs =
    listOf(
        PrimaryTab(Search, Lucide.Search, Res.string.navigation_search),
        PrimaryTab(Profile, Lucide.User, Res.string.navigation_profile),
    )

@Composable
private fun BottomNavigationIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
  Box(
      modifier =
          modifier
              .fillMaxHeight()
              .then(
                  if (onClick != null)
                      Modifier.selectable(
                          selected = selected,
                          role = Role.Tab,
                          interactionSource = remember { MutableInteractionSource() },
                          indication = null,
                          onClick = onClick,
                      )
                  else Modifier
              )
              .semantics { contentDescription = label },
      contentAlignment = Alignment.Center,
  ) {
    Box(
        modifier =
            Modifier.size(TallybookTheme.dimensions.bottomNavigationSelectedContainer)
                .then(
                    if (selected) {
                      Modifier.background(
                          color = TallybookTheme.colors.navigation.selectedContainer,
                          shape = RoundedCornerShape(TallybookTheme.radii.radius16),
                      )
                    } else {
                      Modifier
                    }
                )
                .semantics { if (onClick != null) this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
      Image(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.bottomNavigationIcon),
          colorFilter =
              ColorFilter.tint(
                  if (selected) {
                    TallybookTheme.colors.navigation.selected
                  } else {
                    TallybookTheme.colors.navigation.content
                  }
              ),
      )
    }
  }
}

@Composable
private fun BottomNavigationAdd(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  Box(
      modifier = modifier.fillMaxHeight(),
      contentAlignment = Alignment.Center,
  ) {
    TallybookSurface(
        modifier =
            Modifier.size(TallybookTheme.dimensions.bottomNavigationAddButton)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                      tapFeedback.trigger()
                      onClick()
                    },
                    role = Role.Button,
                )
                .semantics {
                  contentDescription = label
                  role = Role.Button
                },
        style =
            TallybookSurfaceStyle(
                color = TallybookTheme.colors.brand.primary,
                contentColor = TallybookTheme.colors.brand.onPrimary,
                shape = RoundedCornerShape(TallybookTheme.radii.radius18),
                border = null,
            ),
        isPressed = isPressed,
        hasTapFeedback = tapFeedback.isActive,
    ) {
      Image(
          imageVector = Lucide.Plus,
          contentDescription = null,
          modifier =
              Modifier.align(Alignment.Center)
                  .size(TallybookTheme.dimensions.bottomNavigationAddIcon),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.onPrimary),
      )
    }
  }
}
