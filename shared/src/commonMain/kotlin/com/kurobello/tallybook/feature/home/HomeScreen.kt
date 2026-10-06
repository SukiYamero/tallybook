package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.ui_catalog_title

@Composable
internal fun HomeRoute(
    onAddMovimiento: () -> Unit,
    bottomBarClearance: Dp,
    onOpenCatalog: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
  val state = rememberHomeRouteState(viewModel)
  HomeScreen(
      uiState = state.value,
      onAddMovimiento = onAddMovimiento,
      onRetry = viewModel::retry,
      onOpenCatalog = onOpenCatalog,
      modifier = modifier,
      bottomBarClearance = bottomBarClearance,
  )
}

@Suppress("LongParameterList")
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAddMovimiento: () -> Unit,
    onRetry: () -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomBarClearance: Dp = TallybookTheme.spacing.space0,
) {
  val presentation by rememberHomePresentation(uiState)
  val renderedState = presentation.state
  val contentListState = rememberLazyListState()
  // Loading/error placeholders must not consume the retained content scroll position.
  val pendingListState = remember { LazyListState() }
  TallybookScreenFrame(
      modifier = modifier,
      windowInsets =
          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
      bottomPaddingFloor = TallybookTheme.spacing.space0,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state =
            if (!renderedState.hasSnapshot || renderedState.error != null) pendingListState
            else contentListState,
        contentPadding =
            PaddingValues(bottom = bottomBarClearance + TallybookTheme.spacing.space16),
    ) {
      item { HomeHeader(modifier = Modifier.padding(bottom = TallybookTheme.spacing.space20)) }
      item { HomeSearch(modifier = Modifier.padding(bottom = TallybookTheme.spacing.space18)) }
      when {
        renderedState.error != null -> item { HomeError(onRetry) }
        !renderedState.hasSnapshot -> homeLoading(presentation.showLoading)
        renderedState.isEmpty -> item { HomeEmpty(onAddMovimiento) }
        else -> homePopulated(renderedState)
      }
      if (onOpenCatalog != null) {
        item {
          TallybookButton(
              onClick = onOpenCatalog,
              modifier = Modifier.fillMaxWidth(),
          ) {
            TallybookText(text = stringResource(Res.string.ui_catalog_title))
          }
        }
      }
    }
  }
}

@Preview
@Composable
internal fun HomeLoadingPreview() {
  TallybookTheme { HomeScreen(HomeUiState.loading(), {}, {}) }
}

@Preview
@Composable
internal fun HomeEmptyPreview() {
  TallybookTheme {
    HomeScreen(
        HomeUiState.ready(
            HomeContent(
                summary = HomeUiState.emptySummary(com.kurobello.tallybook.core.model.Moneda.COP),
                recent = emptyList(),
                categoriasById = emptyMap(),
            )
        ),
        {},
        {},
    )
  }
}

@Preview
@Composable
internal fun HomeErrorPreview() {
  TallybookTheme {
    HomeScreen(
        HomeUiState.error(
            com.kurobello.tallybook.core.model.DataError.Local(
                IllegalStateException("Preview failure")
            )
        ),
        {},
        {},
    )
  }
}

@Preview
@Composable
@Suppress("MagicNumber")
internal fun HomePopulatedPreview() {
  val categoria =
      com.kurobello.tallybook.core.model.Categoria(
          id = "food",
          nombre = stringResource(Res.string.category_comida),
          padreId = null,
          icono = com.kurobello.tallybook.core.model.CategoryIcon.UTENSILS,
          color = com.kurobello.tallybook.core.model.CategoryTint.AMBER,
      )
  val movimiento =
      com.kurobello.tallybook.core.model.Movimiento(
          id = "preview",
          tipo = com.kurobello.tallybook.core.model.TipoMovimiento.GASTO,
          monto =
              com.kurobello.tallybook.core.model.Money(
                  10_000,
                  com.kurobello.tallybook.core.model.Moneda.COP,
              ),
          fecha = kotlinx.datetime.LocalDate(2026, 9, 15),
          categoriaId = categoria.id,
          descripcion = null,
          createdAt = kotlin.time.Instant.parse("2026-09-15T00:00:00Z"),
      )
  TallybookTheme {
    HomeScreen(
        HomeUiState.ready(
            HomeContent(
                summary =
                    com.kurobello.tallybook.core.model.MovimientoSummary(
                        movimiento.monto,
                        movimiento.monto,
                    ),
                recent = listOf(movimiento),
                categoriasById = mapOf(categoria.id to categoria),
            )
        ),
        {},
        {},
    )
  }
}
