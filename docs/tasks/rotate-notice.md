# Rotate notice

## Contexto

La app está diseñada portrait-first pero sigue siendo rotable (`docs/ui/architecture.md`, "Orientation
support policy"). En teléfonos en landscape (lado corto < 600 dp) se muestra una tarjeta flotante no
bloqueante, `TallybookRotateNotice`, que sugiere girar el teléfono. Se descarta por sesión
(`rememberSaveable`, porque Android recrea la Activity al rotar) y no aplica a tablets.

## Verificación

- Aceptado en Xiaomi físico (Android).
- **Pendiente — iPhone**: confirmar que la tarjeta aparece en landscape y que el descarte se mantiene al
  rotar (iOS no recrea la vista).
- Deuda conocida: no respeta reduced-motion en iOS, no tiene entrada en el UI catalog, la detección usa
  el tamaño de ventana (multi-window en portrait puede disparar el aviso) y la tarjeta no limita su
  alto con fuente muy grande.
