# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS end-to-end** — nunca se confirmó un build+deploy real al iPhone. Falta: login con Apple ID en
  Xcode, Developer Mode en el iPhone, completar `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`, un
  Cmd+R de prueba.

## Media

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **`initKoin()` a una `Application` propia en Wave 2** — hoy arranca desde `MainActivity.onCreate`, que
  alcanzaba para Wave 1. El `databaseModule` va a necesitar `androidContext()` para `AndroidSqliteDriver`,
  y ese es el momento natural de moverlo. `initKoin(extraModules)` ya acepta la forma.
- **El binding Android de `DatabaseDriverFactory` obliga a un módulo Koin en `androidMain`** —
  `AndroidDatabaseDriverFactory` es `internal` y vive en `androidMain`, así que el módulo que lo declara
  no puede estar en `commonMain/di`. Decidirlo en el spec de Wave 2, no descubrirlo a mitad de la tarea.
- **El `actual` de `ioDispatcher` en iOS no está testeado** — `DispatcherModuleTest` vive en
  `androidHostTest` y afirma `assertNotEquals(io, default)`, que en iOS es falso (ahí `ioDispatcher` es
  `Dispatchers.Default`). Mover el test a `commonTest` requiere reescribir esa aserción.

- **`sqldelight:coroutines-extensions` para queries reactivas** — las queries generadas devuelven
  `Query`, no `Flow`. El primer repositorio de Wave 2 que exponga una lista observable necesita
  `app.cash.sqldelight:coroutines-extensions` (`.asFlow().mapToList(dispatcher)`) en el catálogo. No se
  agregó en Wave 1 a propósito: no tenía consumidor todavía.

## Baja

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
