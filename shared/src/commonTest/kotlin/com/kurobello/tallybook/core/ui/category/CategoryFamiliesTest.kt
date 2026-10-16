package com.kurobello.tallybook.core.ui.category

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryFamiliesTest {
  @Test
  fun parentsKeepPersistedSiblingOrderWithChildrenAndOrphansReachable() {
    val input =
        listOf(
            category("child-b", "parent"),
            category("parent"),
            category("child-a", "parent"),
            category("orphan", "missing"),
            category("other"),
        )
    assertEquals(
        listOf(listOf("parent", "child-b", "child-a"), listOf("orphan"), listOf("other")),
        categoryFamilies(input).map { group -> group.map { it.id } },
    )
  }

  @Test
  fun cyclicImportedParentsDoNotHideEntriesOrVisitThemTwice() {
    val input = listOf(category("first", "second"), category("second", "first"))
    assertEquals(listOf("first", "second"), categoryFamilies(input).flatten().map { it.id })
  }

  private fun category(id: String, parent: String? = null) =
      Categoria(id, id, parent, CategoryIcon.UTENSILS, CategoryTint.AMBER)
}
