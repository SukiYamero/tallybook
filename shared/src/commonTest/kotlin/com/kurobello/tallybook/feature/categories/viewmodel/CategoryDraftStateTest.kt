package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.entry.CategoryAppearance
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryDraftStateTest {

  private val suggester =
      CategoryAppearanceSuggester(CategoryKeywordDictionary.builtIn, CategorySeedCatalog.categories)

  private val gym: CategoryAppearance =
      CategorySeedCatalog.categories
          .single { it.id == "cat_gimnasio" }
          .let { CategoryAppearance(it.icono, it.color) }

  private val unmatched = CategoryAppearance(CategoryIcon.TAG, CategoryTint.NEUTRAL)

  private val CategoryDraftState.appearance: CategoryAppearance
    get() = CategoryAppearance(icono, color)

  @Test
  fun untouchedAppearanceFollowsEveryNameChange() {
    val empty = CategoryDraftState.forNew("", suggester)
    val gymDraft = empty.onNombre("GYM", suggester)
    val renamed = gymDraft.onNombre("zzqx", suggester)

    assertEquals(unmatched, empty.appearance)
    assertEquals(gym, gymDraft.appearance)
    assertEquals(unmatched, renamed.appearance)
  }

  @Test
  fun newDraftStartsFromTheSuggestionOfItsPrefilledName() {
    val draft = CategoryDraftState.forNew("Gimnasio", suggester)

    assertEquals("Gimnasio", draft.nombre)
    assertEquals(gym, draft.appearance)
    assertFalse(draft.nombreEdited)
  }

  @Test
  fun pickingAnIconStopsOnlyTheIconFromFollowing() {
    val draft =
        CategoryDraftState.forNew("", suggester)
            .onIcono(CategoryIcon.BOOK)
            .onNombre("GYM", suggester)

    assertEquals(CategoryIcon.BOOK, draft.icono)
    assertEquals(gym.color, draft.color)
    assertTrue(draft.iconoTouched)
    assertFalse(draft.colorTouched)
  }

  @Test
  fun pickingATintStopsOnlyTheTintFromFollowing() {
    val draft =
        CategoryDraftState.forNew("", suggester)
            .onColor(CategoryTint.PURPLE)
            .onNombre("GYM", suggester)

    assertEquals(gym.icono, draft.icono)
    assertEquals(CategoryTint.PURPLE, draft.color)
  }

  @Test
  fun editingKeepsTheStoredAppearanceAndDescription() {
    val stored =
        Categoria(
            id = "custom-1",
            nombre = "Lectura",
            padreId = null,
            icono = CategoryIcon.BOOK,
            color = CategoryTint.ROSE,
            descripcion = "Libros y revistas",
            origen = CategoriaOrigen.USUARIO,
        )

    val draft = CategoryDraftState.forEdit(stored).onNombre("GYM", suggester)

    assertEquals(CategoryAppearance(CategoryIcon.BOOK, CategoryTint.ROSE), draft.appearance)
    assertEquals("Libros y revistas", draft.descripcion)
  }

  @Test
  fun nameAndDescriptionInputIsCappedSoPasteCannotExceedTheLimits() {
    val draft =
        CategoryDraftState()
            .onNombre("a".repeat(CategoriaRules.NOMBRE_MAX + 5), suggester)
            .onDescripcion("b".repeat(CategoriaRules.DESCRIPCION_MAX + 5))

    assertEquals(CategoriaRules.NOMBRE_MAX, draft.nombre.length)
    assertEquals(CategoriaRules.DESCRIPCION_MAX, draft.descripcion.length)
    assertNull(draft.nombreError)
  }

  @Test
  fun capNeverKeepsHalfOfAnEmojiAtTheLimit() {
    val pasted = "a".repeat(CategoriaRules.NOMBRE_MAX - 1) + "🏋"

    val draft = CategoryDraftState().onNombre(pasted, suggester)

    assertEquals("a".repeat(CategoriaRules.NOMBRE_MAX - 1), draft.nombre)
  }

  @Test
  fun blankNameIsAnErrorThatShowsOnlyAfterTheUserEditsIt() {
    val pristine = CategoryDraftState.forNew("", suggester)
    val cleared = pristine.onNombre("   ", suggester)

    assertEquals(CategoryNombreError.BLANK, pristine.nombreError)
    assertNull(pristine.visibleNombreError)
    assertFalse(pristine.canSave)
    assertEquals(CategoryNombreError.BLANK, cleared.visibleNombreError)
    assertFalse(cleared.canSave)
  }

  @Test
  fun nameLongerThanTheLimitAfterTrimmingIsTooLong() {
    val draft = CategoryDraftState(nombre = "a".repeat(CategoriaRules.NOMBRE_MAX + 1))

    assertEquals(CategoryNombreError.TOO_LONG, draft.nombreError)
    assertEquals(CategoryNombreError.TOO_LONG, draft.visibleNombreError)
    assertFalse(draft.canSave)
  }

  @Test
  fun editingTheNameClearsTheDuplicateThatBlockedSaving() {
    val existing =
        Categoria("cat_gimnasio", "Gimnasio", "cat_salud", CategoryIcon.DUMBBELL, CategoryTint.ROSE)
    val duplicated = CategoryDraftState.forNew("Gimnasio", suggester).copy(duplicate = existing)

    val edited = duplicated.onNombre("Gimnasio casa", suggester)

    assertFalse(duplicated.canSave)
    assertNull(edited.duplicate)
    assertTrue(edited.canSave)
  }

  @Test
  fun savingBlocksAnotherSave() {
    val draft = CategoryDraftState.forNew("Gimnasio", suggester)

    assertTrue(draft.canSave)
    assertFalse(draft.copy(saving = true).canSave)
  }

  @Test
  fun toDraftTrimsAndStoresABlankDescriptionAsNull() {
    val draft = CategoryDraftState.forNew("  Gimnasio  ", suggester).onDescripcion("   ").toDraft()
    val described =
        CategoryDraftState.forNew("Gimnasio", suggester).onDescripcion("  Pesas  ").toDraft()

    assertEquals(CategoriaDraft("Gimnasio", gym.icono, gym.color, null), draft)
    assertEquals("Pesas", described.descripcion)
  }
}
