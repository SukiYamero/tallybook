package com.kurobello.tallybook.core.ui.resources

import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.feature.categories.ui.labelResource
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
import tallybook.shared.generated.resources.category_form_name_blank
import tallybook.shared.generated.resources.category_in_use_message
import tallybook.shared.generated.resources.category_picker_create

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryFormResourcesHostTest {

  // Compose resources fall back to the default locale, so a missing key would still resolve.
  @Test
  fun everyLocaleDeclaresExactlyTheDefaultKeys() {
    val defaultKeys = resourceKeys("values")
    LOCALE_FOLDERS.forEach { folder -> assertEquals(defaultKeys, resourceKeys(folder), folder) }
  }

  @Test
  fun formCopyFollowsEachLocaleVoice() = inEveryLocale { locale ->
    val (blank, create) = FORM_COPY.getValue(locale)
    assertEquals(blank, getString(Res.string.category_form_name_blank), locale)
    assertEquals(create, getString(Res.string.category_picker_create, "Gym"), locale)
  }

  @Test
  fun inUseMessageAgreesInNumberForEveryLocale() = inEveryLocale { locale ->
    val one = getPluralString(Res.plurals.category_in_use_message, 1, 1)
    val three = getPluralString(Res.plurals.category_in_use_message, 3, 3)
    assertTrue("3" !in one && "3" in three, "$locale: $one / $three")
  }

  @Test
  fun everyIconAndTintHasALabelInEveryLocale() = inEveryLocale { locale ->
    val labels =
        CategoryIcon.entries.map { getString(it.labelResource()) } +
            CategoryTint.entries.map { getString(it.labelResource()) }
    assertTrue(labels.none(String::isBlank), locale)
    assertEquals(CategoryIcon.entries.size, labels.take(CategoryIcon.entries.size).distinct().size)
  }

  private fun resourceKeys(folder: String): Set<String> =
      RESOURCE_NAME.findAll(File(RESOURCES_DIR, "$folder/strings.xml").readText())
          .map { it.groupValues[1] }
          .toSet()

  private fun inEveryLocale(block: suspend (String) -> Unit) = runBlocking {
    val original = Locale.getDefault()
    try {
      FORM_COPY.keys.forEach { locale ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        block(locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }

  private companion object {
    const val RESOURCES_DIR = "src/commonMain/composeResources"
    val LOCALE_FOLDERS = listOf("values-en", "values-es-rAR", "values-pt-rBR")
    val RESOURCE_NAME = Regex("""<(?:string|plurals|string-array) name="([^"]+)"""")
    val FORM_COPY =
        mapOf(
            "es-CO" to ("Escribe un nombre para la categoría" to "Crear “Gym”"),
            "es-AR" to ("Escribí un nombre para la categoría" to "Crear “Gym”"),
            "en-US" to ("Enter a name for the category" to "Create “Gym”"),
            "pt-BR" to ("Digite um nome para a categoria" to "Criar “Gym”"),
        )
  }
}
