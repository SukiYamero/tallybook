package com.kurobello.tallybook.core.ui.state

internal sealed interface RetainedLoadState<out T : Any, out E : Any> {
  data class Loading<out T : Any>(val previous: T? = null) : RetainedLoadState<T, Nothing>

  data class Ready<out T : Any>(val value: T) : RetainedLoadState<T, Nothing>

  data class Failed<out T : Any, out E : Any>(override val failure: E, val previous: T? = null) :
      RetainedLoadState<T, E>

  val failure: E?
    get() =
        when (this) {
          is Loading -> null
          is Ready -> null
          is Failed -> failure
        }

  val cachedValue: T?
    get() =
        when (this) {
          is Loading -> previous
          is Ready -> value
          is Failed -> previous
        }

  val isLoading: Boolean
    get() = this is Loading

  val hasSnapshot: Boolean
    get() = cachedValue != null

  val isInitialLoading: Boolean
    get() = isLoading && !hasSnapshot

  fun refreshing(): Loading<T> = Loading(cachedValue)
}

internal fun <T : Any, E : Any> RetainedLoadState<T, E>.failed(
    failure: E
): RetainedLoadState.Failed<T, E> = RetainedLoadState.Failed(failure, cachedValue)
