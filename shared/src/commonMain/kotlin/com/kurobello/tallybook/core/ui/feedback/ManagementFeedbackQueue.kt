package com.kurobello.tallybook.core.ui.feedback

import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.RestoreResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ManagementFeedbackState(
    val feedbackQueue: List<ManagementFeedback> = emptyList(),
    val openSessions: Int = 0,
) {
  val feedback: ManagementFeedback?
    get() = feedbackQueue.firstOrNull()

  val isRestoring: Boolean
    get() = feedback?.isRestoring == true
}

/**
 * One per shell, outliving every management sheet. Sessions are admitted here because a claimed
 * restore and an open management sheet exclude each other: Undo waits while any sheet is open and
 * no sheet opens while a restore runs.
 */
internal class ManagementFeedbackQueue(
    private val scope: CoroutineScope,
    private val restore: suspend (DeletedSnapshot) -> DataOutcome<RestoreResult>,
) {
  private var nextToken = 0L

  val state: StateFlow<ManagementFeedbackState>
    field = MutableStateFlow(ManagementFeedbackState())

  fun openSession(): Boolean {
    var admitted = false
    state.update {
      admitted = !it.isRestoring
      if (admitted) it.copy(openSessions = it.openSessions + 1) else it
    }
    return admitted
  }

  fun closeSession(outcome: ManagementOutcome?) {
    val feedback = outcome?.let {
      ManagementFeedback(token = ++nextToken, kind = it.kind, deleted = it.deleted)
    }
    state.update {
      it.copy(
          feedbackQueue = if (feedback == null) it.feedbackQueue else it.feedbackQueue + feedback,
          openSessions = (it.openSessions - 1).coerceAtLeast(0),
      )
    }
  }

  fun onFeedbackAction(token: Long, revision: Int) {
    val current = state.value
    val feedback = current.feedbackFor(token, revision)?.takeIf { current.openSessions == 0 }
    val deleted = feedback?.deleted ?: return
    val claimed = feedback.copy(revision = revision + 1, isRestoring = true)
    if (state.compareAndSet(current, current.withHead(claimed))) {
      scope.launch { complete(claimed, deleted, restore(deleted)) }
    }
  }

  fun onFeedbackDismiss(token: Long, revision: Int) {
    val feedback = state.value.feedbackFor(token, revision) ?: return
    state.update {
      if (it.feedback == feedback) it.copy(feedbackQueue = it.feedbackQueue.drop(1)) else it
    }
  }

  fun clear() {
    state.value = ManagementFeedbackState()
  }

  private fun complete(
      claimed: ManagementFeedback,
      deleted: DeletedSnapshot,
      outcome: DataOutcome<RestoreResult>,
  ) {
    val kind =
        when (outcome) {
          is DataOutcome.Failure -> ManagementFeedbackKind.RestoreFailed
          is DataOutcome.Success ->
              when (outcome.data) {
                RestoreResult.Restored,
                RestoreResult.AlreadyPresent -> ManagementFeedbackKind.Restored
                RestoreResult.Conflict -> ManagementFeedbackKind.RestoreConflict
                RestoreResult.MissingCuenta -> ManagementFeedbackKind.RestoreFailed
              }
        }
    // Only a storage failure may succeed on retry; without its cuenta the snapshot is released so
    // the money never lands in another one.
    val retryable = outcome is DataOutcome.Failure
    val completed =
        claimed.copy(
            revision = claimed.revision + 1,
            kind = kind,
            isRestoring = false,
            deleted = deleted.takeIf { retryable },
        )
    state.update { latest ->
      if (latest.feedback == claimed) latest.withHead(completed) else latest
    }
  }
}

private fun ManagementFeedbackState.feedbackFor(token: Long, revision: Int): ManagementFeedback? =
    feedback?.takeIf {
      it.token == token && it.revision == revision && !it.isRestoring
    }

private fun ManagementFeedbackState.withHead(head: ManagementFeedback) =
    copy(feedbackQueue = listOf(head) + feedbackQueue.drop(1))
