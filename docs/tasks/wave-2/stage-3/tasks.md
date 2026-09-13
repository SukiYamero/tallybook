# Wave 2 / Stage 3 — Persistencia reactiva de movimientos

## Objetivo

Convertir el repositorio puntual de fundación en la fuente observable que Home y el formulario pueden
usar sin refrescos manuales ni estado duplicado.

## Tareas

- [**task1 — Migración, queries y repositorios reactivos**](task1/task.md): `createdAt`, resumen por
  moneda, recientes, observación de categorías, extensión coroutines y taxonomía de errores necesaria.

## Gate de salida

- La migración preserva filas de schema 1 y asigna un `created_at` determinista a datos existentes.
- Un collector activo recibe lista/resumen inicial y una nueva emisión después de insertar.
- El orden es `fecha DESC`, `created_at DESC`, `id DESC`.
- Monedas distintas nunca se suman juntas; vacío produce ceros de la moneda solicitada.
- Las excepciones se convierten en `DataOutcome.Failure` y la cancelación continúa propagándose.
- Tests host, verificación de migraciones, compilación iOS, `detekt` y `ktfmtCheck` pasan.

## Revisión dedicada

El reviewer rechaza cualquier `Flow` que solo envuelva `executeAsList()` una vez, cualquier suma en
`Double`, cualquier refresh manual desde UI y cualquier fila SQLDelight fuera de `core.data`.
