package com.kurobello.tallybook.core.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class TallybookModalDialogPlacementTest {

  private val rest = DialogBand(top = 100, bottom = 2300)
  private val withKeyboard = DialogBand(top = 100, bottom = 1300)

  @Test
  fun theFirstPlacementCentersTheDialogInTheRestBand() {
    val placement = DialogVerticalPlacement()

    assertEquals(1000, placement.top(rest, rest, dialogHeight = 400))
  }

  @Test
  fun growthKeepsTheTopEdgeSoTheDialogExtendsDownward() {
    val placement = DialogVerticalPlacement()
    val opened = placement.top(rest, rest, dialogHeight = 400)

    assertEquals(opened, placement.top(rest, rest, dialogHeight = 700))
    assertEquals(opened, placement.top(rest, rest, dialogHeight = 1000))
  }

  @Test
  fun theKeyboardMovesTheDialogUpOnlyByTheMinimumNeeded() {
    val placement = DialogVerticalPlacement()
    val opened = placement.top(rest, rest, dialogHeight = 400)

    val bottomUnderKeyboard = opened + 400
    val shifted = placement.top(rest, withKeyboard, dialogHeight = 400)

    assertEquals(opened - (bottomUnderKeyboard - withKeyboard.bottom), shifted)
    assertEquals(withKeyboard.bottom, shifted + 400)
  }

  @Test
  fun theKeyboardDoesNotMoveADialogThatAlreadyFitsAboveIt() {
    val placement = DialogVerticalPlacement()
    val opened = placement.top(rest, rest, dialogHeight = 200)

    assertEquals(opened, placement.top(rest, withKeyboard, dialogHeight = 200))
  }

  @Test
  fun closingTheKeyboardReturnsToTheCapturedTop() {
    val placement = DialogVerticalPlacement()
    val opened = placement.top(rest, rest, dialogHeight = 400)
    placement.top(rest, withKeyboard, dialogHeight = 900)

    assertEquals(opened, placement.top(rest, rest, dialogHeight = 400))
  }

  @Test
  fun aDialogTallerThanTheBandStopsAtTheTopOfTheBand() {
    val placement = DialogVerticalPlacement()
    placement.top(rest, rest, dialogHeight = 400)

    assertEquals(withKeyboard.top, placement.top(rest, withKeyboard, dialogHeight = 1400))
  }

  @Test
  fun aDialogTallerThanTheRestBandStartsAtItsTop() {
    assertEquals(rest.top, DialogVerticalPlacement().top(rest, rest, dialogHeight = 5000))
  }

  @Test
  fun aNewRestBandCentersAgain() {
    val placement = DialogVerticalPlacement()
    placement.top(rest, rest, dialogHeight = 400)
    val landscape = DialogBand(top = 50, bottom = 950)

    assertEquals(50 + 250, placement.top(landscape, landscape, dialogHeight = 400))
  }
}
