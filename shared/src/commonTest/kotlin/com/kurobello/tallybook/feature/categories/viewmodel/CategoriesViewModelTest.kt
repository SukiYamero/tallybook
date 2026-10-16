package com.kurobello.tallybook.feature.categories.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.UnsupportedCategoriaRepository
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaDelete
import com.kurobello.tallybook.core.model.CategoriaDraft
import com.kurobello.tallybook.core.model.CategoriaOrigen
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.CategoriaWrite
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
  @Test
  fun browsesCustomByNameThenPredefinedFamiliesWithPrimaryMonedaCountsDefaultingToZero() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.counts[Moneda.COP] = mapOf("cat_comida" to 3L, "zz" to 1L)
    fixture.categorias.counts[Moneda.USD] = mapOf("cat_comida" to 9L)
    val vm = fixture.create()
    runCurrent()

    val content = assertIs<CategoriesContent.Browse>(vm.uiState.value.content)
    assertEquals(listOf("Ábaco", "zumo"), content.custom.map { it.categoria.nombre })
    assertEquals(listOf(0L, 1L), content.custom.map { it.count })
    assertEquals(
        listOf(listOf("cat_comida", "cat_cafe"), listOf(CATEGORIA_OTROS_ID)),
        content.families.map { family -> family.map { it.categoria.id } },
    )
    assertEquals(listOf(3L, 0L), content.families.first().map { it.count })

    fixture.preferences.moneda.value = Moneda.USD
    runCurrent()
    val usd = assertIs<CategoriesContent.Browse>(vm.uiState.value.content)
    assertEquals(9L, usd.families.first().first().count)
  }

  @Test
  fun searchShowsOneFlatBlockOfNormalizedMatchesWithTheCustomOnesFirst() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onQueryChanged("  CAFÉ ")
    val cafe = assertIs<CategoriesContent.Search>(vm.uiState.value.content)
    assertEquals("CAFÉ", cafe.query)
    assertEquals(listOf("cat_cafe"), cafe.matches.map { it.categoria.id })

    vm.onQueryChanged("o")
    assertEquals(
        listOf("cus_abaco", "zz", "cat_comida", CATEGORIA_OTROS_ID),
        assertIs<CategoriesContent.Search>(vm.uiState.value.content).matches.map {
          it.categoria.id
        },
    )

    vm.onQueryChanged("nothing")
    assertTrue(assertIs<CategoriesContent.Search>(vm.uiState.value.content).matches.isEmpty())
    vm.onClearQuery()
    assertEquals(2, assertIs<CategoriesContent.Browse>(vm.uiState.value.content).custom.size)
  }

  @Test
  fun failedSourceShowsRetryThatReloads() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.all.value = DataOutcome.Failure(DataError.Local(IllegalStateException()))
    val vm = fixture.create()
    runCurrent()
    assertEquals(CategoriesContent.Failed, vm.uiState.value.content)
    assertEquals(false, vm.onOpenSheet(CategorySheetRequest.New("")))

    fixture.categorias.all.value = DataOutcome.Success(CATEGORIAS)
    vm.onRetry()
    runCurrent()
    assertIs<CategoriesContent.Browse>(vm.uiState.value.content)
  }

  @Test
  fun openingACategoryShowsTheEditorForCustomAndDetailsForPredefinedWithItsCount() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.counts[Moneda.COP] = mapOf("cat_comida" to 3L, "zz" to 1L)
    val vm = fixture.create()
    runCurrent()

    assertTrue(vm.onOpenSheet(CategorySheetRequest.Open("zz")))
    val editor = assertNotNull(vm.uiState.value.editor)
    assertEquals("zz", editor.editing?.id)
    assertEquals(1L, editor.count)
    assertEquals(false, vm.onOpenSheet(CategorySheetRequest.Open("cat_comida")))
    vm.onRequestCloseSheet()
    vm.onSheetHidden()
    assertNull(vm.uiState.value.sheet)

    assertTrue(vm.onOpenSheet(CategorySheetRequest.Open("cat_comida")))
    val details = assertNotNull(vm.uiState.value.details)
    assertEquals("cat_comida", details.categoria.id)
    assertEquals(3L, details.count)
    fixture.categorias.counts[Moneda.COP] = mapOf("cat_comida" to 4L)
    fixture.categorias.recount()
    runCurrent()
    assertEquals(4L, vm.uiState.value.details?.count)
    vm.onRequestCloseSheet()
    vm.onSheetHidden()

    assertEquals(false, vm.onOpenSheet(CategorySheetRequest.Open("missing")))
  }

  @Test
  fun viewingMovimientosClosesTheSheetAndOpensSearchOnlyOnceItHid() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.counts[Moneda.COP] = mapOf("cat_comida" to 3L)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(CategorySheetRequest.Open("cat_cafe"))
    vm.onViewMovimientos()
    assertEquals(false, vm.uiState.value.details?.isClosing)
    vm.onRequestCloseSheet()
    vm.onSheetHidden()

    vm.onOpenSheet(CategorySheetRequest.Open("cat_comida"))
    vm.onViewMovimientos()
    assertEquals(true, vm.uiState.value.details?.isClosing)
    runCurrent()
    assertTrue(effects.isEmpty())
    vm.onSheetHidden()
    runCurrent()
    assertNull(vm.uiState.value.sheet)
    assertEquals(listOf<CategoriesEffect>(CategoriesEffect.OpenSearch("cat_comida")), effects)
  }

  @Test
  fun viewingMovimientosFromAnEditorWithUnsavedChangesAsksBeforeDiscarding() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.counts[Moneda.COP] = mapOf("zz" to 2L)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onDescripcionChanged("Jugos")
    vm.onViewMovimientos()
    assertEquals(true, vm.uiState.value.editor?.confirmingDiscard)
    vm.onContinueEditing()
    assertEquals(false, vm.uiState.value.editor?.opensSearch)
    vm.onRequestCloseSheet()
    vm.onConfirmDiscard()
    vm.onSheetHidden()
    runCurrent()
    assertTrue(effects.isEmpty())

    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onDescripcionChanged("Jugos")
    vm.onViewMovimientos()
    vm.onConfirmDiscard()
    assertEquals(true, vm.uiState.value.editor?.isClosing)
    vm.onSheetHidden()
    runCurrent()
    assertEquals(listOf<CategoriesEffect>(CategoriesEffect.OpenSearch("zz")), effects)
  }

  @Test
  fun createPrefillsTheQueryAndClosesWithFeedbackAfterSaving() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    assertTrue(vm.onOpenSheet(CategorySheetRequest.New(" Gimnasio ")))
    assertEquals("Gimnasio", vm.uiState.value.editor?.draft?.nombre)
    assertEquals(false, vm.onOpenSheet(CategorySheetRequest.New("other")))
    vm.onViewMovimientos()
    assertEquals(false, vm.uiState.value.editor?.isClosing)
    vm.onSaveEditor()
    runCurrent()

    assertEquals(listOf("Gimnasio"), fixture.categorias.created.map { it.nombre })
    assertEquals(true, vm.uiState.value.editor?.isClosing)
    assertEquals(
        listOf<CategoriesEffect>(CategoriesEffect.Feedback(CategoriesFeedback.Created)),
        effects,
    )
    vm.onSheetHidden()
    assertNull(vm.uiState.value.editor)
  }

  @Test
  fun editingIntoADuplicateBlocksSavingUntilOpenSwitchesToTheExistingCategory() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onQueryChanged("zu")

    assertTrue(vm.onOpenSheet(CategorySheetRequest.Open("zz")))
    vm.onNombreChanged("abaco")
    vm.onSaveEditor()
    runCurrent()
    val duplicate = vm.uiState.value.editor?.draft?.duplicate
    assertEquals("cus_abaco", duplicate?.id)
    assertEquals(false, vm.uiState.value.editor?.draft?.canSave)

    vm.onOpenDuplicate()
    runCurrent()
    val editor = assertNotNull(vm.uiState.value.editor)
    assertEquals("cus_abaco", editor.editing?.id)
    assertEquals("Ábaco", editor.draft.nombre)
    assertEquals(false, editor.isClosing)
    assertEquals("zu", vm.uiState.value.query)
    assertTrue(effects.isEmpty())

    vm.onNombreChanged("Ábacos")
    vm.onSaveEditor()
    runCurrent()
    assertEquals(listOf("cus_abaco" to "Ábacos"), fixture.categorias.updated)
    assertEquals(CategoriesEffect.Feedback(CategoriesFeedback.Updated), effects.last())
  }

  @Test
  fun duplicateOfAPredefinedCategoryOpensItsDetailsInTheSameSheet() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.New("Comida"))
    vm.onSaveEditor()
    runCurrent()
    vm.onOpenDuplicate()
    val details = assertNotNull(vm.uiState.value.details)
    assertEquals("cat_comida", details.categoria.id)
    assertEquals(false, details.isClosing)
  }

  @Test
  fun unsavedChangesAskBeforeDiscarding() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onRequestCloseSheet()
    assertEquals(true, vm.uiState.value.editor?.isClosing)
    vm.onSheetHidden()

    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onDescripcionChanged("Jugos")
    vm.onRequestCloseSheet()
    assertEquals(true, vm.uiState.value.editor?.confirmingDiscard)
    vm.onContinueEditing()
    assertEquals(false, vm.uiState.value.editor?.isClosing)
    vm.onRequestCloseSheet()
    vm.onConfirmDiscard()
    assertEquals(true, vm.uiState.value.editor?.isClosing)
  }

  @Test
  fun detailsToggleHidesAndShowsInPlaceButNeverOtros() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onToggleHidden("cat_cafe")
    runCurrent()
    assertTrue(fixture.categorias.visibility.isEmpty())

    vm.onOpenSheet(CategorySheetRequest.Open("cat_cafe"))
    vm.onToggleHidden("cat_cafe")
    assertEquals(true, vm.uiState.value.details?.visibilityPending)
    vm.onToggleHidden("cat_cafe")
    vm.onRequestCloseSheet()
    runCurrent()
    assertEquals(listOf("cat_cafe" to true), fixture.categorias.visibility)
    val details = assertNotNull(vm.uiState.value.details)
    assertTrue(details.categoria.oculta)
    assertEquals(false, details.visibilityPending)
    assertEquals(false, details.isClosing)
    val cafe = assertIs<CategoriesContent.Browse>(vm.uiState.value.content).families[0].last()
    assertTrue(cafe.categoria.oculta)

    vm.onToggleHidden("cat_cafe")
    runCurrent()
    assertEquals(listOf("cat_cafe" to true, "cat_cafe" to false), fixture.categorias.visibility)
    fixture.categorias.failVisibility = true
    vm.onToggleHidden("cat_cafe")
    runCurrent()
    assertEquals(true, vm.uiState.value.details?.visibilityFailed)
    vm.onRequestCloseSheet()
    vm.onSheetHidden()

    fixture.categorias.failVisibility = false
    vm.onOpenSheet(CategorySheetRequest.Open(CATEGORIA_OTROS_ID))
    vm.onToggleHidden(CATEGORIA_OTROS_ID)
    runCurrent()
    assertEquals(2, fixture.categorias.visibility.size)
    assertTrue(effects.isEmpty())
  }

  @Test
  fun editorToggleFollowsTheStoredRowWithoutClosing() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onToggleHidden("zz")
    runCurrent()
    assertEquals(true, vm.uiState.value.editor?.editing?.oculta)
    assertEquals(false, vm.uiState.value.editor?.isClosing)
    assertTrue(effects.isEmpty())

    fixture.categorias.failVisibility = true
    vm.onToggleHidden("zz")
    runCurrent()
    assertEquals(CategoryEditorError.VISIBILITY, vm.uiState.value.editor?.error)
  }

  @Test
  fun deletingAnUnusedCategoryConfirmsThenDeletes() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.Open("cus_abaco"))
    vm.onRequestDelete()
    assertEquals(CategoryDeletion.Checking, vm.uiState.value.editor?.deletion)
    runCurrent()
    assertEquals(CategoryDeletion.Confirm(), vm.uiState.value.editor?.deletion)
    vm.onCancelDelete()
    assertNull(vm.uiState.value.editor?.deletion)

    vm.onRequestDelete()
    runCurrent()
    vm.onConfirmDelete()
    runCurrent()
    assertEquals(listOf("cus_abaco"), fixture.categorias.deleted)
    assertEquals(true, vm.uiState.value.editor?.isClosing)
    assertEquals(CategoriesEffect.Feedback(CategoriesFeedback.Deleted), effects.single())
    assertEquals(
        listOf("zz"),
        assertIs<CategoriesContent.Browse>(vm.uiState.value.content).custom.map { it.categoria.id },
    )
  }

  @Test
  fun aCategoryInUseOffersHidingInsteadWithItsMovimientoCount() = runTest {
    val fixture = CategoriesFixture(this)
    fixture.categorias.movimientoCounts["zz"] = 2L
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.Open("zz"))
    vm.onRequestDelete()
    runCurrent()
    assertEquals(CategoryDeletion.InUse(2L), vm.uiState.value.editor?.deletion)
    vm.onConfirmDelete()
    vm.onHideInstead()
    runCurrent()
    assertTrue(fixture.categorias.deleted.isEmpty())
    assertEquals(listOf("zz" to true), fixture.categorias.visibility)
    assertEquals(true, vm.uiState.value.editor?.isClosing)
    assertEquals(CategoriesEffect.Feedback(CategoriesFeedback.Hidden("zumo")), effects.single())
  }

  @Test
  fun aDeleteRacingANewMovimientoTurnsIntoHideInstead() = runTest {
    val fixture = CategoriesFixture(this)
    val vm = fixture.create()
    runCurrent()
    vm.onOpenSheet(CategorySheetRequest.Open("cus_abaco"))
    vm.onRequestDelete()
    runCurrent()
    fixture.categorias.deleteResult = CategoriaDelete.InUse(1L)
    vm.onConfirmDelete()
    runCurrent()
    assertEquals(CategoryDeletion.InUse(1L), vm.uiState.value.editor?.deletion)
  }
}

private class CategoriesFixture(private val scope: TestScope) {
  val categorias = FakeCategorias()
  val preferences = FakePreferences()

  fun create() =
      CategoriesViewModel(
          categorias = categorias,
          preferences = preferences,
          suggester =
              CategoryAppearanceSuggester(
                  CategoryKeywordDictionary.builtIn,
                  CategorySeedCatalog.categories,
              ),
          scope = scope.backgroundScope,
      )

  fun collectEffects(vm: CategoriesViewModel): List<CategoriesEffect> {
    val effects = mutableListOf<CategoriesEffect>()
    scope.backgroundScope.launch { vm.effects.collect { effects += it } }
    return effects
  }
}

private val CATEGORIAS =
    listOf(
        Categoria("cat_comida", "Comida", null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        Categoria("cat_cafe", "Café", "cat_comida", CategoryIcon.COFFEE, CategoryTint.AMBER),
        Categoria(
            CATEGORIA_OTROS_ID,
            "Otros",
            null,
            CategoryIcon.CIRCLE_DASHED,
            CategoryTint.NEUTRAL,
        ),
        Categoria(
            "zz",
            "zumo",
            null,
            CategoryIcon.TAG,
            CategoryTint.NEUTRAL,
            origen = CategoriaOrigen.USUARIO,
        ),
        Categoria(
            "cus_abaco",
            "Ábaco",
            null,
            CategoryIcon.CALCULATOR,
            CategoryTint.BLUE,
            descripcion = "Cuentas",
            origen = CategoriaOrigen.USUARIO,
        ),
    )

private class FakeCategorias : CategoriaRepository by UnsupportedCategoriaRepository {
  val all = MutableStateFlow<DataOutcome<List<Categoria>>>(DataOutcome.Success(CATEGORIAS))
  val counts = mutableMapOf<Moneda, Map<String, Long>>()
  val movimientoCounts = mutableMapOf<String, Long>()
  private val countRevision = MutableStateFlow(0)
  val created = mutableListOf<CategoriaDraft>()
  val updated = mutableListOf<Pair<String, String>>()
  val visibility = mutableListOf<Pair<String, Boolean>>()
  val deleted = mutableListOf<String>()
  var failVisibility = false
  var deleteResult: CategoriaDelete = CategoriaDelete.Deleted

  fun recount() {
    countRevision.value++
  }

  override fun observeAll(): Flow<DataOutcome<List<Categoria>>> = all

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      countRevision.map {
        DataOutcome.Success(counts[moneda].orEmpty())
      }

  override suspend fun countMovimientos(id: String): DataOutcome<Long> =
      DataOutcome.Success(movimientoCounts[id] ?: 0L)

  override suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite> =
      write(null, draft) {
        created += draft
        Categoria("new", draft.nombre, null, draft.icono, draft.color, draft.descripcion)
      }

  override suspend fun update(id: String, draft: CategoriaDraft): DataOutcome<CategoriaWrite> =
      write(id, draft) {
        updated += id to draft.nombre
        stored().first { it.id == id }.copy(nombre = draft.nombre)
      }

  override suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit> {
    if (failVisibility) return DataOutcome.Failure(DataError.Local(IllegalStateException()))
    visibility += id to oculta
    all.value =
        DataOutcome.Success(stored().map { if (it.id == id) it.copy(oculta = oculta) else it })
    return DataOutcome.Success(Unit)
  }

  override suspend fun delete(id: String): DataOutcome<CategoriaDelete> {
    val result = deleteResult
    if (result == CategoriaDelete.Deleted) {
      deleted += id
      all.value = DataOutcome.Success(stored().filterNot { it.id == id })
    }
    return DataOutcome.Success(result)
  }

  private fun stored(): List<Categoria> = (all.value as DataOutcome.Success).data

  private fun write(
      id: String?,
      draft: CategoriaDraft,
      save: () -> Categoria,
  ): DataOutcome<CategoriaWrite> {
    val key = CategoriaRules.nombreKey(draft.nombre)
    val existing =
        stored().firstOrNull { it.id != id && CategoriaRules.nombreKey(it.nombre) == key }
    return DataOutcome.Success(
        if (existing != null) CategoriaWrite.Duplicate(existing) else CategoriaWrite.Saved(save())
    )
  }
}

private class FakePreferences : AppPreferencesRepository {
  val moneda = MutableStateFlow(Moneda.COP)

  override val preferences: Flow<DataOutcome<AppPreferencesState>> = moneda.map {
    DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(it)))
  }

  override suspend fun initialize(monedaPrincipal: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setMonedaPrincipal(moneda: Moneda) = DataOutcome.Success(Unit)

  override suspend fun setThemeMode(themeMode: ThemeMode) = DataOutcome.Success(Unit)

  override suspend fun setAppLocale(appLocale: AppLocale?) = DataOutcome.Success(Unit)
}
