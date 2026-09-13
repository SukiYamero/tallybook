# Wave 2 — Primer movimiento local

> **Para subagentes implementadores:** ejecutar una sola tarea por contexto fresco mediante
> `subagent-driven-development`; completar TDD y checks antes de pedir el review del stage. Un reviewer
> distinto corrige el diff antes de habilitar el stage siguiente.

**Goal:** crear, persistir y mostrar el primer movimiento local de Tallybook.

**Architecture:** bootstrap app-level; SQLDelight/DataStore como fuentes de verdad; repositorios
reactivos; ViewModels compartidos; pantallas Compose puras con UDF.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Material3 1.11.0-alpha07, SQLDelight
2.3.2, Koin 4.2.2, DataStore 1.2.1 y Compose Unstyled 2.9.0.

**Spec:** este archivo junto con `docs/business-logic.md`, `docs/stack.md` y `docs/wording.md`.

## Objetivo

Convertir la fundación técnica en el primer flujo de producto usable de Tallybook: detectar la
configuración regional inicial, preparar las categorías, registrar un gasto o ingreso en SQLDelight y
verlo reflejado inmediatamente en Home. Al reiniciar la app, el movimiento y la moneda principal deben
seguir disponibles.

`Tallybook` es el nombre de trabajo de esta app. La aplicación web anterior se consulta como referencia
de reglas, copy y experiencia, pero su nombre no se porta a código, recursos ni UI nuevos.

## Punto de partida

La fundación ya entrega `Money`, `Movimiento`, `Categoria`, `DataOutcome`, SQLDelight, repositorios
puntuales, Koin, Navigation 3, el theme y una pantalla Home de smoke test. Esta wave ensancha esos
contratos solo cuando existe un consumidor real:

- `MovimientoRepository` pasa de snapshots manuales a observación reactiva.
- La moneda principal tiene un único dueño persistente en Preferences DataStore.
- Las categorías predefinidas se insertan de manera idempotente en la base local.
- Home y el formulario consumen modelos de UI, nunca filas generadas por SQLDelight.
- Un insert exitoso invalida la query observable; la UI no agrega una copia optimista paralela.

## Resultado observable

La wave está terminada cuando, en Android físico:

1. Una instalación limpia abre un Home vacío con la moneda inferida de la región del dispositivo.
2. El botón de agregar abre el formulario de movimiento.
3. El usuario elige gasto/ingreso, escribe un monto positivo, elige fecha y categoría y guarda.
4. El formulario solo se cierra después de que SQLDelight confirma la escritura.
5. Home actualiza balance y movimientos recientes sin recarga ni llamada manual a `refresh`.
6. Al matar y volver a abrir el proceso, el movimiento sigue visible.
7. Un fallo de lectura o escritura muestra un estado accionable sin perder la entrada del formulario.

## Arquitectura y flujo de datos

```text
DeviceLocaleProvider ──> AppPreferencesRepository (DataStore)
          │
          └────────────> LocalDataBootstrapper ──> CategoriaRepository ──> SQLDelight

AddMovimientoScreen ──actions──> AddMovimientoViewModel ──insert──> MovimientoRepository
        ▲                         │                                  │
        └──────── UiState ────────┘                                  ▼
                                                              SQLDelight query
                                                                     │ invalidation
                                                                     ▼
HomeScreen <── UiState ── HomeViewModel <── Flow de DataOutcome ─────┘
```

- Persistencia local: SQLDelight es la fuente de verdad de movimientos/categorías; DataStore lo es
  únicamente para preferencias pequeñas.
- Estado de pantalla: un `UiState` inmutable por pantalla. Valores derivados son getters, no campos
  copiables que puedan quedar inconsistentes.
- Eventos one-shot: `Channel(Channel.BUFFERED).receiveAsFlow()`. Los errores que requieren que el
  usuario decida o reintente permanecen en estado durable.
- Plataforma: `commonMain` contiene reglas, repositorios, ViewModels y UI. Android/iOS solo leen APIs
  de locale y rutas de archivo detrás de interfaces semánticas con bindings de Koin.
- Tiempo: `Clock` inyectado y `TimeZone.currentSystemDefault()` capturado una vez por operación.
- Identidad: `IdGenerator` inyectable con `Uuid.random()` en producción.
- Errores: los repositorios continúan siendo la frontera; ninguna excepción de SQLDelight/DataStore
  llega al ViewModel.

## Alcance

Incluido:

- `es`, `es-AR`, `en` y `pt-BR` para todo string visible nuevo.
- COP, MXN, ARS, CLP, BRL, USD, DOP y PEN como moneda inicial por región.
- Las 58 categorías predefinidas de la referencia web: 12 padre y 46 hijas.
- Balance general por moneda, lista reciente reactiva y creación local de movimientos.
- Selector jerárquico y búsqueda de categorías predefinidas.
- Estados de inicialización, vacío, carga, error y envío.

Fuera de alcance:

- Google Sign-In, Drive, outbox, sync, perfiles y modo invitado.
- Editar o eliminar movimientos desde la UI.
- Crear, editar o archivar categorías.
- Search, History, filtros por periodo, gráficos y `categorySuggest`.
- Selector manual de idioma/moneda; esta wave solo deja el almacenamiento que lo habilitará.
- Toast global, bottom navigation completa, PIN/biometría, exportación y paginación.

## Stages y bloqueos

- [**Stage 1 — Locale, preferencias y recursos**](stage-1/tasks.md): resolver idioma/región, moneda
  inicial, DataStore y strings localizados. Sin bloqueos fuera de la fundación.
- [**Stage 2 — Catálogo y bootstrap local**](stage-2/tasks.md): portar y sembrar las categorías.
  Depende de Stage 1.
- [**Stage 3 — Persistencia reactiva de movimientos**](stage-3/tasks.md): migración, queries
  observables y repositorios. Depende de Stage 2.
- [**Stage 4 — Motor del formulario**](stage-4/tasks.md): reglas puras y ViewModel. Depende de Stage 3.
- [**Stage 5 — Componentes del flujo**](stage-5/tasks.md): visuales de categorías, selector y sheet de
  alta. Depende de Stage 4.
- [**Stage 6 — Home e integración final**](stage-6/tasks.md): bootstrap de app, Home real, conexión del
  sheet y QA. Depende de Stage 5.

Un stage se ejecuta en el orden declarado por su `tasks.md`. Tareas marcadas como paralelas pueden usar
subagentes distintos; las demás esperan el contrato que consumen.

## Gate de revisión por stage

Antes de avanzar:

1. Cada implementador completa el ciclo RED → GREEN → REFACTOR y los checks de su `task.md`.
2. Un subagente nuevo revisa el diff acumulado contra este README y todos los task files del stage.
3. El reviewer busca y corrige bugs, duplicación, fuentes de verdad paralelas, APIs más grandes de lo
   necesario, imports de plataforma en `commonMain`, pérdidas de cancelación, estado Compose mal
   ubicado, accesibilidad y tests que no demuestren el contrato.
4. El reviewer vuelve a ejecutar los checks del stage y reporta evidencia concreta.
5. El orquestador informa el resultado. El stage siguiente no empieza con hallazgos abiertos.

La revisión no puede debilitar assertions, silenciar warnings ni ampliar alcance para "mejorar" código
no relacionado.

## Convenciones globales de implementación

- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/stack.md` y `docs/wording.md` antes de editar.
- Invocar `android-dev` como baseline y las skills requeridas por cada task file.
- Código, identificadores, tests y comentarios en inglés; conservar el vocabulario de dominio en
  español (`Movimiento`, `Categoria`, `Moneda`, `monto`, `fecha`, `descripcion`).
- No crear una capa de use cases mientras una regla no sea reutilizada por más de un ViewModel o no
  coordine más de un repositorio.
- No exponer tipos SQLDelight, DataStore, Android ni Foundation/Swift fuera de sus implementaciones.
- No usar `Dispatchers.IO/Main` directamente en lógica compartida.
- Un componente reusable acepta `modifier` y lo aplica en su raíz; la pantalla pura recibe estado y
  callbacks, no el ViewModel ni Koin.
- Semantics primero en pruebas UI; `testTag` solo cuando el texto no identifica un nodo de forma
  estable.
- Los nombres con backticks en `commonTest` no usan coma ni caracteres rechazados por Kotlin/Native.
- No introducir datos demo en la base de producción.

## Evidencia de APIs externas

Versiones a fijar en esta wave:

- Preferences DataStore `1.2.1`, estable y KMP; usar
  `androidx.datastore:datastore-preferences-core`.
- SQLDelight `coroutines-extensions` `2.3.2`, igual que el runtime/driver ya resuelto.
- Compose Unstyled `composeunstyled-modal-bottom-sheet` `2.9.0`, compatible con Compose
  Multiplatform 1.11.1; no subir a 2.9.2 porque requiere Compose 1.12.
- Kotlin 2.4.10: `Uuid.random()` y explicit backing fields son estables.

Fuentes primarias que cada tarea sensible debe conservar en su contexto:

- <https://developer.android.com/jetpack/androidx/releases/datastore>
- <https://developer.android.com/reference/kotlin/androidx/datastore/preferences/core/PreferenceDataStoreFactory>
- <https://sqldelight.github.io/sqldelight/latest/2.x/extensions/coroutines-extensions/index.html>
- <https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html>
- <https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html>
- <https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/-companion/random.html>
- <https://composables.com/compose-unstyled/docs/modal-bottom-sheet>

## Verificación global

- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :shared:compileKotlinIosSimulatorArm64`
- `./gradlew detekt ktfmtCheck`
- `./gradlew :androidApp:assembleDebug`
- Instalar el APK, crear al menos un gasto y un ingreso, matar/reabrir el proceso y comprobar balance,
  orden reciente y persistencia en Android físico.
- Si existe runtime iOS al cerrar la wave: `./gradlew :shared:iosSimulatorArm64Test`; si continúa
  ausente, mantener el bloqueo exacto en `docs/backlog.md`, sin inferir ejecución desde compilación.
