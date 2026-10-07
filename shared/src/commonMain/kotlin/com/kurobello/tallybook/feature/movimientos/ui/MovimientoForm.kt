package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.captureScrollableInputViewport
import com.kurobello.tallybook.core.ui.component.centerFocusedInputIn
import com.kurobello.tallybook.core.ui.component.clearFocusOnUnconsumedTap
import com.kurobello.tallybook.core.ui.component.rememberScrollableInputViewport
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AmountInputError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.movimiento_categories_error
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.movimiento_save_error
import tallybook.shared.generated.resources.retry

internal data class MovimientoFormPresentation(
    val title: String,
    val submitLabel: String,
    val isSubmissionEnabled: Boolean = true,
)

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun MovimientoForm(
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    detailsExpanded: Boolean = false,
    onDetailsExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    startWithCategory: Boolean = false,
    onCategoryLayerReady: () -> Unit = {},
    presentation: MovimientoFormPresentation? = null,
    footerContent: (@Composable () -> Unit)? = null,
    interactionEnabled: Boolean = true,
) {
  val fieldFocus = remember { MovimientoFieldFocus() }
  val categoryEntry =
      rememberMovimientoCategoryEntry(
          uiState,
          actions,
          fieldFocus.amount,
          policy =
              MovimientoCategoryEntryPolicy(
                  startWithCategory,
                  presentation == null,
                  interactionEnabled,
              ),
      )
  NavigationBackHandler(
      state = rememberNavigationEventState(NavigationEventInfo.None),
      isBackEnabled =
          presentation != null && categoryEntry.blocksValidationFocus && interactionEnabled,
      onBackCompleted = categoryEntry.onDismiss,
  )
  val detailsScrollState = rememberScrollState()
  val formScrollState = rememberScrollState()
  val formViewport = rememberScrollableInputViewport()
  val density = LocalDensity.current
  val restingFormHeight = rememberRestingFormHeight()
  val imeBottom = WindowInsets.ime.getBottom(density)
  val isImeVisible = imeBottom > 0
  ResetFormScrollAfterKeyboardDismissal(isImeVisible, formScrollState)
  val imeScrollPadding = with(density) { imeBottom.toDp() }
  val formMaximumHeight = formMaximumHeight(isImeVisible, restingFormHeight.value)
  val formItemSpacing = TallybookTheme.spacing.space16
  val title = presentation?.title ?: stringResource(Res.string.add_movimiento_pane_title)
  MovimientoValidationFocus(uiState, categoryEntry, fieldFocus)

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .clearFocusOnUnconsumedTap(),
  ) {
    TallybookSheetTitle(
        text = title,
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space16))
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = formMaximumHeight)
                .verticalScroll(formScrollState)
                .captureScrollableInputViewport(formViewport)
                .captureRestingHeight(isImeVisible, restingFormHeight),
    ) {
      Column(
          modifier = Modifier.fillMaxWidth().paddingForImeScroll(isImeVisible, imeScrollPadding),
          verticalArrangement = Arrangement.spacedBy(formItemSpacing),
      ) {
        MovimientoPrimaryFields(
            uiState = uiState,
            interactionEnabled = interactionEnabled,
            actions = actions,
            categoryEntry = categoryEntry,
            fieldFocus =
                fieldFocus.copy(
                    amountModifier = Modifier.centerFocusedInputIn(formScrollState, formViewport)
                ),
        )
        Spacer(modifier = Modifier.height(TallybookTheme.spacing.space8))
        MovimientoDetailsToggle(detailsExpanded, interactionEnabled && !uiState.isSubmitting) {
          onDetailsExpandedChange(!detailsExpanded)
        }
        AnimatedVisibility(
            visible = detailsExpanded,
            enter =
                fadeIn(animationSpec = tween(durationMillis = TallybookTheme.motion.fadeMillis)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = TallybookTheme.motion.fadeMillis),
                        initialOffsetY = { height -> height / 8 },
                    ),
            exit =
                fadeOut(
                    animationSpec = tween(durationMillis = TallybookTheme.motion.sheetDownMillis)
                ) +
                    slideOutVertically(
                        animationSpec =
                            tween(durationMillis = TallybookTheme.motion.sheetDownMillis),
                        targetOffsetY = { height -> height / 8 },
                    ),
        ) {
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .then(
                          if (isImeVisible) {
                            Modifier
                          } else {
                            Modifier.heightIn(
                                    max = TallybookTheme.dimensions.movimientoDetailsMaximumHeight
                                )
                                .verticalScroll(detailsScrollState)
                          }
                      )
                      .testTag("movimiento_details"),
          ) {
            MovimientoDescriptionField(
                uiState,
                actions::onDescripcionChanged,
                Modifier.centerFocusedInputIn(formScrollState, formViewport),
                interactionEnabled && !uiState.isSubmitting,
            )
          }
        }
        MovimientoFormErrorMessage(
            error = uiState.error,
            onRetry = actions::onRetryLoad,
            interactionEnabled = interactionEnabled,
        )
        if (uiState.isLoading) {
          TallybookProgressIndicator(
              contentDescription = stringResource(Res.string.loading),
              modifier = Modifier.align(Alignment.CenterHorizontally),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space18))
    if (isImeVisible) {
      Spacer(
          Modifier.fillMaxWidth()
              .height(TallybookTheme.dimensions.minimumTouchTarget + TallybookTheme.spacing.space8)
      )
    } else {
      MovimientoFormSubmit(uiState, actions::onSubmit, presentation, interactionEnabled)
      footerContent?.invoke()
    }
  }
  CategoryPickerSheet(
      visible = interactionEnabled && categoryEntry.isPickerVisible,
      categorias = uiState.categorias,
      selectedId = uiState.selectedCategoriaId,
      onSelect = {
        onCategoryLayerReady()
        categoryEntry.onSelect(it)
      },
      onDismiss = {
        onCategoryLayerReady()
        categoryEntry.onDismiss()
      },
      onShown = onCategoryLayerReady,
      onHidden = categoryEntry.onHidden,
      onBack = categoryEntry.onDismiss.takeIf { presentation != null },
      allowSubcategories = false,
      headerContent = {
        Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20)) {
          TallybookSheetTitle(
              text = title,
          )
          MovimientoTypeSelector(
              selected = uiState.tipo,
              enabled = interactionEnabled && !uiState.isSubmitting,
              onSelected = actions::onTipoChanged,
          )
          MovimientoCategoryGuidance(uiState.tipo)
        }
      },
      paneTitle = title,
  )
}

@Composable
private fun MovimientoValidationFocus(
    uiState: MovimientoFormState,
    categoryEntry: MovimientoCategoryEntry,
    fieldFocus: MovimientoFieldFocus,
) {
  LaunchedEffect(uiState.submitAttempted) {
    when {
      categoryEntry.blocksValidationFocus -> Unit
      uiState.isCategoriaMissing -> {
        fieldFocus.category.requestFocus()
      }
      uiState.amountError != null -> {
        fieldFocus.amount.requestFocus()
      }
    }
  }
}

@Composable
private fun rememberRestingFormHeight(): MutableState<Dp?> = remember { mutableStateOf(null) }

private fun formMaximumHeight(isImeVisible: Boolean, restingHeight: Dp?): Dp =
    if (isImeVisible) restingHeight ?: TallybookTheme.dimensions.movimientoFormMaximumHeight
    else TallybookTheme.dimensions.movimientoFormMaximumHeight

@Composable
private fun ResetFormScrollAfterKeyboardDismissal(isImeVisible: Boolean, state: ScrollState) {
  LaunchedEffect(isImeVisible) { if (!isImeVisible) state.scrollTo(0) }
}

@Composable
private fun Modifier.captureRestingHeight(
    isImeVisible: Boolean,
    restingHeight: MutableState<Dp?>,
): Modifier {
  val density = LocalDensity.current
  return onSizeChanged { size ->
    if (!isImeVisible) {
      restingHeight.value = with(density) { size.height.toDp() }
    }
  }
}

private fun Modifier.paddingForImeScroll(isImeVisible: Boolean, padding: Dp): Modifier =
    if (isImeVisible) padding(bottom = padding) else this

@Composable
internal fun AmountInputError.message(): String =
    when (this) {
      AmountInputError.EMPTY -> stringResource(Res.string.amount_required)
      AmountInputError.MALFORMED -> stringResource(Res.string.amount_malformed)
      AmountInputError.NOT_POSITIVE -> stringResource(Res.string.amount_not_positive)
      AmountInputError.TOO_MANY_DECIMALS -> stringResource(Res.string.amount_too_many_decimals)
      AmountInputError.OVERFLOW -> stringResource(Res.string.amount_overflow)
    }

@Composable
private fun MovimientoFormErrorMessage(
    error: MovimientoFormError?,
    onRetry: () -> Unit,
    interactionEnabled: Boolean,
) {
  when (error) {
    null -> Unit
    MovimientoFormError.PREFERENCES_UNAVAILABLE ->
        MovimientoFormRetryError(
            stringResource(Res.string.movimiento_preferences_error),
            onRetry,
            interactionEnabled,
        )
    MovimientoFormError.CATEGORIES_UNAVAILABLE ->
        MovimientoFormRetryError(
            stringResource(Res.string.movimiento_categories_error),
            onRetry,
            interactionEnabled,
        )
    MovimientoFormError.SAVE_FAILED ->
        TallybookText(
            text = stringResource(Res.string.movimiento_save_error),
            color = TallybookTheme.colors.status.danger.base,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = TallybookTheme.typography.body,
        )
  }
}

@Composable
private fun MovimientoFormRetryError(
    message: String,
    onRetry: () -> Unit,
    interactionEnabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = message,
        color = TallybookTheme.colors.status.danger.base,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = TallybookTheme.typography.body,
    )
    TallybookTextButton(onClick = onRetry, enabled = interactionEnabled) {
      TallybookText(stringResource(Res.string.retry))
    }
  }
}
