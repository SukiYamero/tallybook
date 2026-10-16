package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookScreenHeader
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesContent
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategorySheetRequest
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.categories_custom_empty
import tallybook.shared.generated.resources.categories_load_error
import tallybook.shared.generated.resources.categories_search_empty
import tallybook.shared.generated.resources.categories_section_custom
import tallybook.shared.generated.resources.categories_section_predefined
import tallybook.shared.generated.resources.categories_title
import tallybook.shared.generated.resources.category_new_title
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
) {
  val onOpen: (String) -> Unit = { id -> actions.onOpenSheet(CategorySheetRequest.Open(id)) }
  TallybookScreenFrame(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      CategoriesTopBar(
          onBack = onBack,
          onNewCategory = { actions.onOpenSheet(CategorySheetRequest.New(uiState.query)) },
      )
      TallybookSearchField(
          query = uiState.query,
          onQueryChange = actions::onQueryChanged,
          placeholder = stringResource(Res.string.search_categories),
          clearContentDescription = stringResource(Res.string.search_clear_query),
          modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space32),
          onClear = actions::onClearQuery,
      )
      LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(vertical = TallybookTheme.spacing.space16),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
      ) {
        when (val content = uiState.content) {
          CategoriesContent.Loading ->
              item(key = "loading") {
                TallybookLoadingState(
                    contentDescription = stringResource(Res.string.loading),
                    modifier =
                        Modifier.defaultMinSize(
                            minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                        ),
                )
              }
          CategoriesContent.Failed -> item(key = "failed") { CategoriesFailure(actions::onRetry) }
          is CategoriesContent.Browse -> browse(content, onOpen)
          is CategoriesContent.Search -> search(content, onOpen)
        }
      }
    }
  }
  CategorySheetHost(uiState.sheet, actions)
}

private fun LazyListScope.browse(content: CategoriesContent.Browse, onOpen: (String) -> Unit) {
  item(key = "heading-custom") {
    TallybookSectionHeading(stringResource(Res.string.categories_section_custom))
  }
  if (content.custom.isEmpty()) {
    item(key = "custom-empty") { SecondaryText(stringResource(Res.string.categories_custom_empty)) }
  } else {
    item(key = "custom") { CategoryChipBlock(content.custom, onOpen) }
  }
  if (content.families.isNotEmpty()) {
    item(key = "heading-predefined") {
      TallybookSectionHeading(
          stringResource(Res.string.categories_section_predefined),
          modifier = Modifier.padding(top = TallybookTheme.spacing.space8),
      )
    }
    items(content.families, key = { "family-${it.first().categoria.id}" }) { family ->
      CategoryChipBlock(family, onOpen)
    }
  }
}

private fun LazyListScope.search(content: CategoriesContent.Search, onOpen: (String) -> Unit) {
  if (content.matches.isEmpty()) {
    item(key = "search-empty") {
      SecondaryText(stringResource(Res.string.categories_search_empty, content.query))
    }
  } else {
    item(key = "matches") { CategoryChipBlock(content.matches, onOpen) }
  }
}

@Composable
private fun CategoriesTopBar(onBack: () -> Unit, onNewCategory: () -> Unit) {
  TallybookScreenHeader(
      title = stringResource(Res.string.categories_title),
      modifier = Modifier.padding(top = TallybookTheme.spacing.space20),
      navigation = {
        TallybookIconButton(
            onClick = onBack,
            icon = Lucide.ChevronLeft,
            contentDescription = stringResource(Res.string.back),
            style = TallybookIconButtonStyle.Ghost,
        )
      },
      actions = {
        TallybookTextButton(onClick = onNewCategory) {
          Image(
              imageVector = Lucide.Plus,
              contentDescription = null,
              modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
              colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
          )
          TallybookText(stringResource(Res.string.category_new_title))
        }
      },
  )
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
