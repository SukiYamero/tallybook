# Wave 2 / Stage 4 — Motor del formulario

## Objetivo

Implementar y probar el estado, validación y escritura del alta de movimientos sin depender todavía de
Compose visual.

## Tareas

- [**task1 — Reglas y AddMovimientoViewModel**](task1/task.md): parser de monto, tiempo/ID inyectables,
  `UiState`, `Actions`, efecto de guardado y coordinación con repositorios.

## Gate de salida

- Monto vacío, mal formado, cero, negativo o con demasiados decimales no escribe.
- Fecha futura y categoría ausente no escriben.
- Doble submit mientras hay una escritura en curso produce un solo movimiento.
- Un fallo conserva todos los campos y deja un error durable; éxito emite un solo efecto de cierre.
- La fecha por defecto usa el día local del dispositivo, no el día UTC.
- Tests `commonTest`, test Android host cuando haga falta y compilación iOS pasan con lint/formato.

## Revisión dedicada

El reviewer verifica los cuatro buckets de estado, ausencia de lógica de negocio en Compose, fakes en
vez de mocks y que `Channel` se use solo para el efecto one-shot de cierre.
