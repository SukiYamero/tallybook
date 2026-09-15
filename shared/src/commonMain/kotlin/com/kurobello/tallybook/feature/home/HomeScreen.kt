package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetRoute
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.home_add_first

@Composable
internal fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sheetSession = remember { AddMovimientoSheetSession() }

  HomeSheetSessionLifecycle(sheetSession)

  HomeScreen(
      uiState = uiState,
      onAddMovimiento = sheetSession::open,
      onRetry = viewModel::retry,
      modifier = modifier,
  )

  if (sheetSession.hasBeenOpened) {
    val addMovimientoViewModel: AddMovimientoViewModel =
        koinViewModel(
            key = sheetSession.viewModelKey,
            viewModelStoreOwner = sheetSession.viewModelStoreOwner,
        )
    AddMovimientoSheetRoute(
        visible = sheetSession.isVisible,
        onDismiss = sheetSession::dismiss,
        onHidden = sheetSession::onSheetHidden,
        viewModel = addMovimientoViewModel,
    )
  }
}

internal class AddMovimientoSheetSession {
  var isVisible by mutableStateOf(false)
    private set

  var hasBeenOpened by mutableStateOf(false)
    private set

  private var nextSession by mutableIntStateOf(0)
  private var activeSession by mutableIntStateOf(0)
  private var owner: AddMovimientoViewModelStoreOwner? = null

  val viewModelKey: String
    get() = "add-movimiento-$activeSession"

  val viewModelStoreOwner: ViewModelStoreOwner
    get() = requireNotNull(owner)

  fun open() {
    if (hasBeenOpened) return
    activeSession = nextSession
    owner = AddMovimientoViewModelStoreOwner()
    hasBeenOpened = true
    isVisible = true
  }

  fun dismiss() {
    if (!isVisible) return
    isVisible = false
  }

  fun onSheetHidden() {
    if (isVisible || !hasBeenOpened) return
    release()
    nextSession += 1
  }

  fun dispose() {
    release()
  }

  private fun release() {
    owner?.viewModelStore?.clear()
    owner = null
    isVisible = false
    hasBeenOpened = false
  }
}

private class AddMovimientoViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAddMovimiento: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val addMovementLabel = stringResource(Res.string.home_add_first)
  TallybookScreenFrame(modifier = modifier) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(bottom = TallybookTheme.dimensions.homeFloatingActionClearance),
    ) {
      item { HomeHeader(modifier = Modifier.padding(bottom = TallybookTheme.spacing.space20)) }
      item { HomeSearch(modifier = Modifier.padding(bottom = TallybookTheme.spacing.space18)) }
      when {
        uiState.isLoading -> item { HomeLoading() }
        uiState.error != null -> item { HomeError(onRetry) }
        uiState.isEmpty -> item { HomeEmpty(onAddMovimiento) }
        else -> homePopulated(uiState)
      }
    }

    FloatingActionButton(
        modifier =
            Modifier.align(Alignment.BottomEnd).semantics { contentDescription = addMovementLabel },
        onClick = onAddMovimiento,
    ) {
      TallybookText("+")
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
        HomeUiState(
            isLoading = false,
            summary = HomeUiState.emptySummary(com.kurobello.tallybook.core.model.Moneda.COP),
            recent = emptyList(),
            categoriasById = emptyMap(),
            error = null,
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
        HomeUiState(
            isLoading = false,
            summary =
                com.kurobello.tallybook.core.model.MovimientoSummary(
                    movimiento.monto,
                    movimiento.monto,
                ),
            recent = listOf(movimiento),
            categoriasById = mapOf(categoria.id to categoria),
            error = null,
        ),
        {},
        {},
    )
  }
}
