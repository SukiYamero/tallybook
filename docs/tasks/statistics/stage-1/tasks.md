# Stage 1 — Fundaciones

## Objetivo

Dejar listas, probadas y sin UI de pantalla las cuatro piezas que la pantalla consume:
- el snapshot de datos;
- las reglas puras;
- los componentes de gráficos;
- el filtro de Search por rango.

## Tareas

- [**task1 — Datos**](task1/task.md): tipos del modelo, consultas SQL y `StatisticsRepository`. Los
  tipos del modelo se commitean primero, porque task2 los consume.
- [**task2 — Reglas**](task2/task.md): selección Mes/Año, tramo comparable, variación, top + "Otras",
  ritmo y "Lo que cambió".
- [**task3 — Gráficos**](task3/task.md): Vico 3.2.3, `TallybookDonut`, `TallybookDistributionRow`,
  `TallybookTrendChart`, `TallybookPaceChart` y `TallybookCategoryTrendChart`, con entradas en el
  catálogo.
- [**task4 — Filtro de Search por rango**](task4/task.md): `SearchFilterRequest` con rango y tipo.

Las cuatro tocan archivos disjuntos (tabla de dueños en el [README](../README.md)). task3 corre en su
propio worktree (`feat/statistics-charts`) en paralelo con task1 → task2 → task4, con 2 builds de
Gradle como máximo. Detalle en el README, sección Ejecución.

## Criterio de salida

- Tests host verdes:
  - consultas y snapshot en `androidHostTest`;
  - reglas en `commonTest`;
  - aplicación del request de Search.
- Componentes de gráficos con previews y entradas en el UI catalog.
- Compilan `:shared:compileKotlinIosSimulatorArm64` y `:androidApp:assembleDebug`.
- `./gradlew detekt ktfmtCheck` limpio.

## Revisión intermedia

Un revisor independiente revisa estos puntos, que fijan el trabajo dependiente:
- el contrato `StatisticsRequest`/`StatisticsSnapshot`;
- la lectura en una sola transacción;
- que ninguna consulta duplique filas por tags;
- la integración de Vico en iOS;
- la suba de coroutines.

Corrige lo que confirme, vuelve a correr solo los checks afectados y reporta. Stage 2 arranca con
cero hallazgos abiertos.
