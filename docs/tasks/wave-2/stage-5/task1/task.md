# Wave 2 / Stage 5 / Task 1 — Visuales de categorías y movimientos

## Objetivo

Crear los componentes pequeños compartidos que resuelven icono/tinte de categoría y representan un
balance o movimiento sin conocer ViewModels, repositorios ni navegación.

## Contexto y skills

- Requiere Stage 4 revisado, aunque puede trabajar con fixtures puros.
- Leer `AGENTS.md`, `docs/stack.md`, `docs/tasks/wave-2/README.md`,
  `web/moneta/src/components/shared/categoryIcons.ts`, `tintClasses.ts`, `MovimientoRow.tsx` y
  `web/moneta/src/features/home/BalanceCard.tsx` como referencias legacy.
- Skills requeridas: `android-dev`, `compose-component-design`, `android-ux`,
  `compose-ui-testing-patterns`, `android-testing` y `test-driven-development`.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/category/CategoryIconVector.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/category/CategoryTintColors.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/category/CategoryAvatar.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/category/CategoryChip.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/movimiento/MovimientoRow.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/movimiento/BalanceCard.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/category/CategoryPresentationTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/movimiento/MovimientoComponentsTest.kt`

## Contratos de componentes

```kotlin
@Composable
fun CategoryAvatar(categoria: Categoria, modifier: Modifier = Modifier)

@Composable
fun CategoryChip(
    categoria: Categoria,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun MovimientoRow(
    movimiento: Movimiento,
    categoria: Categoria?,
    modifier: Modifier = Modifier,
)

@Composable
fun BalanceCard(summary: MovimientoSummary, modifier: Modifier = Modifier)
```

El mapping `CategoryIcon → ImageVector` es exhaustivo y vive en un solo archivo. El mapping
`CategoryTint → colores` usa `MaterialTheme`/`LocalTallybookColors`; ningún hex nuevo ni copia de la
paleta se introduce aquí.

## Reglas exactas

- `modifier` se aplica en la raíz y el caller conserva placement/padding externo.
- `CategoryChip` expone role/selected/click semantics y mínimo 48dp táctil.
- `CategoryAvatar` marca el icono como decorativo cuando el nombre adyacente ya describe la categoría.
- Tintes: emerald/blue/amber/rose/purple usan chart 1–5; success/danger/info usan status; el contrato
  queda exhaustivo para las ocho variantes del catálogo.
- `MovimientoRow` muestra nombre localizado; si la categoría ya no resuelve, usa
  `unknown_category`, un icono/tinte neutral y nunca el ID crudo.
- El signo se deriva de `TipoMovimiento`: ingreso positivo, gasto negativo. `Money.minorUnits` sigue
  siendo positivo en storage.
- `BalanceCard` presenta balance, ingresos y gastos; no recalcula sumas desde una lista.
- No agregar callback de abrir detalle: editar/ver movimientos está fuera de esta wave.
- Previews usan datos fijos, theme claro/oscuro y no construyen Koin.

## Implementación TDD

- [ ] Test puro RED/GREEN que obliga a cubrir todos los enums de icono/tinte definidos por Stage 2.
- [ ] Implementar mappings y componentes mínimos con previews.
- [ ] UI test de `CategoryChip`: label visible, selected semantics y callback único.
- [ ] UI test de `MovimientoRow`: gasto/ingreso muestran signo correcto y missing category muestra
      fallback localizado.
- [ ] UI test de `BalanceCard`: tres montos correctos desde un summary fijo.
- [ ] Verificar visualmente light/dark; usar screenshot test solo si color/layout no queda cubierto por
      inspección y semantics.
- [ ] Ejecutar tests pertinentes, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(ui): add category and movement presentation components`.

## Bloqueos

Bloqueada por Stage 4. Bloquea Stage 5 / task2 y Home de Stage 6.
