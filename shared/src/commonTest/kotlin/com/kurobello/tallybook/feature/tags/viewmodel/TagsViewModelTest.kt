package com.kurobello.tallybook.feature.tags.viewmodel

import com.kurobello.tallybook.core.data.AppPreferencesRepository
import com.kurobello.tallybook.core.data.EtiquetaRepository
import com.kurobello.tallybook.core.data.FakeEtiquetaRepository
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.AppPreferencesState
import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.ETIQUETA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {
  @Test
  fun browsesTagsByNameWithPrimaryMonedaCountsDefaultingToZero() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.counts.value =
        mapOf(Moneda.COP to mapOf(EMPRESA.id to 3L), Moneda.USD to mapOf(VIAJE.id to 7L))
    val vm = fixture.create()
    runCurrent()

    val cop = assertIs<TagsContent.Browse>(vm.uiState.value.content)
    assertEquals(listOf("Ábaco", "empresa", "Viaje Chile"), cop.rows.map { it.etiqueta.nombre })
    assertEquals(listOf(0L, 3L, 0L), cop.rows.map { it.count })

    fixture.preferences.moneda.value = Moneda.USD
    runCurrent()
    val usd = assertIs<TagsContent.Browse>(vm.uiState.value.content)
    assertEquals(listOf(0L, 0L, 7L), usd.rows.map { it.count })
  }

  @Test
  fun searchKeepsNormalizedMatchesAndReportsAnEmptyResult() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onQueryChanged("  ABA ")
    val abaco = assertIs<TagsContent.Search>(vm.uiState.value.content)
    assertEquals("ABA", abaco.query)
    assertEquals(listOf(ABACO.id), abaco.matches.map { it.etiqueta.id })

    vm.onQueryChanged("nada")
    val none = assertIs<TagsContent.Search>(vm.uiState.value.content)
    assertEquals("nada", none.query)
    assertTrue(none.matches.isEmpty())

    vm.onQueryChanged("")
    assertEquals(3, assertIs<TagsContent.Browse>(vm.uiState.value.content).rows.size)
  }

  @Test
  fun anEmptyCatalogBrowsesNoRows() = runTest {
    val fixture = TagsFixture(this, initial = emptyList())
    val vm = fixture.create()
    runCurrent()

    assertEquals(TagsContent.Browse(emptyList()), vm.uiState.value.content)
  }

  @Test
  fun aFailedSourceRefusesSheetsAndRetryReloads() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.fake.failure = DataError.Local(IllegalStateException())
    val vm = fixture.create()
    runCurrent()
    assertEquals(TagsContent.Failed, vm.uiState.value.content)
    assertFalse(vm.onOpenSheet(TagSheetRequest.New("")))

    fixture.etiquetas.fake.failure = null
    vm.onRetry()
    runCurrent()
    assertIs<TagsContent.Browse>(vm.uiState.value.content)
  }

  @Test
  fun onlyOneSheetOpensAtATimeAndAnUnknownTagOpensNone() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.counts.value = mapOf(Moneda.COP to mapOf(EMPRESA.id to 2L))
    val vm = fixture.create()
    runCurrent()

    assertFalse(vm.onOpenSheet(TagSheetRequest.Open("missing")))
    assertTrue(vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id)))
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(EMPRESA, sheet.editing)
    assertEquals("empresa", sheet.nombre)
    assertEquals(2L, sheet.count)
    assertFalse(vm.onOpenSheet(TagSheetRequest.Open(VIAJE.id)))

    fixture.etiquetas.counts.value = mapOf(Moneda.COP to mapOf(EMPRESA.id to 5L))
    runCurrent()
    assertEquals(5L, vm.uiState.value.sheet?.count)
  }

  @Test
  fun creatingATagSavesTheTrimmedNameClosesAndReportsIt() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    assertTrue(vm.onOpenSheet(TagSheetRequest.New(" Gym ")))
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertNull(sheet.editing)
    assertEquals("Gym", sheet.nombre)
    assertNull(sheet.visibleNombreError)

    vm.onNombreChanged("Gimnasio ")
    vm.onSaveSheet()
    runCurrent()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(listOf<TagsEffect>(TagsEffect.Feedback(TagsFeedback.CREATED)), effects)
    vm.onSheetHidden()
    assertNull(vm.uiState.value.sheet)
    assertEquals(
        listOf("Ábaco", "empresa", "Gimnasio", "Viaje Chile"),
        assertIs<TagsContent.Browse>(vm.uiState.value.content).rows.map { it.etiqueta.nombre },
    )
  }

  @Test
  fun aNewSheetHidesTheBlankErrorUntilTheNameIsEdited() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.New(""))
    val pristine = assertNotNull(vm.uiState.value.sheet)
    assertNull(pristine.visibleNombreError)
    assertFalse(pristine.canSave)
    vm.onNombreChanged("a")
    vm.onNombreChanged(" ")
    assertEquals(TagNombreError.BLANK, vm.uiState.value.sheet?.visibleNombreError)
  }

  @Test
  fun creatingADuplicateOffersTheExistingTagInsteadOfWriting() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.counts.value = mapOf(Moneda.COP to mapOf(EMPRESA.id to 4L))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.New("EMPRESA"))
    vm.onSaveSheet()
    runCurrent()
    val duplicate = assertNotNull(vm.uiState.value.sheet)
    assertEquals(EMPRESA, duplicate.duplicate)
    assertFalse(duplicate.canSave)
    assertFalse(duplicate.isClosing)
    assertEquals(3, fixture.etiquetas.fake.etiquetas.value.size)
    assertTrue(effects.isEmpty())

    vm.onOpenDuplicate()
    val opened = assertNotNull(vm.uiState.value.sheet)
    assertEquals(EMPRESA, opened.editing)
    assertEquals("empresa", opened.nombre)
    assertEquals(4L, opened.count)
    assertNull(opened.duplicate)
  }

  @Test
  fun editingTheNameClearsTheDuplicate() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.New("empresa"))
    vm.onSaveSheet()
    runCurrent()
    vm.onNombreChanged("empresa 2")
    assertNull(vm.uiState.value.sheet?.duplicate)
    assertTrue(vm.uiState.value.sheet?.canSave == true)
  }

  @Test
  fun openingAStoredNameLongerThanTheLimitKeepsItAndShowsTheErrorAtOnce() = runTest {
    val legacy = Etiqueta("t_legacy", "x".repeat(CategoriaRules.NOMBRE_MAX + 5), CREATED_AT)
    val fixture = TagsFixture(this, initial = listOf(legacy))
    val vm = fixture.create()
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.Open(legacy.id))

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(legacy.nombre, sheet.nombre)
    assertEquals(TagNombreError.TOO_LONG, sheet.visibleNombreError)
    assertFalse(sheet.canSave)
  }

  @Test
  fun renameRejectsBlankTooLongAndDuplicateNamesInline() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id))

    vm.onNombreChanged("   ")
    assertEquals(TagNombreError.BLANK, vm.uiState.value.sheet?.visibleNombreError)
    assertFalse(vm.uiState.value.sheet?.canSave == true)
    vm.onNombreChanged("x".repeat(30))
    assertNull(vm.uiState.value.sheet?.visibleNombreError)
    vm.onNombreChanged("x".repeat(31))
    assertEquals(TagNombreError.TOO_LONG, vm.uiState.value.sheet?.visibleNombreError)
    assertFalse(vm.uiState.value.sheet?.canSave == true)
    vm.onSaveSheet()
    runCurrent()
    assertTrue(fixture.etiquetas.updates.isEmpty())

    vm.onNombreChanged("viaje chile")
    vm.onSaveSheet()
    runCurrent()
    assertEquals(VIAJE, vm.uiState.value.sheet?.duplicate)
    assertEquals(
        listOf<Triple<String, String, String?>>(Triple(EMPRESA.id, "viaje chile", null)),
        fixture.etiquetas.updates,
    )
    assertEquals(
        "empresa",
        fixture.etiquetas.fake.etiquetas.value.first { it.id == EMPRESA.id }.nombre,
    )

    vm.onNombreChanged("Oficina")
    vm.onSaveSheet()
    runCurrent()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(listOf<TagsEffect>(TagsEffect.Feedback(TagsFeedback.UPDATED)), effects)
    assertEquals(
        "Oficina",
        fixture.etiquetas.fake.etiquetas.value.first { it.id == EMPRESA.id }.nombre,
    )
  }

  @Test
  fun savingAnUnchangedNameClosesWithoutWriting() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id))
    vm.onNombreChanged(" empresa ")
    vm.onSaveSheet()
    runCurrent()

    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertTrue(fixture.etiquetas.updates.isEmpty())
    assertTrue(effects.isEmpty())
  }

  @Test
  fun editingTheDescriptionSavesItNormalizedAndAnUnchangedOneDoesNotWrite() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.Open(VIAJE.id))
    assertEquals("", vm.uiState.value.sheet?.descripcion)
    vm.onDescripcionChanged(" Viaje  de trabajo ")
    vm.onSaveSheet()
    runCurrent()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(
        listOf<Triple<String, String, String?>>(
            Triple(VIAJE.id, "Viaje Chile", "Viaje de trabajo")
        ),
        fixture.etiquetas.updates,
    )
    assertEquals(listOf<TagsEffect>(TagsEffect.Feedback(TagsFeedback.UPDATED)), effects)
    vm.onSheetHidden()

    vm.onOpenSheet(TagSheetRequest.Open(VIAJE.id))
    assertEquals("Viaje de trabajo", vm.uiState.value.sheet?.descripcion)
    vm.onDescripcionChanged("Viaje de  trabajo ")
    vm.onSaveSheet()
    runCurrent()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    assertEquals(1, fixture.etiquetas.updates.size)
    assertEquals(1, effects.size)
  }

  @Test
  fun aTooLongDescriptionBlocksSavingUntilShortened() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.New("Gym"))
    vm.onDescripcionChanged("x".repeat(ETIQUETA_DESCRIPCION_MAX + 1))
    assertTrue(vm.uiState.value.sheet?.descripcionTooLong == true)
    assertFalse(vm.uiState.value.sheet?.canSave == true)
    vm.onSaveSheet()
    runCurrent()
    assertTrue(fixture.etiquetas.created.isEmpty())

    vm.onDescripcionChanged("x".repeat(ETIQUETA_DESCRIPCION_MAX))
    assertFalse(vm.uiState.value.sheet?.descripcionTooLong == true)
    vm.onSaveSheet()
    runCurrent()
    assertEquals(
        listOf<Pair<String, String?>>("Gym" to "x".repeat(ETIQUETA_DESCRIPCION_MAX)),
        fixture.etiquetas.created,
    )
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
  }

  @Test
  fun aFailedSaveKeepsTheTypedNameWithAnError() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.New("Gym"))
    fixture.etiquetas.failWrites = true
    vm.onSaveSheet()
    runCurrent()

    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertEquals(TagSheetError.SAVE, sheet.error)
    assertEquals("Gym", sheet.nombre)
    assertFalse(sheet.saving)
    assertFalse(sheet.isClosing)
  }

  @Test
  fun deletingCountsEveryMonedaAsksAndDeletesWithFeedback() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.fake.movimientoCounts.value = mapOf(EMPRESA.id to 3L)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id))

    vm.onRequestDelete()
    runCurrent()
    assertEquals(TagDeletion.Confirm(3L), vm.uiState.value.sheet?.deletion)
    vm.onCancelDelete()
    assertNull(vm.uiState.value.sheet?.deletion)

    vm.onRequestDelete()
    runCurrent()
    vm.onConfirmDelete()
    runCurrent()
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertTrue(sheet.isClosing)
    assertNull(sheet.deletion)
    assertEquals(listOf<TagsEffect>(TagsEffect.Feedback(TagsFeedback.DELETED)), effects)
    assertEquals(
        listOf(ABACO.id, VIAJE.id),
        assertIs<TagsContent.Browse>(vm.uiState.value.content).rows.map { it.etiqueta.id },
    )
  }

  @Test
  fun anUnusedTagConfirmsWithAZeroCount() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()
    vm.onOpenSheet(TagSheetRequest.Open(ABACO.id))

    vm.onRequestDelete()
    runCurrent()
    assertEquals(TagDeletion.Confirm(0L), vm.uiState.value.sheet?.deletion)
  }

  @Test
  fun deleteFailuresKeepTheSheetOpenWithAnError() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()
    vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id))

    fixture.etiquetas.failCount = true
    vm.onRequestDelete()
    runCurrent()
    assertNull(vm.uiState.value.sheet?.deletion)
    assertEquals(TagSheetError.DELETE, vm.uiState.value.sheet?.error)

    fixture.etiquetas.failCount = false
    fixture.etiquetas.failDelete = true
    vm.onRequestDelete()
    runCurrent()
    assertNull(vm.uiState.value.sheet?.error)
    vm.onConfirmDelete()
    runCurrent()
    val sheet = assertNotNull(vm.uiState.value.sheet)
    assertNull(sheet.deletion)
    assertEquals(TagSheetError.DELETE, sheet.error)
    assertFalse(sheet.isClosing)
    assertTrue(effects.isEmpty())
    assertEquals(3, fixture.etiquetas.fake.etiquetas.value.size)
  }

  @Test
  fun viewingMovimientosClosesTheSheetAndOpensSearchOnlyOnceItHid() = runTest {
    val fixture = TagsFixture(this)
    fixture.etiquetas.counts.value = mapOf(Moneda.COP to mapOf(EMPRESA.id to 2L))
    val vm = fixture.create()
    val effects = fixture.collectEffects(vm)
    runCurrent()

    vm.onOpenSheet(TagSheetRequest.Open(ABACO.id))
    vm.onViewMovimientos()
    assertEquals(false, vm.uiState.value.sheet?.isClosing)
    vm.onRequestCloseSheet()
    vm.onSheetHidden()

    vm.onOpenSheet(TagSheetRequest.Open(EMPRESA.id))
    vm.onViewMovimientos()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    runCurrent()
    assertTrue(effects.isEmpty())
    vm.onSheetHidden()
    runCurrent()
    assertNull(vm.uiState.value.sheet)
    assertEquals(listOf<TagsEffect>(TagsEffect.OpenSearch(EMPRESA.id)), effects)
  }

  @Test
  fun closingTheSheetWaitsForItToHide() = runTest {
    val fixture = TagsFixture(this)
    val vm = fixture.create()
    runCurrent()

    vm.onSheetHidden()
    vm.onOpenSheet(TagSheetRequest.New("Gym"))
    vm.onSheetHidden()
    assertNotNull(vm.uiState.value.sheet)
    vm.onRequestCloseSheet()
    assertEquals(true, vm.uiState.value.sheet?.isClosing)
    vm.onSheetHidden()
    assertNull(vm.uiState.value.sheet)
    assertTrue(vm.onOpenSheet(TagSheetRequest.New("")))
  }
}

private class TagsFixture(
    private val scope: TestScope,
    initial: List<Etiqueta> = listOf(VIAJE, EMPRESA, ABACO),
) {
  val etiquetas = RecordingEtiquetas(FakeEtiquetaRepository(initial))
  val preferences = FakePreferences()

  fun create(): TagsViewModel =
      TagsViewModel(
          etiquetas = etiquetas,
          preferences = preferences,
          scope = scope.backgroundScope,
      )

  fun collectEffects(vm: TagsViewModel): List<TagsEffect> {
    val effects = mutableListOf<TagsEffect>()
    scope.backgroundScope.launch { vm.effects.collect { effects += it } }
    return effects
  }
}

/** Per-moneda counts and targeted failures over the shared fake's naming rules. */
private class RecordingEtiquetas(val fake: FakeEtiquetaRepository) : EtiquetaRepository by fake {
  val counts = MutableStateFlow<Map<Moneda, Map<String, Long>>>(emptyMap())
  val created = mutableListOf<Pair<String, String?>>()
  val updates = mutableListOf<Triple<String, String, String?>>()
  var failWrites = false
  var failCount = false
  var failDelete = false

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      counts.map {
        DataOutcome.Success(it[moneda].orEmpty())
      }

  override suspend fun create(nombre: String, descripcion: String?): DataOutcome<EtiquetaWrite> {
    if (failWrites) return failure()
    created += nombre to descripcion
    return fake.create(nombre, descripcion)
  }

  override suspend fun update(
      id: String,
      nombre: String,
      descripcion: String?,
  ): DataOutcome<EtiquetaWrite> {
    if (failWrites) return failure()
    updates += Triple(id, nombre, descripcion)
    return fake.update(id, nombre, descripcion)
  }

  override suspend fun countMovimientos(id: String): DataOutcome<Long> =
      if (failCount) failure() else fake.countMovimientos(id)

  override suspend fun delete(id: String): DataOutcome<Unit> =
      if (failDelete) failure() else fake.delete(id)

  private fun failure() = DataOutcome.Failure(DataError.Local(IllegalStateException()))
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

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")
private val VIAJE = Etiqueta("t_viaje", "Viaje Chile", CREATED_AT)
private val EMPRESA = Etiqueta("t_empresa", "empresa", CREATED_AT)
private val ABACO = Etiqueta("t_abaco", "Ábaco", CREATED_AT)
