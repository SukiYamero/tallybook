@file:Suppress("LongMethod", "LongParameterList")

package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Immutable
internal data class TallybookPeriodOption<T : Any>(
    val value: T,
    val label: String,
    val caption: String? = null,
    val hasData: Boolean = false,
    val key: String = value.toString(),
)

@Composable
internal fun TallybookPeriodPickerHeader(
    title: String,
    yearControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
  Row(
      modifier = modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.Top,
  ) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
    ) {
      TallybookText(text = title, style = TallybookTheme.typography.title)
      if (supportingText != null) {
        TallybookText(
            text = supportingText,
            color = TallybookTheme.colors.foreground.secondary,
            style = TallybookTheme.typography.body,
        )
      }
    }
    yearControl()
  }
}

@Composable
internal fun <T : Any> TallybookPeriodPickerRow(
    options: List<TallybookPeriodOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  require(options.isNotEmpty()) { "Period picker options must not be empty." }

  val listState = rememberLazyListState()
  val selectedIndex = remember(options, selected) { options.indexOfFirst { it.value == selected } }
  LaunchedEffect(selectedIndex) {
    if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
  }

  LazyRow(
      modifier =
          modifier.selectableGroup().semantics { this.contentDescription = contentDescription },
      state = listState,
      contentPadding = PaddingValues(horizontal = TallybookTheme.spacing.space4),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
  ) {
    items(items = options, key = { it.key }) { option ->
      TallybookPeriodPickerChip(
          option = option,
          selected = option.value == selected,
          enabled = enabled,
          onSelect = onSelect,
      )
    }
  }
}

@Composable
private fun <T : Any> TallybookPeriodPickerChip(
    option: TallybookPeriodOption<T>,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val colors = TallybookTheme.colors
  val containerColor = if (selected) colors.brand.primary else colors.surface.sunken
  val contentColor = if (selected) colors.brand.onPrimary else colors.foreground.primary
  val captionColor = if (selected) contentColor.copy(alpha = 0.65f) else colors.foreground.faint
  val indicatorColor =
      when {
        selected -> colors.brand.onPrimary
        option.hasData -> colors.brand.primary
        else -> Color.Transparent
      }
  TallybookSurface(
      modifier =
          Modifier.widthIn(min = TallybookTheme.dimensions.minimumTouchTarget)
              .defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.RadioButton,
                  onClick = { onSelect(option.value) },
              ),
      style =
          TallybookSurfaceStyle(
              color = containerColor,
              contentColor = contentColor,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border = null,
              contentAlignment = Alignment.Center,
          ),
      isPressed = isPressed,
  ) {
    Column(
        modifier = Modifier.padding(horizontal = TallybookTheme.spacing.space10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
    ) {
      if (option.caption != null) {
        TallybookText(
            text = option.caption,
            color = captionColor,
            style = TallybookTheme.typography.caption,
        )
      }
      TallybookText(
          text = option.label,
          color = contentColor,
          style = TallybookTheme.typography.bodyEmphasis,
      )
      Box(
          modifier =
              Modifier.size(TallybookTheme.spacing.space4)
                  .background(indicatorColor, RoundedCornerShape(TallybookTheme.radii.radius8))
      )
    }
  }
}

@Composable
internal fun TallybookYearMenu(
    years: List<Int>,
    selectedYear: Int,
    onSelect: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxVisibleYears: Int = DEFAULT_MAX_VISIBLE_YEARS,
) {
  require(years.isNotEmpty()) { "Year menu options must not be empty." }
  require(maxVisibleYears > 0) { "Year menu must show at least one year." }

  var isOpen by remember { mutableStateOf(false) }
  val popupVisibility = remember { MutableTransitionState(false) }
  popupVisibility.targetState = isOpen
  val overlayState = LocalTallybookViewportOverlayState.current
  val overlayOwner = remember { Any() }
  val density = androidx.compose.ui.platform.LocalDensity.current
  val verticalOffset =
      remember(density) { with(density) { TallybookTheme.spacing.space8.roundToPx() } }
  val popupPositionProvider =
      remember(density) {
        TallybookAnchoredPopupPositionProvider(
            verticalOffset = verticalOffset,
            edgeMargin = with(density) { TallybookTheme.spacing.space16.roundToPx() },
        )
      }
  val chevronRotation by
      animateFloatAsState(
          targetValue = if (isOpen) 180f else 0f,
          animationSpec =
              if (isOpen) TallybookOverlayMotion.enterAnimationSpec
              else TallybookOverlayMotion.exitAnimationSpec,
          label = "year menu chevron",
      )
  DisposableEffect(overlayState, overlayOwner) {
    onDispose { overlayState?.hideScrim(overlayOwner) }
  }

  Box(modifier = modifier) {
    TallybookYearMenuTrigger(
        year = selectedYear,
        contentDescription = contentDescription,
        chevronRotation = chevronRotation,
        enabled = enabled,
        onClick = {
          if (isOpen) {
            overlayState?.hideScrim(overlayOwner)
          } else {
            overlayState?.showScrim(overlayOwner)
          }
          isOpen = !isOpen
        },
    )
    if (popupVisibility.currentState || popupVisibility.targetState) {
      Popup(
          popupPositionProvider = popupPositionProvider,
          onDismissRequest = {
            overlayState?.hideScrim(overlayOwner)
            isOpen = false
          },
          properties = PopupProperties(focusable = true),
      ) {
        AnimatedVisibility(
            visibleState = popupVisibility,
            enter =
                fadeIn(animationSpec = TallybookOverlayMotion.enterAnimationSpec) +
                    scaleIn(
                        animationSpec = TallybookOverlayMotion.enterAnimationSpec,
                        initialScale = TallybookOverlayMotion.initialScale,
                    ),
            exit =
                fadeOut(animationSpec = TallybookOverlayMotion.exitAnimationSpec) +
                    scaleOut(
                        animationSpec = TallybookOverlayMotion.exitAnimationSpec,
                        targetScale = TallybookOverlayMotion.initialScale,
                    ),
        ) {
          TallybookYearMenuContent(
              years = years,
              selectedYear = selectedYear,
              onSelect = { year ->
                onSelect(year)
                overlayState?.hideScrim(overlayOwner)
                isOpen = false
              },
              maxVisibleYears = maxVisibleYears,
          )
        }
      }
    }
  }
}

@Composable
private fun TallybookYearMenuTrigger(
    year: Int,
    contentDescription: String,
    chevronRotation: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  TallybookSurface(
      modifier =
          Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  enabled = enabled,
                  role = Role.Button,
                  onClick = onClick,
              )
              .semantics { this.contentDescription = contentDescription },
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.raised,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius10),
              border = null,
              contentAlignment = Alignment.Center,
          ),
      isPressed = isPressed,
  ) {
    Row(
        modifier =
            Modifier.padding(
                horizontal = TallybookTheme.spacing.space12,
                vertical = TallybookTheme.spacing.space8,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(text = year.toString(), style = TallybookTheme.typography.bodyEmphasis)
      androidx.compose.foundation.Image(
          imageVector = Lucide.ChevronDown,
          contentDescription = null,
          modifier =
              Modifier.size(TallybookTheme.dimensions.datePickerChipIconSize).graphicsLayer {
                rotationZ = chevronRotation
              },
          colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
      )
    }
  }
}

@Composable
private fun TallybookYearMenuContent(
    years: List<Int>,
    selectedYear: Int,
    onSelect: (Int) -> Unit,
    maxVisibleYears: Int,
) {
  val listState = rememberLazyListState()
  val selectedIndex = remember(years, selectedYear) { years.indexOf(selectedYear) }
  val maximumHeight =
      TallybookTheme.dimensions.minimumTouchTarget * maxVisibleYears +
          TallybookTheme.spacing.space2 * (maxVisibleYears - 1) +
          TallybookTheme.spacing.space8
  LaunchedEffect(selectedIndex) { if (selectedIndex >= 0) listState.scrollToItem(selectedIndex) }
  TallybookSurface(
      modifier =
          Modifier.widthIn(
              min = TallybookTheme.dimensions.minimumTouchTarget,
              max = TallybookTheme.dimensions.yearMenuMaximumWidth,
          ),
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.sunken,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border = null,
          ),
  ) {
    LazyColumn(
        modifier =
            Modifier.heightIn(max = maximumHeight)
                .padding(TallybookTheme.spacing.space4)
                .selectableGroup(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
    ) {
      items(items = years, key = { it }) { year ->
        TallybookYearMenuOption(
            year = year,
            selected = year == selectedYear,
            onSelect = onSelect,
        )
      }
    }
  }
}

private const val DEFAULT_MAX_VISIBLE_YEARS = 4

@Composable
private fun TallybookYearMenuOption(
    year: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  TallybookSurface(
      modifier =
          Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .selectable(
                  selected = selected,
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.RadioButton,
                  onClick = { onSelect(year) },
              ),
      style =
          TallybookSurfaceStyle(
              color = if (selected) TallybookTheme.colors.surface.highest else Color.Transparent,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius10),
              border = null,
              contentAlignment = Alignment.CenterStart,
          ),
      isPressed = isPressed,
  ) {
    Row(
        modifier = Modifier.padding(horizontal = TallybookTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          text = year.toString(),
          modifier = Modifier.weight(1f),
          style = TallybookTheme.typography.bodyEmphasis,
      )
      if (selected) {
        androidx.compose.foundation.Image(
            imageVector = Lucide.Check,
            contentDescription = null,
            modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
            colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.secondary),
        )
      }
    }
  }
}
