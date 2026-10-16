package com.kurobello.tallybook.feature.movimientos.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPickerSelectionTest {

  @Test
  fun choosingAnotherCategoryReportsItOnce() {
    val updates = mutableListOf<String>()

    CategoryPickerSelection(selectedId = "health") { updates += it }.choose("food")

    assertEquals(listOf("food"), updates)
  }

  @Test
  fun choosingWithoutASelectionReportsTheChoice() {
    val updates = mutableListOf<String>()

    CategoryPickerSelection(selectedId = null) { updates += it }.choose("food")

    assertEquals(listOf("food"), updates)
  }

  @Test
  fun choosingTheCurrentCategoryReportsNothing() {
    val updates = mutableListOf<String>()

    CategoryPickerSelection(selectedId = "health") { updates += it }.choose("health")

    assertEquals(emptyList(), updates)
  }
}
