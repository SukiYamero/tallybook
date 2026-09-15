package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.ui.movimiento.BalanceCard
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetRoute
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.app_name
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.home_add_first
import tallybook.shared.generated.resources.home_empty_body
import tallybook.shared.generated.resources.home_empty_title
import tallybook.shared.generated.resources.home_load_error_body
import tallybook.shared.generated.resources.home_load_error_title
import tallybook.shared.generated.resources.home_recent
import tallybook.shared.generated.resources.loading
import tallybook.shared.generated.resources.retry

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
  Scaffold(
      modifier = modifier,
      topBar = { TopAppBar(title = { Text(stringResource(Res.string.app_name)) }) },
      floatingActionButton = {
        FloatingActionButton(
            modifier = Modifier.semantics { contentDescription = addMovementLabel },
            onClick = onAddMovimiento,
        ) {
          Text("+")
        }
      },
  ) { contentPadding ->
    when {
      uiState.isLoading -> HomeLoading(contentPadding)
      uiState.error != null -> HomeError(contentPadding, onRetry)
      uiState.isEmpty -> HomeEmpty(contentPadding, onAddMovimiento)
      else -> HomePopulated(uiState, contentPadding)
    }
  }
}

@Composable
private fun HomeLoading(contentPadding: PaddingValues) {
  val loadingDescription = stringResource(Res.string.loading)
  Box(
      modifier = Modifier.fillMaxSize().padding(contentPadding),
      contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
        modifier = Modifier.semantics { contentDescription = loadingDescription }
    )
  }
}

@Composable
private fun HomeError(contentPadding: PaddingValues, onRetry: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = stringResource(Res.string.home_load_error_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(text = stringResource(Res.string.home_load_error_body))
    Button(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun HomeEmpty(contentPadding: PaddingValues, onAddMovimiento: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = stringResource(Res.string.home_empty_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(text = stringResource(Res.string.home_empty_body))
    Button(onClick = onAddMovimiento) { Text(stringResource(Res.string.home_add_first)) }
  }
}

@Composable
private fun HomePopulated(uiState: HomeUiState, contentPadding: PaddingValues) {
  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      BalanceCard(summary = uiState.summary, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
    item {
      Text(stringResource(Res.string.home_recent), modifier = Modifier.padding(horizontal = 16.dp))
    }
    items(items = uiState.recent, key = { it.id }) { movimiento ->
      MovimientoRow(
          movimiento = movimiento,
          categoria = uiState.categoriasById[movimiento.categoriaId],
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
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
