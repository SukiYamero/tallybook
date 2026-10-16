package com.kurobello.tallybook.feature.tags.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonDefaults
import com.kurobello.tallybook.core.ui.component.TallybookIconButtonStyle
import com.kurobello.tallybook.core.ui.component.TallybookLoadingState
import com.kurobello.tallybook.core.ui.component.TallybookScreenHeader
import com.kurobello.tallybook.core.ui.component.TallybookSearchEmptyState
import com.kurobello.tallybook.core.ui.component.TallybookSearchField
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.tag.EtiquetaChip
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.tags.viewmodel.TagRow
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheetRequest
import com.kurobello.tallybook.feature.tags.viewmodel.TagsActions
import com.kurobello.tallybook.feature.tags.viewmodel.TagsContent
import com.kurobello.tallybook.feature.tags.viewmodel.TagsUiState
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.search_clear_query
import tallybook.shared.generated.resources.search_tags
import tallybook.shared.generated.resources.tag_new_title
import tallybook.shared.generated.resources.tags_empty
import tallybook.shared.generated.resources.tags_load_error
import tallybook.shared.generated.resources.tags_search_empty
import tallybook.shared.generated.resources.tags_title

@Composable
internal fun TagsScreen(
    uiState: TagsUiState,
    actions: TagsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val onOpen: (String) -> Unit = { id -> actions.onOpenSheet(TagSheetRequest.Open(id)) }
  TallybookScreenFrame(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      TagsTopBar(
          onBack = onBack,
          onNewTag = { actions.onOpenSheet(TagSheetRequest.New(uiState.query)) },
      )
      TallybookSearchField(
          query = uiState.query,
          onQueryChange = actions::onQueryChanged,
          placeholder = stringResource(Res.string.search_tags),
          clearContentDescription = stringResource(Res.string.search_clear_query),
          modifier = Modifier.fillMaxWidth().padding(top = TallybookTheme.spacing.space32),
          onClear = { actions.onQueryChanged("") },
      )
      LazyColumn(
          // The outer padding keeps scrolled chips from clipping flush under the search field; the
          // content padding completes the resting gap.
          modifier =
              Modifier.weight(1f).fillMaxWidth().padding(top = TallybookTheme.spacing.space8),
          contentPadding =
              PaddingValues(
                  top = TallybookTheme.spacing.space8,
                  bottom = TallybookTheme.spacing.space16,
              ),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
      ) {
        tagsContent(uiState.content, actions::onRetry, onOpen)
      }
    }
  }
  TagSheetHost(uiState.sheet, actions)
}

private fun LazyListScope.tagsContent(
    content: TagsContent,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
) {
  when (content) {
    TagsContent.Loading ->
        item(key = "loading") {
          TallybookLoadingState(
              contentDescription = stringResource(Res.string.loading),
              modifier =
                  Modifier.defaultMinSize(
                      minHeight = TallybookTheme.dimensions.containedLoadingMinHeight
                  ),
          )
        }
    TagsContent.Failed -> item(key = "failed") { TagsFailure(onRetry) }
    is TagsContent.Browse ->
        item(key = "browse") {
          if (content.rows.isEmpty()) SecondaryText(stringResource(Res.string.tags_empty))
          else TagChipBlock(content.rows, onOpen)
        }
    is TagsContent.Search ->
        item(key = "search") {
          if (content.matches.isEmpty()) {
            TallybookSearchEmptyState(
                message = stringResource(Res.string.tags_search_empty, content.query),
                modifier = Modifier.fillMaxWidth(),
            )
          } else {
            TagChipBlock(content.matches, onOpen)
          }
        }
  }
}

@Composable
private fun TagChipBlock(rows: List<TagRow>, onOpen: (String) -> Unit) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    rows.forEach { row ->
      key(row.etiqueta.id) {
        EtiquetaChip(
            etiqueta = row.etiqueta,
            selected = false,
            onClick = { onOpen(row.etiqueta.id) },
            count = row.count,
        )
      }
    }
  }
}

@Composable
private fun TagsTopBar(onBack: () -> Unit, onNewTag: () -> Unit) {
  TallybookScreenHeader(
      title = stringResource(Res.string.tags_title),
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
        TallybookIconButton(
            onClick = onNewTag,
            icon = Lucide.Plus,
            contentDescription = stringResource(Res.string.tag_new_title),
            colors =
                TallybookIconButtonDefaults.colors(TallybookIconButtonStyle.Ghost)
                    .copy(content = TallybookTheme.colors.brand.primary),
            iconSize = TallybookTheme.dimensions.iconLarge,
        )
      },
  )
}

@Composable
private fun TagsFailure(onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    SecondaryText(stringResource(Res.string.tags_load_error))
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

@Suppress("TooManyFunctions")
private object PreviewTagsActions : TagsActions {
  override fun onQueryChanged(query: String) = Unit

  override fun onRetry() = Unit

  override fun onOpenSheet(request: TagSheetRequest) = false

  override fun onNombreChanged(value: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onSaveSheet() = Unit

  override fun onViewMovimientos() = Unit

  override fun onOpenDuplicate() = Unit

  override fun onRequestDelete() = Unit

  override fun onConfirmDelete() = Unit

  override fun onCancelDelete() = Unit

  override fun onRequestCloseSheet() = Unit

  override fun onSheetHidden() = Unit
}

private val previewCreatedAt = Instant.parse("2026-01-01T00:00:00Z")
private const val PREVIEW_EMPRESA_COUNT = 12L
private const val PREVIEW_VIAJE_COUNT = 3L

private val previewState =
    TagsUiState(
        content =
            TagsContent.Browse(
                listOf(
                    TagRow(
                        Etiqueta("t_empresa", "Empresa", previewCreatedAt),
                        PREVIEW_EMPRESA_COUNT,
                    ),
                    TagRow(Etiqueta("t_regalos", "Regalos", previewCreatedAt), 0L),
                    TagRow(
                        Etiqueta("t_viaje", "Viaje Chile", previewCreatedAt),
                        PREVIEW_VIAJE_COUNT,
                    ),
                )
            )
    )

@Preview
@Composable
internal fun TagsScreenLightPreview() {
  TallybookTheme(darkTheme = false) { TagsScreen(previewState, PreviewTagsActions, onBack = {}) }
}

@Preview
@Composable
internal fun TagsScreenDarkPreview() {
  TallybookTheme(darkTheme = true) { TagsScreen(previewState, PreviewTagsActions, onBack = {}) }
}
