# Cómo se trabaja en este repo

## Ciclo de una feature

1. **Worktree por feature** — `EnterWorktree`, rama `feat/<slug>`, máximo **4 worktrees activos** a la
   vez. Cada worktree tiene su propio `docs/tasks/<slug>.md` (copiar `tasks/_template.md`), con el
   trabajo agrupado en waves.
2. **Los subagentes ejecutan la wave.** Leen `docs/tasks/<slug>.md` para contexto, invocan las skills
   del stack que correspondan (ver `AGENTS.md` §3), y cuestionan el spec si algo no cierra en vez de
   implementarlo a ciegas.
3. **Antes de reportar terminado, el subagente confirma:**
   - `detekt`/`ktfmtCheck` limpios (el hook de `PostToolUse` ya lo fuerza en cada edición).
   - Tests mínimos que aporten valor real, agrupados — no un archivo por caso ni cobertura por
     cobertura.
4. **Code review dedicado.** Un subagente nuevo (rol code-reviewer, skill `code-review` con `--fix`
   sobre el diff de esa feature puntual) busca código espagueti, duplicado, algo reusable en vez de
   escribirlo de nuevo, un enfoque más simple con menos código, bugs o casos no contemplados — y lo
   arregla ahí mismo, no solo lo señala.
5. **El code-reviewer reporta al orquestador** (yo) que lint + tests + review quedaron ok.
6. **Yo te aviso a vos**: "pasate al worktree `<slug>` y probá `<qué>`". Vos das el ok o pedís iterar —
   se vuelve al paso 3 con lo que falte.
7. **Con tu ok**: compacto el spec (saco decisiones/historial, dejo solo lo vigente), actualizo
   `business-logic.md`/`features/<nombre>.md` si aplica, commit, merge a `main`, borro la rama y el
   worktree (`ExitWorktree`).

## Roles

- **Vos** — última palabra: probás en dispositivo físico real y das el ok o pedís cambios. Nada se
  mergea sin tu confirmación.
- **Yo (orquestador)** — reparto el trabajo entre worktrees/subagentes, hago seguimiento proactivo sin
  esperar a que preguntes en qué va cada uno, y soy el único punto que te reporta a vos.
- **Subagentes** — ejecutan una wave dentro de un worktree, usan las skills del stack, cuestionan el
  spec cuando algo no tiene sentido; el de code-review corrige lo que encuentra en vez de solo
  señalarlo.

## Reglas de documentación

- **Una sola referencia por tema.** El mismo dato nunca vive en dos `.md` — se actualiza donde vive, no
  se copia a otro lado.
- **Nada de historial de decisiones.** Git ya es el changelog; un doc describe el estado actual, no cómo
  se llegó a él.
- **Troubleshooting: lo mínimo indispensable.** Solo lo que un agente no puede re-derivar por su cuenta
  (una causa real no obvia), nunca una bitácora de todo lo que se probó.

## Estructura

- `business-logic.md` — reglas de negocio del dominio, la única referencia. Se actualiza en el lugar,
  nunca se bifurca en otro archivo.
- `stack.md` — librerías ya decididas pero sin código todavía, para no re-discutirlas al arrancar la
  feature que las necesita.
- `wording.md` — voz y tono, portado de moneta. Única referencia de cómo se escribe la copy en cada
  idioma.
- `tasks/<slug>.md` — spec activo de una feature en desarrollo (copiar `tasks/_template.md`). Efímero:
  se borra o se compacta a `features/` al terminar, nunca se acumula. Si la feature es grande (varias
  waves, cada una con varias tareas que un subagente necesita ejecutar sin releer todo el spec),
  usar en cambio `tasks/<slug>/` como folder: un `README.md` con el Contexto/alcance general, un
  `wave-N/tasks.md` por wave (overview, qué se necesita a nivel general) y un `wave-N/taskM/task.md`
  por tarea con el detalle completo — objetivo, contexto, archivos, interfaces, pasos de
  implementación, y si bloquea o está bloqueada por otra tarea. Mismo ciclo de vida efímero que la
  versión de un solo archivo. Ver `tasks/fundacion/` como ejemplo real.
- `features/<nombre>.md` — doc chico post-confirmación, solo para features realmente importantes.
- `backlog.md` — pendientes por criticidad, sin historial. Se borra el ítem apenas se resuelve.
