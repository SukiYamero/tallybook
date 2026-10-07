package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Immutable

internal enum class TallybookSnackbarDuration {
  Short,
  Long,
  Indefinite,
}

internal enum class TallybookSnackbarDismissReason {
  Timeout,
  Close,
}

@Immutable
internal data class TallybookSnackbarMessage(
    val token: Long,
    val revision: Int,
    val message: String,
    val duration: TallybookSnackbarDuration,
    val actionLabel: String? = null,
    val isBusy: Boolean = false,
)
