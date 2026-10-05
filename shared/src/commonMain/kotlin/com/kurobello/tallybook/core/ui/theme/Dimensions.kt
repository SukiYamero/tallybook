package com.kurobello.tallybook.core.ui.theme

import androidx.compose.ui.unit.dp

internal object TallybookDimensions {
  private const val MOVIMIENTO_DETAILS_VISIBLE_FIELDS = 3

  val borderThin = 1.dp
  val minimumTouchTarget = TallybookSpacing.space48
  val screenHorizontal = TallybookSpacing.space20
  val screenTopFloor = TallybookSpacing.space24
  val screenBottomFloor = TallybookSpacing.space16
  val screenContentMaxWidth = 600.dp
  val modalSheetMaximumWidth = 640.dp
  val modalSheetDragAreaHeight = minimumTouchTarget
  val modalSheetDragHandleWidth = TallybookSpacing.space40
  val modalSheetDragHandleHeight = TallybookSpacing.space4
  val modalSheetBottomPadding = TallybookSpacing.space12
  val iconSmall = 16.dp
  val iconMedium = 20.dp
  val iconLarge = 24.dp
  val homeHeaderHeight = 44.dp
  val homeSearchHeight = 46.dp
  val homeSearchIcon = iconMedium
  val homeEmptyIconContainer = 56.dp
  val homeEmptyIcon = iconLarge
  val bottomNavigationContentHeight = 72.dp
  val bottomNavigationIcon = iconLarge
  val bottomNavigationSelectedContainer = 44.dp
  val bottomNavigationAddButton = 54.dp
  val bottomNavigationAddIcon = iconLarge
  val loadingIndicatorSize = 40.dp
  val loadingIndicatorStrokeWidth = 4.dp
  val containedLoadingMinHeight = 144.dp
  val iconButtonContainerSize = minimumTouchTarget
  val iconButtonIconSize = iconMedium
  val fieldMinimumHeight = minimumTouchTarget
  val movimientoDetailsMaximumHeight = fieldMinimumHeight * MOVIMIENTO_DETAILS_VISIBLE_FIELDS
  val movimientoFormMaximumHeight = 600.dp
  val fieldHorizontalPadding = TallybookSpacing.space12
  val fieldVerticalPadding = TallybookSpacing.space12
  val fieldLabelSpacing = TallybookSpacing.space10
  val fieldIconSize = iconMedium
  val fieldBorderWidth = borderThin
  val amountInputMinimumWidth = TallybookSpacing.space48
  val amountInputMaximumWidth = 240.dp
  val datePickerChipIconSize = TallybookSpacing.space14
  val datePickerMediumChipHeight = minimumTouchTarget
  val datePickerDaySize = TallybookSpacing.space40
  val datePickerMaximumWidth = 336.dp
  val categoryPickerResultsMaximumHeight = 420.dp
  val yearMenuMaximumWidth = 128.dp
  val segmentedControlMinimumHeight = minimumTouchTarget
  val movementSummaryIcon = iconSmall
}
