package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
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
      BottomNavigationIcon(
          icon = Lucide.House,
          label = stringResource(Res.string.navigation_home),
          selected = true,
          modifier = Modifier.weight(1f),
      )
      BottomNavigationIcon(
          icon = Lucide.History,
          label = stringResource(Res.string.navigation_history),
          modifier = Modifier.weight(1f),
      )
      BottomNavigationAdd(
          label = stringResource(Res.string.navigation_add_movement),
          onClick = onAddMovement,
          modifier = Modifier.weight(1f),
      )
      BottomNavigationIcon(
          icon = Lucide.Search,
          label = stringResource(Res.string.navigation_search),
          modifier = Modifier.weight(1f),
      )
      BottomNavigationIcon(
          icon = Lucide.User,
          label = stringResource(Res.string.navigation_profile),
          modifier = Modifier.weight(1f),
      )
    }
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    )
  }
}

@Composable
private fun BottomNavigationIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
  Box(
      modifier = modifier.fillMaxHeight(),
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
                .semantics {
                  contentDescription = label
                  if (selected) this.selected = true
                },
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
  Box(
      modifier = modifier.fillMaxHeight(),
      contentAlignment = Alignment.Center,
  ) {
    Box(
        modifier =
            Modifier.size(TallybookTheme.dimensions.bottomNavigationAddButton)
                .clip(RoundedCornerShape(TallybookTheme.radii.radius18))
                .background(
                    color = TallybookTheme.colors.brand.primary,
                )
                .clickable(onClick = onClick, role = Role.Button)
                .semantics {
                  contentDescription = label
                  role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
      Image(
          imageVector = Lucide.Plus,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.bottomNavigationAddIcon),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.onPrimary),
      )
    }
  }
}
