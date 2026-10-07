package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.TallybookTextField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.collapsed
import tallybook.shared.generated.resources.description_optional
import tallybook.shared.generated.resources.expanded
import tallybook.shared.generated.resources.more_fields

@Composable
internal fun MovimientoDetailsToggle(
    detailsExpanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
  val descriptionState =
      stringResource(if (detailsExpanded) Res.string.expanded else Res.string.collapsed)
  val detailsChevronRotation by
      animateFloatAsState(
          targetValue = if (detailsExpanded) 180f else 0f,
          animationSpec = tween(durationMillis = TallybookTheme.motion.sheetDownMillis),
          label = "movement details chevron",
      )

  Column(modifier = Modifier.fillMaxWidth()) {
    TallybookTextButton(
        onClick = onToggle,
        modifier =
            Modifier.align(Alignment.CenterHorizontally).semantics {
              stateDescription = descriptionState
            },
        enabled = enabled,
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
                Modifier.size(TallybookTheme.dimensions.datePickerChipIconSize).graphicsLayer {
                  rotationZ = detailsChevronRotation
                },
            colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
        )
      }
    }
  }
}

@Composable
internal fun MovimientoDescriptionField(
    uiState: MovimientoFormState,
    onValueChange: (String) -> Unit,
    inputModifier: Modifier,
    enabled: Boolean,
) {
  val focusManager = LocalFocusManager.current
  TallybookTextField(
      value = uiState.descripcion,
      onValueChange = { onValueChange(it.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) },
      modifier = Modifier.fillMaxWidth().then(inputModifier),
      enabled = enabled,
      label = stringResource(Res.string.description_optional),
      message =
          TallybookFieldMessage.Supporting(
              "${uiState.descripcion.length}/$MOVIMIENTO_DESCRIPTION_MAX_LENGTH"
          ),
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
      labelContent = { TallybookInputLabel(stringResource(Res.string.description_optional)) },
  )
}
