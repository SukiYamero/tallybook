package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.data.TimeZoneProvider
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddMovimientoViewModel(
    private val repositories: AddMovimientoRepositories,
    private val movimientoRepository: MovimientoRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
    scope: CoroutineScope? = null,
) : ViewModel(), AddMovimientoActions {

  private val coroutineScope = scope ?: viewModelScope
  private val retryLoadRequests = MutableStateFlow(0)
  private val initialDraft =
      MovimientoDraft(
          tipo = TipoMovimiento.GASTO,
          amountInput = "",
          fecha = clock.todayIn(timeZoneProvider.current()),
          selectedCategoriaId = null,
          descripcion = "",
      )

  val uiState: StateFlow<AddMovimientoUiState>
    field =
        MutableStateFlow(
            AddMovimientoUiState(
                tipo = initialDraft.tipo,
                amountInput = initialDraft.amountInput,
                fecha = initialDraft.fecha,
                moneda = Moneda.COP,
                categorias = emptyList(),
                selectedCategoriaId = initialDraft.selectedCategoriaId,
                descripcion = initialDraft.descripcion,
                isLoading = true,
                isSubmitting = false,
                submitAttempted = false,
                error = null,
            )
        )

  private val effectChannel = Channel<AddMovimientoEffect>(Channel.BUFFERED)
  val effects: Flow<AddMovimientoEffect> = effectChannel.receiveAsFlow()

  init {
    coroutineScope.launch {
      combine(
              repositories.preferenceLoadStates(retryLoadRequests),
              repositories.categoriaLoadStates(retryLoadRequests),
          ) { preferences, categorias ->
            LoadSnapshot.from(preferences, categorias)
          }
          .collect { snapshot ->
            uiState.update { current ->
              current.copy(
                  moneda = snapshot.moneda ?: current.moneda,
                  categorias = snapshot.categorias ?: current.categorias,
                  isLoading = snapshot.isLoading,
                  error =
                      snapshot.error
                          ?: current.error?.takeIf { it == MovimientoFormError.SAVE_FAILED },
              )
            }
          }
    }
  }

  override fun onTipoChanged(tipo: TipoMovimiento) {
    updateFields { copy(tipo = tipo) }
  }

  override fun onAmountChanged(value: String) {
    updateFields { copy(amountInput = value) }
  }

  override fun onFechaChanged(value: kotlinx.datetime.LocalDate) {
    updateFields { copy(fecha = value) }
  }

  override fun onCategoriaSelected(id: String) {
    updateFields { if (categorias.any { it.id == id }) copy(selectedCategoriaId = id) else this }
  }

  override fun onDescripcionChanged(value: String) {
    updateFields { copy(descripcion = value.take(MOVIMIENTO_DESCRIPTION_MAX_LENGTH)) }
  }

  override fun onDiscardChanges() {
    uiState.update { current ->
      if (current.isSubmitting) {
        current
      } else {
        current.copy(
            tipo = initialDraft.tipo,
            amountInput = initialDraft.amountInput,
            fecha = initialDraft.fecha,
            selectedCategoriaId = initialDraft.selectedCategoriaId,
            descripcion = initialDraft.descripcion,
            submitAttempted = false,
            error = current.error?.takeUnless { it == MovimientoFormError.SAVE_FAILED },
            hasUnsavedChanges = false,
        )
      }
    }
  }

  override fun onRetryLoad() {
    retryLoadRequests.update { it + 1 }
  }

  override fun onSubmit() {
    if (uiState.value.isSubmitting) return

    uiState.update {
      it.copy(
          submitAttempted = true,
          error = it.error?.takeUnless { error -> error == MovimientoFormError.SAVE_FAILED },
      )
    }
    val timeZone = timeZoneProvider.current()
    val createdAt = clock.now()
    val movement = uiState.value.toMovimientoOrNull(idGenerator, createdAt, timeZone) ?: return
    uiState.update { it.copy(isSubmitting = true) }
    coroutineScope.launch {
      var saved = false
      try {
        when (movimientoRepository.insert(movement)) {
          is DataOutcome.Success -> {
            effectChannel.send(AddMovimientoEffect.Saved)
            saved = true
          }
          is DataOutcome.Failure ->
              uiState.update { it.copy(error = MovimientoFormError.SAVE_FAILED) }
        }
      } finally {
        if (!saved) {
          uiState.update { it.copy(isSubmitting = false) }
        }
      }
    }
  }

  private fun updateFields(transform: AddMovimientoUiState.() -> AddMovimientoUiState) {
    uiState.update { current ->
      transform(current)
          .copy(error = current.error?.takeUnless { it == MovimientoFormError.SAVE_FAILED })
          .withDirtyState()
    }
  }

  private fun AddMovimientoUiState.withDirtyState(): AddMovimientoUiState =
      copy(hasUnsavedChanges = toDraft() != initialDraft)
}

internal data class AddMovimientoRepositories(
    val preferences: AppPreferencesRepository,
    val categorias: CategoriaRepository,
)

private data class MovimientoCandidate(
    val amount: Money?,
    val categoriaId: String?,
    val isCategoriaValid: Boolean,
    val isFechaValid: Boolean,
    val isReadyToSave: Boolean,
) {
  fun toMovimientoOrNull(
      idGenerator: IdGenerator,
      tipo: TipoMovimiento,
      fecha: kotlinx.datetime.LocalDate,
      descripcion: String?,
      createdAt: kotlin.time.Instant,
  ): Movimiento? =
      takeIf {
            listOf(
                    amount != null,
                    categoriaId != null,
                    isCategoriaValid,
                    isFechaValid,
                    isReadyToSave,
                )
                .all { it }
          }
          ?.let {
            Movimiento(
                id = idGenerator.next(),
                tipo = tipo,
                monto = requireNotNull(amount),
                fecha = fecha,
                categoriaId = requireNotNull(categoriaId),
                descripcion = descripcion,
                createdAt = createdAt,
            )
          }
}

private fun AddMovimientoUiState.toMovimientoOrNull(
    idGenerator: IdGenerator,
    createdAt: kotlin.time.Instant,
    timeZone: kotlinx.datetime.TimeZone,
): Movimiento? =
    MovimientoCandidate(
            amount = (parseAmountInput(amountInput, moneda) as? AmountInputResult.Valid)?.money,
            categoriaId = selectedCategoriaId,
            isCategoriaValid = categorias.any { it.id == selectedCategoriaId },
            isFechaValid = fecha <= createdAt.toLocalDateTime(timeZone).date,
            isReadyToSave = !isLoading && error == null,
        )
        .toMovimientoOrNull(
            idGenerator = idGenerator,
            tipo = tipo,
            fecha = fecha,
            descripcion = descripcion.normalizeDescription(),
            createdAt = createdAt,
        )

private sealed interface LoadState<out T> {
  data object Loading : LoadState<Nothing>

  data class Value<T>(val outcome: DataOutcome<T>) : LoadState<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun AddMovimientoRepositories.preferenceLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<AppPreferencesState>> = retryRequests.flatMapLatest {
  preferences.preferences
      .map<DataOutcome<AppPreferencesState>, LoadState<AppPreferencesState>> { LoadState.Value(it) }
      .onStart { emit(LoadState.Loading) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun AddMovimientoRepositories.categoriaLoadStates(
    retryRequests: Flow<Int>
): Flow<LoadState<List<com.kurobello.tallybook.core.model.Categoria>>> =
    retryRequests.flatMapLatest {
      categorias
          .observeAll()
          .map<
              DataOutcome<List<com.kurobello.tallybook.core.model.Categoria>>,
              LoadState<List<com.kurobello.tallybook.core.model.Categoria>>,
          > {
            LoadState.Value(it)
          }
          .onStart { emit(LoadState.Loading) }
    }

private data class LoadSnapshot(
    val moneda: Moneda?,
    val categorias: List<com.kurobello.tallybook.core.model.Categoria>?,
    val isLoading: Boolean,
    val error: MovimientoFormError?,
) {
  companion object {
    fun from(
        preferences: LoadState<AppPreferencesState>,
        categorias: LoadState<List<com.kurobello.tallybook.core.model.Categoria>>,
    ): LoadSnapshot =
        LoadSnapshot(
            moneda =
                (preferences as? LoadState.Value)
                    ?.outcome
                    .let { it as? DataOutcome.Success }
                    ?.data
                    ?.let { it as? AppPreferencesState.Ready }
                    ?.value
                    ?.monedaPrincipal,
            categorias =
                (categorias as? LoadState.Value)?.outcome.let { it as? DataOutcome.Success }?.data,
            isLoading = preferences is LoadState.Loading || categorias is LoadState.Loading,
            error =
                when {
                  (preferences as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                      MovimientoFormError.PREFERENCES_UNAVAILABLE
                  (categorias as? LoadState.Value)?.outcome is DataOutcome.Failure ->
                      MovimientoFormError.CATEGORIES_UNAVAILABLE
                  else -> null
                },
        )
  }
}

private fun String.normalizeDescription(): String? =
    trim().replace(Regex("\\s+"), " ").ifEmpty { null }
