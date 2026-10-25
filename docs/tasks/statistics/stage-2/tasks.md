# Stage 2 — Pantalla

## Objetivo

Construir Estadísticas completa como ruta montable y probada sin depender de la barra. El
ViewModel, la pantalla pura, la hoja de detalle, la copy y los estados.

## Tareas

- [**task1 — ViewModel, pantalla y hoja de detalle**](task1/task.md). Es una sola tarea porque la
  pantalla y la hoja comparten estado y ViewModel. Partirla obligaría a dos escritores sobre los
  mismos archivos.

## Criterio de salida

- `StatisticsRoute` se puede montar en el UI catalog y en tests con callbacks falsos.
- Tests host del ViewModel y tests de UI pura verdes.
- Copy en los 4 idiomas.
- `:androidApp:assembleDebug`, compilación iOS y `detekt ktfmtCheck` limpios.

Sin review intermedia: esta stage no fija contratos que otra stage tenga que adivinar.
