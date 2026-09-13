# Wave 2 / Stage 2 — Catálogo y bootstrap local

## Objetivo

Portar la taxonomía predefinida y dejar una inicialización local idempotente que nunca sobrescriba datos
existentes.

## Tareas

- [**task1 — Catálogo y seed de categorías**](task1/task.md): estructura tipada, nombres desde Compose
  Resources, inserción `INSERT OR IGNORE`, `CategoriaRepository.ensureSeeded` y
  `LocalDataBootstrapper`.

## Gate de salida

- El catálogo contiene 58 IDs únicos, 12 raíces y 46 hijas; cada `padreId` existe.
- Todo icono y tinte tiene una clave soportada por el contrato de UI de Stage 5.
- Ejecutar el bootstrap dos veces conserva 58 filas y no modifica una fila previamente editada.
- Un fallo parcial puede reintentarse: preferencias y categorías son operaciones idempotentes.
- Tests host, compilación iOS, `detekt` y `ktfmtCheck` pasan antes del reviewer.

## Revisión dedicada

El reviewer compara la taxonomía con `web/moneta/src/lib/schema.ts` y los nombres con
`web/moneta/src/lib/seedConfig.ts`, tratándolos como referencia legacy. Ningún string visible puede
quedar duplicado como literal en Kotlin.
