# Wave 2 / Stage 5 / Task 2 — Selector jerárquico de categorías

## Objetivo

Construir un campo y modal bottom sheet para elegir cualquiera de las categorías predefinidas por
jerarquía o búsqueda, sin introducir creación/edición de categorías.

## Contexto y skills

- Requiere Stage 5 / task1.
- Leer `docs/tasks/wave-2/README.md`, `docs/wording.md`,
  `web/moneta/src/features/tags/CategoryField.tsx`, `CategorySheet.tsx` y
  `web/moneta/src/lib/categoryTree.ts` como referencia de comportamiento.
- Skills requeridas: `android-dev`, `compose-state-and-effects`, `compose-component-design`,
  `android-ux`, `compose-ui-testing-patterns`, `android-testing` y `test-driven-development`.
- Añadir `com.composables:composeunstyled-modal-bottom-sheet:2.9.0`; verificar la versión resuelta y
  la API oficial antes de implementar.

## Archivos

- Modificar: `gradle/libs.versions.toml`
- Modificar: `shared/build.gradle.kts`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookModalBottomSheet.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategoryTree.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategorySearch.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategoryField.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategoryPickerSheet.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategoryTreeTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategorySearchTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/CategoryPickerTest.kt`

## Contratos

```kotlin
data class CategoryTree(
    val roots: List<Categoria>,
    val childrenByParent: Map<String, List<Categoria>>,
)

internal fun buildCategoryTree(categorias: List<Categoria>): CategoryTree

internal fun searchCategorias(categorias: List<Categoria>, query: String): List<Categoria>

@Composable
fun CategoryField(
    selected: Categoria?,
    isError: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun CategoryPickerSheet(
    visible: Boolean,
    categorias: List<Categoria>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)
```

`TallybookModalBottomSheet` posee forma, scrim, ancho máximo, insets, IME y drag handle; el caller
provee contenido variable mediante slot. El estado `ModalBottomSheetState` no sale al ViewModel.

## Reglas exactas

- Primer nivel: las 12 raíces. Una raíz con hijas abre segundo nivel; una sin hijas selecciona.
- Segundo nivel: incluye primero la propia categoría padre como opción general y luego sus hijas.
- Search es plano sobre raíces/hijas, insensible a mayúsculas y diacríticos, y abandona drill-in.
- Query vacía vuelve a la jerarquía; sin resultados muestra `no_categories_found`.
- Cada apertura empieza en raíz, query vacía y sin foco forzado que abra teclado automáticamente.
- Seleccionar invoca `onSelect(id)` una vez y solicita cierre. El ViewModel sigue siendo dueño del ID.
- No mostrar tile Custom, archived ni controles inertes.
- Back dentro del segundo nivel vuelve a raíz; Back desde raíz, scrim y gesto dismiss cierran.
- El contenido scrollea dentro del sheet; drag handle queda fijo y tiene acciones semánticas.
- La hoja tiene pane title localizado y touch targets mínimos de 48dp.
- No duplicar la lista de categorías en estado local; solo guardar query y parent ID efímeros con
  `rememberSaveable`.

## Implementación TDD

- [ ] Tests RED/GREEN de tree: raíces/hijas, orphan como raíz, input vacío y orden estable.
- [ ] Tests RED/GREEN de búsqueda con tildes, mayúsculas, raíz e hija.
- [ ] Añadir dependencia y construir wrapper mínimo de bottom sheet con preview.
- [ ] UI test de apertura inicial, drill-in, Back interno y selección del padre general.
- [ ] UI test de búsqueda plana, empty result y selección de hija.
- [ ] UI test de dismiss por callback y semantics del estado seleccionado.
- [ ] Prueba manual con teclado abierto en Android físico; confirmar que sheet/CTA futuro no quedan
      detrás de IME.
- [ ] Ejecutar tests pertinentes, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(categories): add the predefined category picker`.

## Bloqueos

Bloqueada por Stage 5 / task1. Bloquea Stage 5 / task3.
