# AGENTS.md — Tallybook (KMP)

## 1. Stack

**Kotlin Multiplatform, Compose Multiplatform (UI compartida Android + iOS)**: Kotlin 2.4.10 (K2) ·
Compose Multiplatform 1.11.1 · Material3 1.11.0-alpha07 · AGP 9.0.1 · minSdk 31 / target-compileSdk 36 ·
package `com.kurobello.tallybook`. Lint/formato: **Detekt 1.23.8** (análisis estático) + **ktfmt 0.26.0**
(formateo, reemplaza a ktlint) — config compartida en `config/detekt.yml`.

- `shared/` — módulo Kotlin Multiplatform: toda la UI (Compose Multiplatform) y lógica de negocio
  viven en `commonMain`. `androidMain`/`iosMain` solo para `expect`/`actual` de APIs de plataforma.
- `androidApp/` — entry point Android (una `MainActivity` que monta el Compose de `shared`).
- `iosApp/` — entry point iOS (Xcode project, Swift). Aunque la UI es Compose Multiplatform, este
  target sigue siendo necesario como shell/entry point y para cualquier código nativo iOS puntual.
- DI: **Koin** (no Hilt — Hilt es Android-only y no compila en `commonMain`).
- Red: **Ktor** en `commonMain` (no Retrofit, no compila en iOS).
- Persistencia: **SQLDelight** en `commonMain` (no Room Multiplatform — decidido, no solo por
  continuidad con `native-kmp-migration.md` de moneta: SQL-first da el control de queries/transacciones
  que va a necesitar el motor de sync/outbox, mejor que el ORM por anotaciones de Room). Ninguno de los
  tres tiene código todavía — son decisiones de stack, se implementan cuando arranque la primera feature
  que los necesite.

**Paquetes** (convención a seguir cuando haya código real, no estructura ya creada): feature-vertical
dentro de `commonMain` — `com.kurobello.tallybook.core.{data,model,database,network,ui}` para lo
compartido entre features (`core.model` sin deps de framework), `com.kurobello.tallybook.feature.<x>.
{ui,viewmodel}` por feature, `com.kurobello.tallybook.di` para los módulos de Koin. Kotlin no importa
por path relativo (import por package totalmente calificado), así que el problema de "imports gigantes" de
TS/JS no aplica acá — no hace falta alias.

**Skills instaladas para este stack** (se auto-invocan por contexto, no hace falta pedirlas):
`claude-android-skill` (arquitectura Android general), `chrisbanes-skills` (Compose state/effects,
performance, coroutines/Flow, Gradle), `android-skills` (KMP: `kmp-boundaries`, `kmp-ktor`, `koin`,
`android-data-layer`, testing/debugging). MCP: `mobile-mcp` (control del device físico).

## 2. Comandos — exactos, con flags

- Build debug Android: `./gradlew :androidApp:assembleDebug`
- Test unitario compartido (JVM/Android host): `./gradlew :shared:testAndroidHostTest`
- Test compartido en simulador iOS: `./gradlew :shared:iosSimulatorArm64Test`
- Instalar + lanzar en el Android físico conectado:
  `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`
- iOS: abrir `iosApp/iosApp.xcodeproj` en Xcode y correr con Cmd+R sobre el iPhone físico
  (o `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphoneos build` para solo compilar).
- Lint (cero warnings, todos los módulos): `./gradlew detekt`
- Formato — verificar: `./gradlew ktfmtCheck` · aplicar: `./gradlew ktfmtFormat`

## 3. Estilo de código — con ejemplo, no descripción

Placeholder actual del wizard (`shared/src/commonMain/kotlin/.../App.kt`) — sirve como ejemplo real de
sintaxis Compose Multiplatform en este repo, **no** como patrón de arquitectura a repetir:

```kotlin
@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showContent = !showContent }) { Text("Click me!") }
            AnimatedVisibility(showContent) {
                Text("Compose: ${remember { Greeting().greet() }}")
            }
        }
    }
}
```

El patrón real a seguir para pantallas nuevas es MVVM con split MVI (`UiState` + `Channel` de efectos +
interfaz `Actions`) — ver skill `android-skills:android-dev`, sección "New-project UI convention".

## 4. Reglas de arquitectura

- Una sola dirección de datos: Vista (Compose, `commonMain`) → ViewModel (`StateFlow<UiState>`) →
  Repositorio → fuente de datos.
- **Single Source of Truth, sin excepciones salvo `expect`/`actual`.** Cada dato tiene un único dueño —
  la base local (Room/SQLDelight en `commonMain`) para datos persistidos, el `UiState` del ViewModel
  para estado de pantalla. La red actualiza esa fuente, la UI nunca lee la respuesta de red directo ni
  duplica el mismo dato en dos lugares. En KMP esto se extiende un nivel más: la lógica de negocio se
  escribe una sola vez en `commonMain` para Android e iOS — si algo termina reimplementado por
  plataforma, ya se rompió el patrón. La única bifurcación legítima es `expect`/`actual`: el contrato
  (la firma, el "qué") sigue single-sourced ahí, solo la implementación difiere por plataforma.
- Cero lógica de negocio en la capa de vista; cero Android/iOS-only imports en `commonMain`.
- Nunca `Dispatchers.Main`/`Dispatchers.IO` a pie en código compartido — inyectar el
  `CoroutineDispatcher` (no está garantizado en todos los targets sin los artefactos `-ktx`).
- Specifics de plataforma (biometría, keystore/keychain, push) vía `expect`/`actual`, nunca `if` de
  plataforma en medio de lógica compartida.
- Composables chicos, reutilizables y stateless cuando se pueda — el estado vive en el ViewModel, no en
  la UI.
- UDF (paralelo a React, para pensarlo con el mismo modelo mental): el estado baja (`UiState`), los
  eventos suben (`Actions`) — nunca al revés.
- Compose Multiplatform + `StateFlow`/`Flow` son la única UI/estado de este proyecto — nada de
  XML/View system ni `LiveData`, ni siquiera para un caso puntual Android-only.
- **Comentarios: estrictos.** Solo si explican un *por qué* no deducible del código ni del historial de
  git (`git log`/`git blame` ya son el changelog). Nunca narrar el *qué*, nunca "antes era X", nunca un
  comentario que un lector podría inferir en 5 segundos leyendo la línea de al lado. Antes de escribirlo,
  preguntarse: ¿esto se puede encontrar leyendo el código o el historial? Si sí, no se escribe.

### Antipatrones prohibidos de este stack

| Mal | Bien |
|---|---|
| Retrofit/OkHttp directo en `shared/` | Ktor en `commonMain` (Retrofit es Android-only, no compila iOS) |
| `SharedFlow` para un evento one-shot (navegar, snackbar) | `Channel(Channel.BUFFERED).receiveAsFlow()` — no se pierde en background |
| Un segundo flag `shouldShowX` al lado de uno ya existente en el mismo ViewModel | Generalizar el mecanismo existente, no duplicarlo |
| Comentar cada línea o restatear lo que ya dice el código | Comentar solo la excepción no obvia (workaround, constraint, gotcha) |

## 5. Límites — qué el agente nunca debe tocar

- `iosApp/Configuration/Config.xcconfig` → `TEAM_ID` (firma personal de Xcode, se completa a mano una
  vez logueado con el Apple ID) y cualquier `*.mobileprovision`/certificado.
- `local.properties`, `*.keystore`, `.env` si se agregan más adelante.
- Contenido interno de `iosApp.xcodeproj/project.pbxproj` — editar desde Xcode, no a mano.

## 6. Loop de verificación

`./gradlew detekt ktfmtCheck` antes de dar por terminado cualquier cambio de Kotlin — no es opcional,
es lo que separa "compila" de "cumple la convención del repo". Build angosto primero:
`./gradlew :androidApp:assembleDebug` o el módulo tocado, antes de un build completo. Evidencia final
sobre el dispositivo físico real (no emulador): `adb install` + `adb shell am
start` en Android, Cmd+R en Xcode sobre el iPhone en iOS — o vía MCP `mobile-mcp`
(`mobile_take_screenshot`, `mobile_list_elements_on_screen`) para verificación visual sin salir de la
sesión.

## 7. Git

- Branches: `feat/`, `fix/`.
- Commits: conventional commits (`feat:`, `fix:`, `chore:`).
- Este AGENTS.md se actualiza en el mismo PR que cambia una convención.

## 8. Workflow de desarrollo y documentación

Ciclo completo (worktrees, roles, code review dedicado, compactación) en `docs/README.md` — no
duplicado acá. Resumen: una feature por worktree (máximo 4 a la vez), subagentes ejecutan por waves y
un code-reviewer dedicado corrige antes de pedir tu ok en dispositivo real; `docs/business-logic.md` es
la única referencia de reglas de negocio, `docs/tasks/<slug>.md` el spec activo (efímero, se compacta al
cerrar la feature).
