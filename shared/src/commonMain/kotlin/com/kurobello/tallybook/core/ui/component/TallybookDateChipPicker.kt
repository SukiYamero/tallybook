@file:Suppress("LongMethod", "LongParameterList")

package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.calendar_next_month
import tallybook.shared.generated.resources.calendar_previous_month
import tallybook.shared.generated.resources.date_chip_value
import tallybook.shared.generated.resources.date_picker_months
import tallybook.shared.generated.resources.date_picker_weekdays

@Composable
internal fun TallybookDateChipPicker(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxDate: LocalDate? = null,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
) {
  var isOpen by remember { mutableStateOf(false) }
  val popupVisibility = remember { MutableTransitionState(false) }
  popupVisibility.targetState = isOpen
  var visibleMonth by remember(value) { mutableStateOf(YearMonth(value.year, value.month)) }
  var isYearSelectorOpen by remember { mutableStateOf(false) }
  val monthNames = stringArrayResource(Res.array.date_picker_months)
  val weekdayNames = stringArrayResource(Res.array.date_picker_weekdays)
  val selectedLabel =
      stringResource(Res.string.date_chip_value, value.day, monthNames[value.month.ordinal])
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val density = LocalDensity.current
  val viewportOverlayState = LocalTallybookViewportOverlayState.current
  val viewportOverlayOwner = remember { Any() }
  DisposableEffect(viewportOverlayState, viewportOverlayOwner) {
    onDispose { viewportOverlayState?.hideScrim(viewportOverlayOwner) }
  }
  val popupPositionProvider =
      remember(density) {
        TallybookDatePickerPopupPositionProvider(
            verticalOffset = with(density) { TallybookTheme.spacing.space8.roundToPx() },
            edgeMargin = with(density) { TallybookTheme.spacing.space16.roundToPx() },
        )
      }
  Box(modifier = modifier) {
    TallybookSurface(
        modifier =
            Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                      viewportOverlayState?.showScrim(viewportOverlayOwner)
                      isOpen = true
                      visibleMonth = YearMonth(value.year, value.month)
                      isYearSelectorOpen = false
                    },
                )
                .semantics {
                  role = Role.Button
                  contentDescription = label
                },
        style =
            TallybookSurfaceStyle(
                color = TallybookTheme.colors.surface.sunken,
                contentColor = TallybookTheme.colors.foreground.secondary,
                shape = RoundedCornerShape(TallybookTheme.radii.radius10),
                border =
                    BorderStroke(
                        TallybookTheme.dimensions.fieldBorderWidth,
                        TallybookTheme.colors.border.subtle,
                    ),
            ),
        isPressed = isPressed,
    ) {
      Row(
          modifier =
              Modifier.padding(
                  horizontal = TallybookTheme.spacing.space14,
                  vertical = TallybookTheme.spacing.space8,
              ),
          horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space6),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        DatePickerIcon(icon = Lucide.CalendarDays)
        TallybookText(
            text = selectedLabel,
            color = TallybookTheme.colors.foreground.secondary,
            style = TallybookTheme.typography.bodyEmphasis,
        )
        DatePickerIcon(icon = Lucide.ChevronDown)
      }
    }
    if (popupVisibility.currentState || popupVisibility.targetState) {
      Popup(
          popupPositionProvider = popupPositionProvider,
          onDismissRequest = {
            viewportOverlayState?.hideScrim(viewportOverlayOwner)
            isOpen = false
          },
          properties = PopupProperties(focusable = true),
      ) {
        AnimatedVisibility(
            visibleState = popupVisibility,
            enter =
                fadeIn(animationSpec = datePickerEnterAnimationSpec) +
                    scaleIn(
                        animationSpec = datePickerEnterAnimationSpec,
                        initialScale = DATE_PICKER_INITIAL_SCALE,
                    ),
            exit =
                fadeOut(animationSpec = datePickerExitAnimationSpec) +
                    scaleOut(
                        animationSpec = datePickerExitAnimationSpec,
                        targetScale = DATE_PICKER_INITIAL_SCALE,
                    ),
        ) {
          TallybookDateCalendar(
              visibleMonth = visibleMonth,
              selectedDate = value,
              maxDate = maxDate,
              firstDayOfWeek = firstDayOfWeek,
              monthNames = monthNames,
              weekdayNames = weekdayNames,
              isYearSelectorOpen = isYearSelectorOpen,
              onPreviousMonth = { visibleMonth = visibleMonth.previous() },
              onNextMonth = { visibleMonth = visibleMonth.next() },
              onYearSelectorClick = { isYearSelectorOpen = true },
              onYearSelected = { year ->
                visibleMonth = YearMonth(year, visibleMonth.month)
                isYearSelectorOpen = false
              },
              onDateSelected = {
                onValueChange(it)
                viewportOverlayState?.hideScrim(viewportOverlayOwner)
                isOpen = false
              },
          )
        }
      }
    }
  }
}

@Composable
private fun TallybookDateCalendar(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    maxDate: LocalDate?,
    firstDayOfWeek: DayOfWeek,
    monthNames: List<String>,
    weekdayNames: List<String>,
    isYearSelectorOpen: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onYearSelectorClick: () -> Unit,
    onYearSelected: (Int) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
  val canNavigateForward = maxDate == null || visibleMonth < YearMonth(maxDate.year, maxDate.month)
  TallybookSurface(
      modifier = Modifier.widthIn(max = TallybookTheme.dimensions.datePickerMaximumWidth),
      style =
          TallybookSurfaceStyle(
              color = TallybookTheme.colors.surface.sunken,
              contentColor = TallybookTheme.colors.foreground.primary,
              shape = RoundedCornerShape(TallybookTheme.radii.radius12),
              border =
                  BorderStroke(
                      TallybookTheme.dimensions.fieldBorderWidth,
                      TallybookTheme.colors.border.subtle,
                  ),
          ),
  ) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TallybookIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = onPreviousMonth,
            icon = Lucide.ChevronLeft,
            contentDescription = stringResource(Res.string.calendar_previous_month),
        )
        Row(
            modifier =
                Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onYearSelectorClick,
                    )
                    .semantics { contentDescription = "${visibleMonth.year}" }
                    .padding(TallybookTheme.spacing.space8),
            horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          TallybookText(
              text = "${monthNames[visibleMonth.month.ordinal]} ${visibleMonth.year}",
              color = TallybookTheme.colors.foreground.primary,
              style = TallybookTheme.typography.title,
          )
          DatePickerIcon(
              icon = Lucide.ChevronDown,
              color = TallybookTheme.colors.foreground.tertiary,
          )
        }
        TallybookIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onNextMonth,
            icon = Lucide.ChevronRight,
            contentDescription = stringResource(Res.string.calendar_next_month),
            enabled = canNavigateForward,
        )
      }
      if (isYearSelectorOpen) {
        TallybookYearSelector(
            selectedYear = visibleMonth.year,
            years = selectableYears(selectedDate, maxDate),
            onYearSelected = onYearSelected,
        )
      } else {
        Row(modifier = Modifier.fillMaxWidth()) {
          weekdayNames.rotated(firstDayOfWeek).forEach { weekday ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              TallybookText(
                  text = weekday,
                  color = TallybookTheme.colors.foreground.faint,
                  style = TallybookTheme.typography.caption,
              )
            }
          }
        }
        monthGridDates(visibleMonth, firstDayOfWeek).chunked(DAYS_IN_WEEK).forEach { week ->
          Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { date ->
              val isEnabled = maxDate == null || date <= maxDate
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TallybookCalendarDay(
                    date = date,
                    isCurrentMonth = date.month == visibleMonth.month,
                    isSelected = date == selectedDate,
                    enabled = isEnabled,
                    monthName = monthNames[date.month.ordinal],
                    onClick = { onDateSelected(date) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TallybookYearSelector(
    selectedYear: Int,
    years: IntRange,
    onYearSelected: (Int) -> Unit,
) {
  years.toList().reversed().chunked(YEARS_PER_ROW).forEach { row ->
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
    ) {
      row.forEach { year ->
        val isSelected = year == selectedYear
        TallybookSurface(
            modifier =
                Modifier.weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { onYearSelected(year) },
                    ),
            style =
                TallybookSurfaceStyle(
                    color =
                        if (isSelected) TallybookTheme.colors.brand.primary
                        else TallybookTheme.colors.surface.sunken,
                    contentColor =
                        if (isSelected) TallybookTheme.colors.brand.onPrimary
                        else TallybookTheme.colors.foreground.primary,
                    shape = RoundedCornerShape(TallybookTheme.radii.radius10),
                    border = null,
                    contentAlignment = Alignment.Center,
                ),
        ) {
          TallybookText(
              text = year.toString(),
              modifier = Modifier.padding(TallybookTheme.spacing.space12),
              color =
                  if (isSelected) TallybookTheme.colors.brand.onPrimary
                  else TallybookTheme.colors.foreground.primary,
              style = TallybookTheme.typography.bodyEmphasis,
          )
        }
      }
    }
  }
}

@Composable
private fun TallybookCalendarDay(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    monthName: String,
    onClick: () -> Unit,
) {
  val colors = TallybookTheme.colors
  val contentColor =
      when {
        !enabled -> colors.foreground.disabled
        !isCurrentMonth -> colors.foreground.faint
        isSelected -> colors.brand.onPrimary
        else -> colors.foreground.primary
      }
  val dateDescription =
      stringResource(
          Res.string.calendar_date_content_description,
          date.day,
          monthName,
          date.year,
      )
  Box(
      modifier =
          Modifier.size(TallybookTheme.dimensions.datePickerDaySize)
              .then(
                  if (isSelected) {
                    Modifier.clip(RoundedCornerShape(TallybookTheme.radii.radius10))
                        .background(colors.brand.primary)
                  } else {
                    Modifier
                  }
              )
              .clickable(
                  interactionSource = null,
                  indication = null,
                  enabled = enabled,
                  role = Role.Button,
                  onClick = onClick,
              )
              .semantics { contentDescription = dateDescription },
      contentAlignment = Alignment.Center,
  ) {
    TallybookText(
        text = date.day.toString(),
        color = contentColor,
        style = TallybookTheme.typography.bodyEmphasis,
    )
  }
}

@Composable
private fun DatePickerIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = TallybookTheme.colors.foreground.faint,
) {
  Image(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.datePickerChipIconSize),
      colorFilter = ColorFilter.tint(color),
  )
}

internal fun monthGridDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
  val firstDate = month.firstDay
  val leadingDays =
      (firstDate.dayOfWeek.ordinal - firstDayOfWeek.ordinal + DAYS_IN_WEEK) % DAYS_IN_WEEK
  val gridDays = leadingDays + month.numberOfDays
  val completeWeeks = (gridDays + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK
  return List(completeWeeks * DAYS_IN_WEEK) { firstDate.plus(DatePeriod(days = it - leadingDays)) }
}

internal fun selectableYears(selectedDate: LocalDate, maxDate: LocalDate?): IntRange {
  val maximum = maxDate?.year ?: selectedDate.year + YEARS_AHEAD_WITHOUT_LIMIT
  return FIRST_SELECTABLE_YEAR..maximum
}

internal class TallybookDatePickerPopupPositionProvider(
    private val verticalOffset: Int,
    private val edgeMargin: Int,
) : PopupPositionProvider {
  override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize,
  ): IntOffset {
    val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
    val maxX = maxOf(edgeMargin, windowSize.width - popupContentSize.width - edgeMargin)
    val availableBelow = windowSize.height - anchorBounds.bottom - verticalOffset
    val availableAbove = anchorBounds.top - verticalOffset
    val maxY = maxOf(edgeMargin, windowSize.height - popupContentSize.height - edgeMargin)
    val y =
        when {
          availableBelow >= popupContentSize.height -> anchorBounds.bottom + verticalOffset
          availableAbove >= popupContentSize.height ->
              anchorBounds.top - verticalOffset - popupContentSize.height
          availableAbove > availableBelow ->
              (anchorBounds.top - verticalOffset - popupContentSize.height).coerceIn(
                  edgeMargin,
                  maxY,
              )
          else -> (anchorBounds.bottom + verticalOffset).coerceIn(edgeMargin, maxY)
        }
    return IntOffset(centeredX.coerceIn(edgeMargin, maxX), y)
  }
}

private fun YearMonth.previous(): YearMonth =
    if (month == Month.JANUARY) YearMonth(year - 1, Month.DECEMBER)
    else YearMonth(year, month.ordinal)

private fun YearMonth.next(): YearMonth =
    if (month == Month.DECEMBER) YearMonth(year + 1, Month.JANUARY)
    else YearMonth(year, month.ordinal + 2)

private fun List<String>.rotated(firstDayOfWeek: DayOfWeek): List<String> =
    drop(firstDayOfWeek.ordinal) + take(firstDayOfWeek.ordinal)

private const val DAYS_IN_WEEK = 7
private const val FIRST_SELECTABLE_YEAR = 2011
private const val YEARS_AHEAD_WITHOUT_LIMIT = 1
private const val YEARS_PER_ROW = 4
private const val DATE_PICKER_ENTER_DURATION_MILLIS = 200
private const val DATE_PICKER_EXIT_DURATION_MILLIS = 150
private const val DATE_PICKER_INITIAL_SCALE = 0.985f

private val datePickerEnterAnimationSpec =
    tween<Float>(
        durationMillis = DATE_PICKER_ENTER_DURATION_MILLIS,
        easing = LinearOutSlowInEasing,
    )

private val datePickerExitAnimationSpec =
    tween<Float>(
        durationMillis = DATE_PICKER_EXIT_DURATION_MILLIS,
        easing = FastOutLinearInEasing,
    )
