package com.kurobello.tallybook.core.ui.tag

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_tag_field_suggestions
import tallybook.shared.generated.resources.movimiento_tags_section
import tallybook.shared.generated.resources.tag_add_description
import tallybook.shared.generated.resources.tag_remove

@OptIn(ExperimentalResourceApi::class)
class EtiquetaTagFieldTest {
  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun theSectionHeaderIsAHeadingAndTagsAreSelected() {
    composeRule.setContent {
      TallybookTheme { EtiquetaTagField(listOf(trip), max = LIMIT, onRemove = {}, onAdd = {}) }
    }

    composeRule.onNodeWithText(string(Res.string.movimiento_tags_section)).assert(isHeading())
    composeRule.onNodeWithText(trip.nombre).assertIsSelected()
  }

  @Test
  fun theLastTagCanBeRemoved() {
    val removed = mutableListOf<Etiqueta>()
    composeRule.setContent {
      TallybookTheme {
        EtiquetaTagField(listOf(trip), max = LIMIT, onRemove = { removed += it }, onAdd = {})
      }
    }

    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_remove, trip.nombre))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .performClick()

    composeRule.runOnIdle { assertEquals(listOf(trip), removed) }
  }

  @Test
  fun addOpensThePickerBelowTheLimitAndSuggestionsSkipSelectedTags() {
    var adds = 0
    val suggested = mutableListOf<Etiqueta>()
    composeRule.setContent {
      TallybookTheme {
        EtiquetaTagField(
            etiquetas = listOf(trip),
            max = LIMIT,
            onRemove = {},
            onAdd = { adds++ },
            suggestions = listOf(trip, work),
            onSuggestion = { suggested += it },
        )
      }
    }

    composeRule.onNodeWithContentDescription(string(Res.string.tag_add_description)).performClick()
    composeRule.onNodeWithText(work.nombre).performClick()

    composeRule.runOnIdle {
      assertEquals(1, adds)
      assertEquals(listOf(work), suggested)
    }
  }

  @Test
  fun addAndSuggestionsAreHiddenAtTheLimit() {
    composeRule.setContent {
      TallybookTheme {
        EtiquetaTagField(
            etiquetas = listOf(trip, work),
            max = 2,
            onRemove = {},
            onAdd = {},
            suggestions = listOf(home),
        )
      }
    }

    composeRule
        .onNodeWithContentDescription(string(Res.string.tag_add_description))
        .assertDoesNotExist()
    composeRule
        .onNodeWithText(string(Res.string.category_tag_field_suggestions))
        .assertDoesNotExist()
    composeRule.onNodeWithText(home.nombre).assertDoesNotExist()
  }

  private fun string(resource: StringResource, vararg args: Any): String = runBlocking {
    getString(resource, *args)
  }

  private companion object {
    const val LIMIT = 6
    val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z")
    val trip = Etiqueta("trip", "Viaje Chile", createdAt)
    val work = Etiqueta("work", "Empresa", createdAt)
    val home = Etiqueta("home", "Casa", createdAt)
  }
}
