# Statistics / Stage 3 / Task 2 — Docs, review final y aceptación

## Objetivo

Dejar las reglas en sus documentos canónicos y cerrar con una review final independiente y la
aceptación en el Xiaomi.

## Docs

Cada dato vive en un solo lugar y sin historial:

- `docs/business-logic.md`: sección nueva **Statistics**, con las decisiones 3–12 escritas como
  reglas vigentes. En *Tracking and History*, ajustar la frase de navegación para reflejar que
  Search ya no tiene pestaña.
- `docs/ui/architecture.md`, tres cambios:
  - **Tallybook destination rule:** la tabla de la barra pasa a Home · History · Add · Statistics ·
    Profile, Search queda como destino primario sin pestaña con entrada desde Home, y se anota que
    si crecen los destinos se evalúa un menú;
  - una subsección de presentación de Estadísticas;
  - en la paleta `chart`, cómo se asigna por rango.
- `docs/ui/components.md`: los componentes de `core/ui/chart`.
- `docs/stack.md`: Vico 3.2.3, fijado y por qué, junto con la decisión de dona y barras propias.
- `docs/features/README.md`: la fila de Estadísticas, en el estado correspondiente.
- `docs/backlog.md`: sacar el ítem Estadísticas y dejar los pendientes reales que queden
  (verificación en iOS, accesibilidad pendiente).

## Review final

La hace un revisor independiente sobre el diff completo de `feat/statistics` contra `main`:
- corrección: sumas, tramos, monedas y transferencias;
- ownership y una sola fuente de verdad;
- duplicación con Historial;
- performance de recomposición en los gráficos;
- accesibilidad;
- comentarios según `AGENTS.md`.

El revisor corrige lo confirmado, vuelve a correr los checks afectados y reporta.

## Aceptación

- Confirmar un único dispositivo físico (el Xiaomi). Después: `adb install -r ... && adb shell am start ...`.
- Smoke del flujo principal (decisiones 1, 2, 7, 8, 9 y 11):
  - pestaña;
  - tarjeta;
  - Search desde Home;
  - Mes ↔ Año;
  - dimensiones;
  - hoja;
  - "Ver N".
- Además: tema oscuro, texto agrandado y TalkBack sobre los resúmenes.
- Correr los device tests nuevos y modificados con la receta MIUI.
- Reportar al usuario qué probar y qué quedó sin verificar.

## Salida

Docs actualizados, review final cerrada y evidencia en el dispositivo. Con el ok del usuario: se
compacta el spec, se hace el merge y se borran la rama y el worktree.
