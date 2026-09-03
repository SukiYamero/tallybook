# Wave 1 / Task 4 — Theme

## Objetivo

Portar los tokens de color (claro/oscuro) de moneta a Compose Multiplatform y dejar `TallybookTheme`
envolviendo `MaterialTheme`, con Manrope cargada como fuente vía compose resources.

## Contexto

- Fuente de los tokens: `docs/stack.md` dice que son los ya definidos en moneta
  (`web/moneta/docs/ui/design-tokens.md`, solo colores, sin spacing/tipografía). **Leer ese archivo
  real antes de escribir el `ColorScheme`** — no inventar una paleta.
- Manrope: conseguir el archivo de fuente del proyecto moneta (o de Google Fonts si moneta ya lo trae
  embebido) — colocar como recurso en `shared/src/commonMain/composeResources/font/`.
- El theme no define spacing/tipografía custom todavía (fuera de alcance — moneta tampoco tiene esos
  tokens, per `docs/stack.md`). Usar la `Typography` default de Material3 con Manrope como
  `fontFamily`, no reinventar una escala tipográfica.

## Archivos

- Crear: `shared/src/commonMain/composeResources/font/manrope_regular.ttf` (y los pesos que realmente
  use moneta — confirmar cuáles antes de copiar solo uno)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Color.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Typography.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/TallybookTheme.kt`

## Interfaces (lo que Wave 3 va a consumir)

```kotlin
@Composable
fun TallybookTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

## Implementación

- [ ] **Step 1** — leer `web/moneta/docs/ui/design-tokens.md`, extraer la paleta light/dark exacta.
- [ ] **Step 2** — crear `Color.kt` con los `Color(0xFF...)` correspondientes, agrupados en
      `lightColorScheme()`/`darkColorScheme()` de Material3.
- [ ] **Step 3** — agregar las fuentes Manrope a `composeResources/font/`, crear `Typography.kt` con
      `FontFamily(Font(Res.font.manrope_regular, FontWeight.Normal), ...)` aplicado sobre
      `Typography()` default de Material3.
- [ ] **Step 4** — implementar `TallybookTheme` combinando ambos.
- [ ] **Step 5** — no hay lógica que testear con TDD (son tokens estáticos) — se confirma visualmente
      en Wave 3 cuando la pantalla smoke-test se renderiza con este theme.
- [ ] **Step 6** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 7** — commit:
```bash
git add shared/src/commonMain/composeResources/font/ \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/
git commit -m "feat(core): port moneta color tokens and Manrope theme"
```

## Bloqueante

No bloquea otras tareas de Wave 1 ni Wave 2. **Bloquea Wave 3** (la pantalla smoke-test se renderiza
con este theme).
