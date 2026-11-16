package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Repeat
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookScreenHeader
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesContent
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesUiState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.recurrentes_empty_body
import tallybook.shared.generated.resources.recurrentes_empty_title
import tallybook.shared.generated.resources.recurrentes_load_error
import tallybook.shared.generated.resources.recurrentes_pending_heading
import tallybook.shared.generated.resources.recurrentes_scheduled_heading
import tallybook.shared.generated.resources.recurrentes_title
import tallybook.shared.generated.resources.retry

@Composable
internal fun RecurrentesScreen(
    uiState: RecurrentesUiState,
    actions: RecurrentesActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
  TallybookScreenFrame(modifier = modifier) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space10),
    ) {
      item(key = "header") {
        RecurrentesTopBar(
            onBack,
            Modifier.padding(
                top = TallybookTheme.spacing.space20,
                bottom = TallybookTheme.spacing.space22,
            ),
        )
      }
      recurrentesContent(uiState.content, actions)
    }
  }
  RecurrentesSheetHost(uiState, actions)
}

private fun LazyListScope.recurrentesContent(
    content: RecurrentesContent,
    actions: RecurrentesActions,
) {
  when (content) {
    RecurrentesContent.Loading ->
        item(key = "loading") {
          TallybookLoadingState(
              contentDescription = stringResource(Res.string.loading),
              modifier =
                  Modifier.defaultMinSize(
                      minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                  ),
          )
        }
    RecurrentesContent.Failed -> item(key = "failed") { RecurrentesFailure(actions::onRetry) }
    is RecurrentesContent.Ready ->
        if (content.isEmpty) item(key = "empty") { RecurrentesEmpty() }
        else recurrentesLists(content, actions)
  }
}

private fun LazyListScope.recurrentesLists(
    content: RecurrentesContent.Ready,
    actions: RecurrentesActions,
) {
  if (content.pending.isNotEmpty()) {
    item(key = "pending-heading") {
      SectionHeading(
          pluralStringResource(
              Res.plurals.recurrentes_pending_heading,
              content.pending.size,
              content.pending.size,
          )
      )
    }
    items(content.pending, key = { "pending-${it.recurrencia.id}-${it.dueDate}" }) { occurrence ->
      PendingOccurrenceRow(
          occurrence = occurrence,
          categoria = content.categoriasById[occurrence.recurrencia.categoriaId],
          onOpen = { actions.onOpenOccurrence(it) },
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
  item(key = "scheduled-heading") {
    SectionHeading(
        stringResource(Res.string.recurrentes_scheduled_heading),
        Modifier.padding(
            top =
                if (content.pending.isEmpty()) TallybookTheme.spacing.space0
                else TallybookTheme.spacing.space14
        ),
    )
  }
  items(content.scheduled, key = { "scheduled-${it.id}" }) { recurrencia ->
    ScheduledRecurrenciaRow(
        recurrencia = recurrencia,
        categoria = content.categoriasById[recurrencia.categoriaId],
        onOpen = { actions.onOpenRecurrencia(it) },
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
  TallybookText(
      text = text,
      modifier = modifier.semantics { heading() },
      style = TallybookTheme.typography.title,
  )
}

@Composable
private fun RecurrentesTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
  TallybookScreenHeader(
      title = stringResource(Res.string.recurrentes_title),
      modifier = modifier,
      navigation = {
        TallybookIconButton(
            onClick = onBack,
            icon = Lucide.ChevronLeft,
            contentDescription = stringResource(Res.string.back),
            style = TallybookIconButtonStyle.Ghost,
        )
      },
  )
}

@Composable
private fun RecurrentesFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookText(
        stringResource(Res.string.recurrentes_load_error),
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.body,
    )
    TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun RecurrentesEmpty() {
  TallybookSurface(
      modifier = Modifier.fillMaxWidth(),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  shape = RoundedCornerShape(TallybookTheme.radii.radius26),
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      ),
              ),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = TallybookTheme.spacing.space24,
                    vertical = TallybookTheme.spacing.space48,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
          modifier =
              Modifier.size(TallybookTheme.dimensions.emptyStateIconContainer)
                  .background(TallybookTheme.colors.surface.raised, CircleShape),
          contentAlignment = Alignment.Center,
      ) {
        Image(
            imageVector = Lucide.Repeat,
            contentDescription = null,
            modifier = Modifier.size(TallybookTheme.dimensions.emptyStateIcon),
            colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
        )
      }
      TallybookText(
          text = stringResource(Res.string.recurrentes_empty_title),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space14).semantics { heading() },
          style = TallybookTheme.typography.title.copy(textAlign = TextAlign.Center),
      )
      TallybookText(
          text = stringResource(Res.string.recurrentes_empty_body),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space6),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
      )
    }
  }
}
