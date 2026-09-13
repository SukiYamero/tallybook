# Wave 2 / Stage 5 — Componentes del flujo

## Objetivo

Construir las piezas visuales reutilizables y la pantalla pura del alta, manteniendo estado de negocio
en el ViewModel y mecánica efímera de overlays en Compose.

## Tareas

- [**task1 — Visuales de categorías y movimientos**](task1/task.md): iconos/tintes tipados,
  avatar/chip, fila de movimiento y tarjeta de balance.
- [**task2 — Selector jerárquico de categorías**](task2/task.md): field, búsqueda y modal bottom sheet;
  depende de task1.
- [**task3 — Formulario y AddMovimientoSheet**](task3/task.md): monto, tipo, fecha, descripción,
  errores y CTA; depende de task2 y del ViewModel de Stage 4.

## Gate de salida

- Todos los componentes aceptan `modifier` en la raíz y exponen callbacks/slots mínimos.
- La selección de categoría funciona en raíz, hijo y búsqueda; cada apertura reinicia navegación y
  búsqueda.
- El sheet se puede cerrar con gesto, scrim y Back; el teclado no tapa el CTA ni desplaza el overlay de
  forma incorrecta.
- Touch targets, pane title, labels, roles, estado seleccionado y errores tienen semantics.
- Tests UI prueban estado/callbacks sin Koin ni ViewModel; screenshot solo si semantics no cubre el
  contrato visual.
- Android host/device tests pertinentes, compilación iOS, lint y formato pasan.

## Revisión dedicada

El reviewer compara comportamiento con la referencia legacy, pero exige interacción nativa
Android/iOS cuando el patrón web no corresponde. Revisa ownership de estado, API de componentes,
accesibilidad, IME, scroll y dismiss paths.
