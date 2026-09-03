# Wave 1 — Fundaciones paralelas

Sin dependencias entre sí — las 5 tareas corren en paralelo, cada una en su propio subagente.

- **task1 — Dominio + dinero**: modelos de negocio puros (`core.model`) + `Money` + error model
  (`DataError`/`DataOutcome`). Ver `task1/task.md`.
- **task2 — Base de datos**: SQLDelight, schema, driver por plataforma. Ver `task2/task.md`.
- **task3 — Navigation 3 shell**: rutas tipadas, back-stack real. Ver `task3/task.md`.
- **task4 — Theme**: tokens de color + Manrope + `TallybookTheme`. Ver `task4/task.md`.
- **task5 — Koin base**: dependencia + `dispatcherModule`. Ver `task5/task.md`.

## Bloqueos

Ninguna tarea de esta wave bloquea a otra de esta wave. **Wave 2** no arranca hasta que task1, task2 y
task5 estén commiteadas y verificadas — task3/task4 no bloquean Wave 2, pero sí Wave 3.
