package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TallybookDismissGuardTest {

  @Test
  fun cleanDraftDismissesImmediately() {
    assertEquals(DismissalDecision.Dismiss, dismissalDecision(hasUnsavedChanges = false))
  }

  @Test
  fun changedDraftRequestsConfirmation() {
    assertEquals(DismissalDecision.RequestConfirmation, dismissalDecision(hasUnsavedChanges = true))
  }

  @Test
  fun continuingEditingKeepsTheSheetOpenAndPreservesTheDraft() {
    var discardCount = 0
    var dismissCount = 0
    val state = DismissGuardState()
    state.update(
        hasUnsavedChanges = true,
        onDiscardChanges = { discardCount += 1 },
        onDismiss = { dismissCount += 1 },
    )
    state.requestDismiss()

    state.continueEditing()

    assertFalse(state.isConfirmationVisible)
    assertEquals(0, discardCount)
    assertEquals(0, dismissCount)
  }

  @Test
  fun confirmingDiscardClearsTheDraftBeforeClosingTheSheet() {
    val events = mutableListOf<String>()
    val state = DismissGuardState()
    state.update(
        hasUnsavedChanges = true,
        onDiscardChanges = { events += "discard" },
        onDismiss = { events += "dismiss" },
    )
    state.requestDismiss()

    state.confirmDiscard()

    assertFalse(state.isConfirmationVisible)
    assertEquals(listOf("discard", "dismiss"), events)
  }

  @Test
  fun requestForChangedDraftShowsTheConfirmation() {
    val state = DismissGuardState()
    state.update(hasUnsavedChanges = true, onDiscardChanges = {}, onDismiss = {})

    val wasDismissed = state.requestDismiss()

    assertFalse(wasDismissed)
    assertTrue(state.isConfirmationVisible)
  }

  @Test
  fun cleanDraftAllowsTheSheetToHide() {
    val state = DismissGuardState()
    state.update(hasUnsavedChanges = false, onDiscardChanges = {}, onDismiss = {})

    val wasDismissed = state.requestDismiss()

    assertTrue(wasDismissed)
  }

  @Test
  fun savingIgnoresDismissRequests() {
    var discardCount = 0
    var dismissCount = 0
    val state = DismissGuardState()
    state.update(
        hasUnsavedChanges = true,
        isDismissEnabled = false,
        onDiscardChanges = { discardCount += 1 },
        onDismiss = { dismissCount += 1 },
    )

    state.requestDismiss()

    assertFalse(state.isConfirmationVisible)
    assertEquals(0, discardCount)
    assertEquals(0, dismissCount)
  }
}
