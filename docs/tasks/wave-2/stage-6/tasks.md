# Wave 2 / Stage 6 — Home e integración final

## Objetivo

Conectar bootstrap, repositorios, ViewModels y UI en el primer flujo end-to-end de Tallybook y retirar
la pantalla de smoke test.

## Tareas

- [**task1 — Home reactivo y app bootstrap**](task1/task.md): estado de inicio, balance, recientes,
  vacío, error y apertura del formulario.
- [**task2 — Integración, cleanup y verificación física**](task2/task.md): DI/Nav, eliminación de
  placeholders, test de integración y QA de persistencia; depende de task1.

## Gate de salida

- Instalación limpia, bootstrap, estado vacío y apertura del formulario funcionan.
- Guardar actualiza Home desde SQLDelight sin insertar manualmente en `HomeUiState`.
- Fallar y reintentar bootstrap/lectura/escritura no pierde estado ni duplica datos.
- Gasto e ingreso afectan el balance con signo correcto y solo dentro de su moneda.
- Matar/reabrir el proceso conserva preferencia, categorías y movimientos.
- No quedan copy del smoke test ni destinos placeholder alcanzables.
- Todos los checks globales del README pasan y el reviewer final corrige el diff completo de Wave 2.

## Revisión dedicada

Además del review del stage, un reviewer final lee los seis `tasks.md`, los once task files y el diff
completo desde el cierre de fundación. Solo después de corregir y verificar reporta al orquestador para
la prueba del usuario en dispositivo físico.
