package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.model.MedioPago
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class SearchFilterSessionTest {
  private val today = LocalDate(2028, 3, 1)
  private val id = SearchFilterSessionId(1)
  private val nextId = SearchFilterSessionId(2)
  private val applied =
      SearchFilters.initial(today)
          .copy(preset = SearchDatePreset.ThisMonth, categoryIds = setOf("kept"))

  @Test
  fun editCancelAndReopenKeepAppliedInputAndCapturedClosingRender() {
    val editing =
        SearchFilterSession.Hidden.open(id, applied).edit(id) {
          it.copy(preset = SearchDatePreset.Last7Days)
        }
    val closing = assertIs<SearchFilterSession.Closing>(editing.cancel(id))
    assertEquals(assertIs<SearchFilterSession.Editing>(editing).draft, closing.renderSnapshot)
    assertEquals(closing, closing.open(nextId, applied))
    assertEquals(closing, closing.edit(id) { it.copy(categoryIds = setOf("stale")) })
    val reopened = assertIs<SearchFilterSession.Editing>(closing.hidden(id).open(nextId, applied))
    assertEquals(applied, reopened.draft)
    assertEquals(setOf("kept"), applied.categoryIds)
  }

  @Test
  fun accountAndMedioDraftsCommitOnlyOnApplyAndClearDropsThem() {
    val opened = SearchFilterSession.Hidden.open(id, applied)
    val edited = opened.edit(id) { it.toggleCuenta("nequi").toggleMedio(MedioPago.CREDITO) }
    assertEquals(applied, (opened as SearchFilterSession.Editing).draft)
    val committed = edited.apply(id, applied.customEnd).committed
    assertEquals(setOf("nequi"), committed?.cuentaIds)
    assertEquals(setOf(MedioPago.CREDITO), committed?.medios)
    val cleared = edited.clear(id, applied.customEnd).committed
    assertEquals(emptySet(), cleared?.cuentaIds)
    assertEquals(emptySet(), cleared?.medios)
  }

  @Test
  fun clearCommitsFreshDefaultsAndClosesExactlyOnce() {
    val now = LocalDate(2028, 3, 2)
    val defaults = SearchFilters.initial(now)
    val cleared = SearchFilterSession.Hidden.open(id, applied).clear(id, now)
    assertEquals(defaults, cleared.committed)
    val closing = assertIs<SearchFilterSession.Closing>(cleared.session)
    assertEquals(defaults, closing.renderSnapshot)
    assertNull(closing.clear(id, now).committed)
    assertEquals(closing, closing.clear(id, now).session)
    assertNull(closing.apply(id, now).committed)
  }

  @Test
  fun validUnchangedApplyCommitsExactlyOnceWithoutResultCount() {
    val first = SearchFilterSession.Hidden.open(id, applied).apply(id, today)
    assertEquals(applied, first.committed)
    val closing = assertIs<SearchFilterSession.Closing>(first.session)
    assertEquals(applied, closing.renderSnapshot)
    val duplicate = closing.apply(id, today)
    assertNull(duplicate.committed)
    assertEquals(closing, duplicate.session)
    assertEquals(closing, closing.cancel(id))
    assertEquals(SearchFilterSession.Hidden, closing.hidden(id))
    assertEquals(SearchFilterSession.Hidden, closing.hidden(id).hidden(id))
  }

  @Test
  fun invalidApplyKeepsEditingWithFeedbackAndNoCommit() {
    val custom = applied.copy(preset = SearchDatePreset.Custom)
    val result = SearchFilterSession.Hidden.open(id, custom).apply(id, LocalDate(2028, 2, 29))
    assertNull(result.committed)
    val editing = assertIs<SearchFilterSession.Editing>(result.session)
    assertEquals(custom, editing.draft)
    assertEquals(SearchDateFailure.FutureDate, editing.dateFailure)
    val cleared = editing.clear(id, today)
    assertEquals(SearchFilters.initial(today), cleared.committed)
    assertIs<SearchFilterSession.Closing>(cleared.session)
  }

  @Test
  fun validApplyNormalizesDraftAndClosingSnapshotAtomically() {
    val reversed =
        applied.copy(preset = SearchDatePreset.Custom, customEnd = LocalDate(2027, 12, 1))
    val result = SearchFilterSession.Hidden.open(id, reversed).apply(id, today)
    assertEquals(LocalDate(2027, 12, 1), result.committed!!.customStart)
    assertEquals(today, result.committed.customEnd)
    assertEquals(
        result.committed,
        assertIs<SearchFilterSession.Closing>(result.session).renderSnapshot,
    )
  }

  @Test
  fun oldSessionActionsCannotEditOrReleaseANewerSession() {
    val newer =
        SearchFilterSession.Hidden.open(id, applied).cancel(id).hidden(id).open(nextId, applied)
    assertEquals(newer, newer.edit(id) { SearchFilters.initial(today) })
    assertEquals(newer, newer.clear(id, today).session)
    assertNull(newer.clear(id, today).committed)
    assertEquals(newer, newer.cancel(id))
    assertEquals(newer, newer.hidden(id))
    assertNull(newer.apply(id, today).committed)
    val closing = newer.cancel(nextId)
    assertEquals(closing, closing.hidden(id))
    assertEquals(closing, closing.clear(nextId, today).session)
    assertEquals(SearchFilterSession.Hidden, closing.hidden(nextId))
  }
}
