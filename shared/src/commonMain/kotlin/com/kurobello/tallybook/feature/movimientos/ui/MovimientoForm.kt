package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookProgressIndicator
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.component.captureScrollableInputViewport
import com.kurobello.tallybook.core.ui.component.centerFocusedInputIn
import com.kurobello.tallybook.core.ui.component.clearFocusOnUnconsumedTap
import com.kurobello.tallybook.core.ui.component.rememberScrollableInputViewport
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.AmountInputError
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.add_movimiento_pane_title
import tallybook.shared.generated.resources.amount_malformed
import tallybook.shared.generated.resources.amount_not_positive
import tallybook.shared.generated.resources.amount_overflow
import tallybook.shared.generated.resources.amount_required
import tallybook.shared.generated.resources.amount_too_many_decimals
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.collapsed
import tallybook.shared.generated.resources.description_optional
import tallybook.shared.generated.resources.expanded
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.more_fields
import tallybook.shared.generated.resources.movimiento_categories_error
import tallybook.shared.generated.resources.movimiento_preferences_error
import tallybook.shared.generated.resources.movimiento_save_error
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.save_expense
import tallybook.shared.generated.resources.save_income

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun MovimientoForm(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    detailsExpanded: Boolean = false,
    onDetailsExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    startWithCategory: Boolean = false,
    onCategoryLayerReady: () -> Unit = {},
) {
  val fieldFocus = remember { MovimientoFieldFocus() }
  val categoryEntry =
      rememberMovimientoCategoryEntry(uiState, actions, startWithCategory, fieldFocus.amount)
  val focusManager = LocalFocusManager.current
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
  val submitLabel = uiState.tipo.saveLabel()
  val selectedCategoria = uiState.categorias.firstOrNull { it.id == uiState.selectedCategoriaId }
  val descriptionState =
      stringResource(if (detailsExpanded) Res.string.expanded else Res.string.collapsed)
  val detailsChevronRotation by
      animateFloatAsState(
          targetValue = if (detailsExpanded) 180f else 0f,
          animationSpec = tween(durationMillis = TallybookTheme.motion.sheetDownMillis),
          label = "movement details chevron",
      )

  LaunchedEffect(
      uiState.submitAttempted,
      uiState.amountError,
      uiState.isCategoriaMissing,
  ) {
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

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .clearFocusOnUnconsumedTap(),
  ) {
    TallybookText(
        text = stringResource(Res.string.add_movimiento_pane_title),
        style = TallybookTheme.typography.title,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(TallybookTheme.spacing.space16))
    Column(
        modifier =
            Modifier.fillMaxWidth()
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
            actions = actions,
            categoryEntry = categoryEntry,
            fieldFocus = fieldFocus,
            amountModifier = Modifier.centerFocusedInputIn(formScrollState, formViewport),
        )
        Spacer(modifier = Modifier.height(TallybookTheme.spacing.space8))
        Column(modifier = Modifier.fillMaxWidth()) {
          TallybookTextButton(
              onClick = { onDetailsExpandedChange(!detailsExpanded) },
              modifier =
                  Modifier.align(Alignment.CenterHorizontally).semantics {
                    stateDescription = descriptionState
                  },
              enabled = !uiState.isSubmitting,
          ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              TallybookText(stringResource(Res.string.more_fields))
              androidx.compose.foundation.Image(
                  imageVector = Lucide.ChevronDown,
                  contentDescription = null,
                  modifier =
                      Modifier.size(TallybookTheme.dimensions.datePickerChipIconSize)
                          .graphicsLayer { rotationZ = detailsChevronRotation },
                  colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
              )
            }
          }
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
            TallybookTextField(
                value = uiState.descripcion,
                onValueChange = {
                  actions.onDescripcionChanged(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH))
                },
                modifier =
                    Modifier.fillMaxWidth().centerFocusedInputIn(formScrollState, formViewport),
                enabled = !uiState.isSubmitting,
                label = stringResource(Res.string.description_optional),
                message =
                    TallybookFieldMessage.Supporting(
                        "${uiState.descripcion.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH"
                    ),
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
          }
        }
        MovimientoFormErrorMessage(error = uiState.error, onRetry = actions::onRetryLoad)
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
          modifier =
              Modifier.fillMaxWidth()
                  .height(
                      TallybookTheme.dimensions.minimumTouchTarget + TallybookTheme.spacing.space8
                  )
      )
    } else {
      TallybookButton(
          onClick = actions::onSubmit,
          modifier =
              Modifier.fillMaxWidth().padding(bottom = TallybookTheme.spacing.space8).semantics {
                contentDescription = submitLabel
              },
          enabled =
              !uiState.isSubmitting &&
                  !uiState.isLoading &&
                  uiState.error == null &&
                  selectedCategoria != null,
      ) {
        Box(contentAlignment = Alignment.Center) {
          if (uiState.isSubmitting) {
            TallybookProgressIndicator(
                contentDescription = stringResource(Res.string.loading),
                modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
            )
          } else {
            TallybookText(text = submitLabel)
          }
        }
      }
    }
  }
  CategoryPickerSheet(
      visible = categoryEntry.isPickerVisible,
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
      allowSubcategories = false,
      headerContent = {
        Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20)) {
          TallybookText(
              text = stringResource(Res.string.add_movimiento_pane_title),
              style = TallybookTheme.typography.title,
              modifier = Modifier.semantics { heading() },
          )
          MovimientoTypeSelector(
              selected = uiState.tipo,
              enabled = !uiState.isSubmitting,
              onSelected = actions::onTipoChanged,
          )
          MovimientoCategoryGuidance(uiState.tipo)
        }
      },
  )
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
private fun MovimientoFormErrorMessage(error: MovimientoFormError?, onRetry: () -> Unit) {
  when (error) {
    null -> Unit
    MovimientoFormError.PREFERENCES_UNAVAILABLE ->
        MovimientoFormRetryError(stringResource(Res.string.movimiento_preferences_error), onRetry)
    MovimientoFormError.CATEGORIES_UNAVAILABLE ->
        MovimientoFormRetryError(stringResource(Res.string.movimiento_categories_error), onRetry)
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
private fun MovimientoFormRetryError(message: String, onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4)) {
    TallybookText(
        text = message,
        color = TallybookTheme.colors.status.danger.base,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = TallybookTheme.typography.body,
    )
    TallybookTextButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun TipoMovimiento.saveLabel(): String =
    when (this) {
      TipoMovimiento.GASTO -> stringResource(Res.string.save_expense)
      TipoMovimiento.INGRESO -> stringResource(Res.string.save_income)
    }
