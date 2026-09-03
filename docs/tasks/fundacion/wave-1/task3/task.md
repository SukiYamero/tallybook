# Wave 1 / Task 3 — Navigation 3 shell

## Objetivo

Confirmar que Navigation 3 (`org.jetbrains.androidx.navigation3`) resuelve y funciona en Compose
Multiplatform con al menos 2 destinos y back-stack real, con rutas tipadas — antes de que Wave 3 le
cuelgue la pantalla smoke-test.

## Contexto

- Decisión ya tomada en `docs/stack.md`: Navigation 3, oficial, estable en KMP desde Compose
  Multiplatform 1.10 (el proyecto ya está en 1.11.1). Sin librerías de terceros.
- Nada de rutas como `String` — cada destino es un objeto/clase tipada, consistente con la API real de
  Navigation 3.

## Archivos

- Modificar: `gradle/libs.versions.toml` — agregar el artefacto de Navigation 3 (verificar el
  group/artifact/versión exactos en el catálogo oficial de JetBrains para Compose Multiplatform
  1.11.1 — la API es reciente y cambia, no asumir de memoria).
- Modificar: `shared/build.gradle.kts` — agregar la dependencia a `commonMain`.
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/TallybookNavHost.kt`

## Interfaces (lo que Wave 3 va a consumir)

```kotlin
sealed interface Destination
data object Placeholder1 : Destination
data object Placeholder2 : Destination

@Composable
fun TallybookNavHost(startDestination: Destination = Placeholder1)
// Wave 3 reemplaza Placeholder1 por el destino real de la pantalla smoke-test,
// consumiendo la misma función — no crea un NavHost paralelo.
```

## Implementación

- [ ] **Step 1** — agregar la dependencia, correr `./gradlew :shared:compileKotlinMetadata` para
      confirmar que resuelve.
- [ ] **Step 2** — implementar `Destinations.kt` con 2 destinos placeholder.
- [ ] **Step 3** — implementar `TallybookNavHost` con un back stack real (botón "ir a Placeholder2" en
      Placeholder1, botón "volver" en Placeholder2 usando el back-stack de Nav3, no un `if` manual).
- [ ] **Step 4** — verificación manual en Android físico (es plomería de navegación, no hay lógica de
      negocio que testear con TDD acá): instalar, tocar "ir a Placeholder2", confirmar que el botón
      atrás del sistema vuelve a Placeholder1 (prueba de que el back-stack es real).
- [ ] **Step 5** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 6** — commit:
```bash
git add shared/build.gradle.kts gradle/libs.versions.toml \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/
git commit -m "feat(core): add Navigation 3 shell with typed destinations"
```

## Bloqueante

No bloquea otras tareas de Wave 1 ni Wave 2. **Bloquea Wave 3** (la pantalla smoke-test se navega a
través de este shell).
