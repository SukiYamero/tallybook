package com.kurobello.tallybook.core.ui.resources

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import java.io.File
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.budget_history_within
import tallybook.shared.generated.resources.budget_suggestion
import tallybook.shared.generated.resources.budgets_line_left
import tallybook.shared.generated.resources.budgets_line_over
import tallybook.shared.generated.resources.budgets_line_pace
import tallybook.shared.generated.resources.budgets_line_per_day
import tallybook.shared.generated.resources.budgets_period
import tallybook.shared.generated.resources.budgets_summary
import tallybook.shared.generated.resources.profile_budgets_body

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetsResourcesHostTest {

  @Test
  fun everyLocaleDeclaresExactlyTheDefaultKeys() {
    val defaultKeys = resourceKeys("values")
    assertTrue(defaultKeys.isNotEmpty())
    LOCALE_FOLDERS.forEach { folder -> assertEquals(defaultKeys, resourceKeys(folder), folder) }
  }

  @Test
  fun spanishCopyMatchesTheSpec() =
      inLocale("es-CO") {
        assertEquals(
            "${cop(20_000)} por encima de tu presupuesto",
            getString(Res.string.budgets_line_over, cop(20_000)),
        )
        assertEquals(
            "Te quedan ${cop(30_000)}",
            getString(Res.string.budgets_line_left, cop(30_000)),
        )
        assertEquals(
            "A este ritmo llegarías al tope el día 22",
            getString(Res.string.budgets_line_pace, 22),
        )
        assertEquals(
            "Te quedan ${cop(110_000)} · ${cop(9_000)} por día",
            getString(Res.string.budgets_line_per_day, cop(110_000), cop(9_000)),
        )
        assertEquals("Vas bien en 5 de 7", getString(Res.string.budgets_summary, 5, 7))
        assertEquals(
            "Dentro del tope en 3 de los últimos 5 meses",
            getString(Res.string.budget_history_within, 3, 5),
        )
        assertEquals(
            "Tu promedio: ${cop(430_000)}",
            getString(Res.string.budget_suggestion, cop(430_000)),
        )
        assertEquals(
            "noviembre · quedan 12 días",
            getPluralString(Res.plurals.budgets_period, 12, "noviembre", 12),
        )
        assertEquals(
            "noviembre · queda 1 día",
            getPluralString(Res.plurals.budgets_period, 1, "noviembre", 1),
        )
      }

  @Test
  fun eachLocaleKeepsItsOwnVoice() = runBlocking {
    val expected =
        mapOf(
            "es-AR" to "Ponele un tope mensual a lo que gastás en cada categoría.",
            "en-US" to "Set a monthly limit for what you spend in each category.",
            "pt-BR" to "Defina um limite mensal para o que você gasta em cada categoria.",
        )
    expected.forEach { (locale, body) ->
      inLocale(locale) { assertEquals(body, getString(Res.string.profile_budgets_body), locale) }
    }
  }

  private fun resourceKeys(folder: String): Set<String> =
      RESOURCE_NAME.findAll(File(RESOURCES_DIR, "$folder/strings_budgets.xml").readText())
          .map { it.groupValues[1] }
          .toSet()

  private fun inLocale(tag: String, block: suspend () -> Unit) = runBlocking {
    val original = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag(tag))
      block()
    } finally {
      Locale.setDefault(original)
    }
  }

  private fun cop(units: Long): String = Money(units * 100, Moneda.COP).format()

  private companion object {
    const val RESOURCES_DIR = "src/commonMain/composeResources"
    val LOCALE_FOLDERS = listOf("values-en", "values-es-rAR", "values-pt-rBR")
    val RESOURCE_NAME = Regex("""<(?:string|plurals|string-array) name="([^"]+)"""")
  }
}
