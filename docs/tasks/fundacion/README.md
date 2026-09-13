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

## Estructura de este spec

Cada wave vive en su propio folder (`wave-1/`, `wave-2/`...) con un `tasks.md` de overview — qué se
necesita a nivel general, crece a medida que la wave crece — y una subcarpeta por tarea (`task1/`,
`task2/`...) con el detalle completo en `task.md`: objetivo, contexto, archivos, interfaces e
implementación paso a paso. El subagente que ejecuta una tarea solo necesita leer ese `task.md`, no
todo el spec.

## Orden de ejecución / bloqueos

- **Wave 1** — sin bloqueos, las 5 tareas corren en paralelo.
- **Wave 2** — bloqueada por Wave 1 / task1 (dominio) + task2 (base de datos) + task5 (Koin base).
- **Wave 3** — bloqueada por Wave 2 / task1 (repositorios) + Wave 1 / task3 (navegación) + task4 (theme).
- **Wave 4** — bloqueada por todo lo anterior.

## Verificación global

- Build limpio de `:androidApp:assembleDebug`, `detekt` y `ktfmtCheck` sin warnings.
- Tests unitarios de Wave 1 y Wave 2 en verde (`./gradlew :shared:testAndroidHostTest`).
- En el Android físico: la app abre, muestra la pantalla smoke-test con el theme aplicado (colores +
  Manrope), el ícono Lucide y el diálogo de `composeunstyled-dialog` se ven correctamente, y navegar entre
  los 2 destinos del shell de Nav3 funciona (back-stack real, no solo un `if`).
- iOS: queda pendiente tu verificación manual (`TEAM_ID` en Xcode, Developer Mode en el iPhone, Cmd+R)
  — no es criterio de cierre de este spec, pero si `:shared:iosSimulatorArm64Test` pasa, la base ya
  está lista para cuando la hagas.
