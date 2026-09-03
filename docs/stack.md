# Librerías decididas, sin código todavía

Referencia única — no duplicar estas decisiones en otro `.md`. Se implementan cuando arranque la
feature que las necesita, no antes.

- **Navegación**: Navigation 3 (`org.jetbrains.androidx.navigation3`) — oficial, estable en KMP desde
  Compose Multiplatform 1.10, sin dependencia de terceros.
- **Auth Google**: Android → Credential Manager + `googleid` + `play-services-auth`
  (`AuthorizationClient` para los scopes de Drive). iOS → Google Sign-In SDK vía **SPM** (plugin
  `spm4Kmp`, genera un XCFramework) — CocoaPods (`kotlin("native.cocoapods")`) como fallback si la
  integración por SPM da fricción.
- **Google Drive**: sin SDK — no existe uno KMP y los de Google son Android-only. REST directo vía Ktor
  + bearer token, que de todas formas es lo que ya asume el motor de sync (shard read-modify-write).
- **Dinero**: `com.ionspin.kotlin:bignum` — Kotlin no tiene `BigDecimal` en `commonMain`; es la única
  opción KMP madura para precisión decimal. No se calculan montos con `Double`.
- **Logging**: Kermit (`co.touchlab:kermit`) por sobre Napier — más mantenido, con writers nativos por
  plataforma (Logcat/os_log) y hooks de Crashlytics ya resueltos.
- **Íconos**: `com.composables:icons-lucide` — Lucide empaquetado 1:1 para Compose Multiplatform, no
  hace falta bundlear los SVG a mano.
- **Componentes headless**: `com.composables:core` (mismo maker que `icons-lucide`) — primitivas sin
  estilo (`ModalBottomSheet`, `Dialog`, `Popup`, `DropdownMenu`) para skinear con diseño propio, el
  equivalente de Radix (que ya usan en la web) para Compose Multiplatform. Da la plomería de
  accesibilidad/foco/estado y el cableado del gesto de drag — **no trae animaciones ni feeling nativo**,
  eso se diseña encima con las APIs de animación de Compose, igual que ya hacían con el `BottomSheet`
  bespoke de la web.
- **Imágenes / preferencias / listas paginadas**: Coil 3, DataStore, Paging 3 — ya cubiertos por skills
  instaladas (`android-skills:coil-compose`, `android-skills:datastore`, `android-skills:paging`), sin
  nada que decidir.
- **Manrope (fuente) y `BottomSheet`/`Toast`/`PagedGrid`** (componentes bespoke del diseño de moneta):
  no son una librería a elegir — se construyen con primitivas de Compose (`Font`/`FontResource`,
  `Modifier`/`AnimatedContent`), igual que en la versión web.
- **Colores/tokens de tema claro y oscuro**: reusar los ya definidos en moneta
  (`web/moneta/docs/ui/design-tokens.md`), no diseñar unos nuevos — portarlos a Compose Multiplatform
  cuando se arme el theming (`ColorScheme` de Material3 o tokens propios, a definir en esa feature).
