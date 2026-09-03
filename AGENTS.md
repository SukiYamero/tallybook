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
- Persistencia/red: aún sin definir — cuando se agregue, red vía **Ktor** (no Retrofit, no compila en
  iOS) y persistencia vía **Room multiplatform** o **SQLDelight** en `commonMain`.

**Skills instaladas para este stack** (se auto-invocan por contexto, no hace falta pedirlas):
`claude-android-skill` (arquitectura Android general), `chrisbanes-skills` (Compose state/effects,
performance, coroutines/Flow, Gradle), `android-skills` (KMP: `kmp-boundaries`, `kmp-ktor`, `koin`,
`android-data-layer`, testing/debugging). MCPs: `mobile-mcp` (control del device físico), `jetbrains`
(contexto de Android Studio, requiere el IDE abierto).

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
- Cero lógica de negocio en la capa de vista; cero Android/iOS-only imports en `commonMain`.
- Nunca `Dispatchers.Main`/`Dispatchers.IO` a pie en código compartido — inyectar el
  `CoroutineDispatcher` (no está garantizado en todos los targets sin los artefactos `-ktx`).
- Specifics de plataforma (biometría, keystore/keychain, push) vía `expect`/`actual`, nunca `if` de
  plataforma en medio de lógica compartida.
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

## 8. Documentación

Workflow completo en `docs/README.md` — no duplicado acá. Resumen: `docs/business-logic.md` es la única
referencia de reglas de negocio; `docs/tasks/<slug>.md` es el spec activo de una feature en desarrollo
(efímero); una vez confirmada en dispositivo real, se compacta (sin historial ni decisiones) y lo que
sobrevive pasa a `docs/features/<nombre>.md` si la feature es importante.
