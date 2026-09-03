# Fundación técnica

## Contexto

Antes de la primera feature real (movimientos), dejamos sólida la base técnica sobre la que se va a
construir todo lo demás: dominio, persistencia, DI, manejo de errores, navegación y theming — todo
decidido en `docs/stack.md` pero sin código todavía. El scaffolding actual es el wizard KMP de
JetBrains sin tocar (`App.kt`, `Greeting.kt`, etc.), confirmado que compila y corre en Android físico.

Fuera de alcance de este spec (a propósito, no por olvido):
- **Auth Google + Drive** — feature propia, con su propio spec, una vez esta base esté probada.
- **Seed completo de las ~57 categorías** — contenido, no arquitectura; el schema sí soporta la
  jerarquía padre/hija desde ya.
- **Taxonomía detallada de `DataError`** (causas de red granulares, mensajes localizados para el
  usuario) — no hay todavía ninguna pantalla real que muestre errores; se expande cuando aparezca la
  primera (movimientos o Auth+Drive). Anotado en `docs/backlog.md`.

## Waves

- **Wave 1** (paralelo — sin dependencias entre sí):
  - **A. Dominio + dinero**: `Moneda` (8 monedas + código ISO + escala decimal), `TipoMovimiento`
    (GASTO/INGRESO), `Categoria` (id, nombre, padreId?, icono, colorToken), `Movimiento` (tipo,
    monto: Money, fecha, categoriaId, descripcion?), `Money` (`bignum.BigDecimal` + `Moneda`,
    aritmética que exige misma moneda, formateo locale-aware). `DataError` (sealed: `Local`,
    `Unknown` por ahora — `Network`/`Server` se agregan cuando exista wave de red) y `DataOutcome<T>`
    (`Success`/`Failure`) en `core.model`. TDD: aritmética/formateo de `Money`, exhaustividad de
    `DataOutcome`.
  - **B. Base de datos**: agregar SQLDelight a `libs.versions.toml` + wiring de plugin/KSP por
    target. Schema `.sq` (tablas `movimiento`, `categoria`) con migración v1 desde el día 1.
    `DatabaseDriverFactory` `expect`/`actual` (Android/iOS).
  - **C. Navigation 3 shell**: agregar dependencia, rutas tipadas (sin strings), al menos 2 destinos
    placeholder para probar back-stack real.
  - **D. Theme**: portar tokens de color (claro/oscuro) de moneta, Manrope vía compose resources,
    `TallybookTheme` envolviendo `MaterialTheme`.
  - **E. Koin base**: agregar dependencia, `dispatcherModule` (dispatchers inyectados, nunca a pie).

- **Wave 2** (depende de A + B): `MovimientoRepository`/`CategoriaRepository` — interfaces en
  `core.data`, impls sobre SQLDelight que capturan excepciones de plataforma y devuelven
  `DataOutcome<T>` (la única capa que hace catch — nada por encima vuelve a intentarlo). Tipos
  generados por SQLDelight nunca cruzan fuera de `core.database`/`core.data`. TDD contra driver de
  test. `databaseModule` + `repositoryModule` en Koin (depende también de E).

- **Wave 3** (depende de Wave 2 + C/D): pantalla smoke-test (`feature.home`) sin lógica de negocio —
  ViewModel trivial inyectado por Koin, renderizada con `TallybookTheme`, navegada vía el shell de
  Nav3, usando un ícono de `icons-lucide` y una primitiva de `composables:core` (confirmar que ambas
  libs resuelven en Android e iOS). `viewModelModule` + test de `checkModules`/`verify()` de Koin.

- **Wave 4** (final, depende de todo): eliminar placeholders del wizard (`Greeting.kt`,
  `GreetingUtil.kt`, `AppInfo.kt`, contenido viejo de `App.kt`). `./gradlew detekt ktfmtCheck` limpio,
  `:androidApp:assembleDebug`, instalar y lanzar en el Android físico conectado.

## Verificación

- Build limpio de `:androidApp:assembleDebug`, `detekt` y `ktfmtCheck` sin warnings.
- Tests unitarios de Wave 1-A y Wave 2 en verde (`./gradlew :shared:testAndroidHostTest`).
- En el Android físico: la app abre, muestra la pantalla smoke-test con el theme aplicado (colores +
  Manrope), el ícono Lucide y la primitiva de `composables:core` se ven correctamente, y navegar entre
  los 2 destinos del shell de Nav3 funciona (back-stack real, no solo un `if`).
- iOS: queda pendiente de tu verificación manual (`TEAM_ID` en Xcode, Developer Mode en el iPhone,
  Cmd+R) — no es criterio de cierre de este spec, pero si el build de `:shared:iosSimulatorArm64Test`
  pasa, la base ya está lista para cuando la hagas.
