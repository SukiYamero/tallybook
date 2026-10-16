package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookSurfaceDefaults
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditorRequest
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryRow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.categories_custom_empty
import tallybook.shared.generated.resources.categories_load_error
import tallybook.shared.generated.resources.categories_search_empty
import tallybook.shared.generated.resources.categories_section_custom
import tallybook.shared.generated.resources.categories_section_predefined
import tallybook.shared.generated.resources.categories_title
import tallybook.shared.generated.resources.category_edit_named
import tallybook.shared.generated.resources.category_hidden_marker
import tallybook.shared.generated.resources.category_hide_named
import tallybook.shared.generated.resources.category_movimiento_count
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_no_movimientos
import tallybook.shared.generated.resources.category_show_named
import tallybook.shared.generated.resources.category_view_movimientos
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.search_categories
import tallybook.shared.generated.resources.search_clear_query

@Composable
internal fun CategoriesScreen(
    uiState: CategoriesUiState,
    actions: CategoriesActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
  val items = remember(uiState.content, uiState.query) { uiState.listItems() }
  TallybookScreenFrame(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      CategoriesTopBar(
          onBack = onBack,
          onNewCategory = { actions.onOpenEditor(CategoryEditorRequest.New(uiState.query)) },
      )
      TallybookSearchField(
          query = uiState.query,
          onQueryChange = actions::onQueryChanged,
          placeholder = stringResource(Res.string.search_categories),
          clearContentDescription = stringResource(Res.string.search_clear_query),
          modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space12),
          onClear = actions::onClearQuery,
      )
      LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(vertical = TallybookTheme.spacing.space16),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      ) {
        items(items, key = CategoriesListItem::key) { item ->
          CategoriesItem(item, uiState, actions)
        }
      }
    }
  }
  CategoryEditorSheet(uiState.editor, actions)
}

@Composable
private fun CategoriesTopBar(onBack: () -> Unit, onNewCategory: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookIconButton(
        onClick = onBack,
        icon = Lucide.ChevronLeft,
        contentDescription = stringResource(Res.string.back),
    )
    TallybookText(
        stringResource(Res.string.categories_title),
        modifier = Modifier.weight(1f).semantics { heading() },
        style = TallybookTheme.typography.headline,
    )
    TallybookTextButton(onClick = onNewCategory) {
      Image(
          imageVector = Lucide.Plus,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
      TallybookText(stringResource(Res.string.category_new_title))
    }
  }
}

@Composable
private fun CategoriesItem(
    item: CategoriesListItem,
    uiState: CategoriesUiState,
    actions: CategoriesActions,
) {
  when (item) {
    CategoriesListItem.Loading ->
        TallybookLoadingState(
            contentDescription = stringResource(Res.string.loading),
            modifier =
                Modifier.defaultMinSize(
                    minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                ),
        )
    CategoriesListItem.Failed -> CategoriesFailure(actions::onRetry)
    is CategoriesListItem.SearchEmpty ->
        SecondaryText(stringResource(Res.string.categories_search_empty, item.query))
    is CategoriesListItem.Heading ->
        TallybookSectionHeading(
            stringResource(
                when (item.section) {
                  CategoriesSection.CUSTOM -> Res.string.categories_section_custom
                  CategoriesSection.PREDEFINED -> Res.string.categories_section_predefined
                }
            ),
            modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
        )
    CategoriesListItem.CustomEmpty ->
        SecondaryText(stringResource(Res.string.categories_custom_empty))
    is CategoriesListItem.Row ->
        CategoryListRow(
            row = item.row,
            highlighted = item.row.categoria.id == uiState.highlightedId,
            visibilityPending = item.row.categoria.id in uiState.pendingVisibility,
            actions = actions,
        )
  }
}

@Composable
private fun CategoriesFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    SecondaryText(stringResource(Res.string.categories_load_error))
    TallybookButton(onClick = onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun SecondaryText(text: String) {
  TallybookText(
      text,
      color = TallybookTheme.colors.foreground.secondary,
      style = TallybookTheme.typography.body,
  )
}

@Composable
private fun CategoryListRow(
    row: CategoryRow,
    highlighted: Boolean,
    visibilityPending: Boolean,
    actions: CategoriesActions,
) {
  val categoria = row.categoria
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  val card = TallybookSurfaceDefaults.cardStyle()
  TallybookSurface(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  start =
                      if (row.isChild) TallybookTheme.spacing.space24
                      else TallybookTheme.spacing.space0
              )
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClickLabel =
                      stringResource(Res.string.category_view_movimientos, categoria.nombre),
                  onClick = {
                    tapFeedback.trigger()
                    actions.onOpenCategory(categoria.id)
                  },
              ),
      style =
          if (highlighted)
              card.copy(
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.categoryOptionSelectedBorderWidth,
                          TallybookTheme.colors.border.ring,
                      )
              )
          else card,
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier =
            Modifier.defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
                .padding(
                    start = TallybookTheme.spacing.space12,
                    end = TallybookTheme.spacing.space4,
                    top = TallybookTheme.spacing.space8,
                    bottom = TallybookTheme.spacing.space8,
                ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CategoryAvatar(categoria)
      CategoryRowText(row, Modifier.weight(1f))
      CategoryRowAction(row, visibilityPending, actions)
    }
  }
}

@Composable
private fun CategoryRowText(row: CategoryRow, modifier: Modifier) {
  val categoria = row.categoria
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
  ) {
    TallybookText(categoria.nombre, style = TallybookTheme.typography.bodyEmphasis)
    val descripcion = categoria.descripcion
    if (row.isCustom && descripcion != null) {
      TallybookText(
          descripcion,
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
      TallybookText(
          if (row.count == 0L) stringResource(Res.string.category_no_movimientos)
          else
              pluralStringResource(
                  Res.plurals.category_movimiento_count,
                  row.count.toPluralQuantity(),
                  row.count,
              ),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.bodySmall,
      )
      if (categoria.oculta) {
        TallybookText(
            stringResource(Res.string.category_hidden_marker),
            color = TallybookTheme.colors.foreground.secondary,
            style = TallybookTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun CategoryRowAction(
    row: CategoryRow,
    visibilityPending: Boolean,
    actions: CategoriesActions,
) {
  val categoria = row.categoria
  when {
    row.isCustom ->
        TallybookIconButton(
            onClick = { actions.onOpenEditor(CategoryEditorRequest.Edit(categoria.id)) },
            icon = Lucide.Pencil,
            contentDescription = stringResource(Res.string.category_edit_named, categoria.nombre),
        )
    row.canChangeVisibility ->
        TallybookIconButton(
            onClick = { actions.onToggleHidden(categoria.id) },
            icon = if (categoria.oculta) Lucide.Eye else Lucide.EyeOff,
            contentDescription =
                stringResource(
                    if (categoria.oculta) Res.string.category_show_named
                    else Res.string.category_hide_named,
                    categoria.nombre,
                ),
            enabled = !visibilityPending,
        )
  }
}

/** Plural selection takes an Int; counts beyond it all read as "other". */
internal fun Long.toPluralQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
