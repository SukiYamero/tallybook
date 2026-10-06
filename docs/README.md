# Cómo se trabaja en este repo

## Ciclo de una feature

1. **Worktree por feature** — `EnterWorktree`, rama `feat/<slug>`, máximo **4 worktrees activos** a la
   vez. Cada worktree tiene su propio `docs/tasks/<slug>.md` (copiar `tasks/_template.md`), con el
   trabajo agrupado en waves.
2. **Los subagentes ejecutan la wave o el stage activo.** Leen su `task.md` y el overview enlazado,
   invocan las skills del stack que correspondan (ver `AGENTS.md` §3), y cuestionan el spec si algo no
   cierra en vez de implementarlo a ciegas.
3. **Antes de reportar terminado, el subagente confirma:**
   - `detekt`/`ktfmtCheck` limpios (el hook de `PostToolUse` ya lo fuerza en cada edición).
   - Tests mínimos que aporten valor real, agrupados — no un archivo por caso ni cobertura por
     cobertura.
   - El gate de APIs externas de este documento está completo cuando aplica.
4. **One final independent code review per feature.** Review the focused diff for correctness,
   ownership, duplication and simpler alternatives; fix confirmed findings and rerun only affected
   checks. Intermediate reviews are reserved for a material architecture, public contract, migration
   or platform-integration boundary that blocks dependent work. Small features use one stage and one
   final review. Prefer a fresh reviewer when available; if runtime limits require reuse, keep the
   reviewer independent of the implementation and state that its context is reused.
5. **El code-reviewer reporta al orquestador** (yo) que lint + tests + review quedaron ok.
6. **Yo te aviso a vos**: "pasate al worktree `<slug>` y probá `<qué>`". Vos das el ok o pedís iterar —
   se vuelve al paso 3 con lo que falte.
7. **Con tu ok**: compacto el spec (saco decisiones/historial, dejo solo lo vigente), actualizo
   `business-logic.md`/`features/<nombre>.md` si aplica, commit, merge a `main`, borro la rama y el
   worktree (`ExitWorktree`).

## Proportional verification

Choose the smallest meaningful validation scope before implementation:

| Change | Required validation |
| --- | --- |
| Copy, spacing or a reversible visual adjustment | Mandatory Kotlin format/lint when applicable, affected build if compilation can change, and a brief visual check |
| Small behavior or feature | Tests for the changed rules and relevant edge cases, narrow build, one final independent review, and a 3–5 minute physical smoke check of the main flow |
| Shared infrastructure, money/persistence correctness, navigation/lifecycle or a migration | Targeted integration/regression checks for the affected boundary; expand only when evidence or a confirmed failure warrants it |
| Performance, pagination or a query/list sensitive to scale | An explicit bounded volume check with an agreed dataset and observable metrics |

- Keep `detekt` and `ktfmtCheck` mandatory for Kotlin changes. Start with the affected module/tests.
  Run the full suite only for shared integration risk, a confirmed regression or a required repository
  gate. Do not repeat passing checks without a source change or unresolved concern that affects them.
- Volume tests, including 10,000-row fixtures, are **not** a default requirement for small features.
  Functional fixtures use the smallest dataset that covers the relevant cases. Large-list probes
  jump to a few stable-key checkpoints and include a real scroll; they do not traverse every screen.
- A smoke check targets the changed behavior and main user flow. Do not revalidate the full app,
  every locale, every device and every restoration path when those boundaries were not changed.
- If the executor/device setup blocks progress, stop the unchanged run and perform one bounded
  diagnosis of up to five minutes. Use an available manual or targeted alternative, or record the
  blocked check precisely. A real product defect still requires a fix; an unexecuted check is never
  reported as passing. Longer infrastructure debugging needs explicit prioritization as its own task.
- Before a retry, record the concrete changed condition or correction. Do not enlarge timeouts,
  disable cleanup, skip assertions or create repeated benchmark variants to obtain a pass.
- Keep one short handoff: changed behavior, commit, checks actually run, result and material limits.
  Prefer one continuation ledger over detailed reports for every minor task.
- Give agents bounded, named assignments and one owner per source/index/build/device resource.
  Use `followup_task` to start work on an idle subagent; `send_message` alone does not start its turn.
  Internal subagent IDs are execution metadata, not project files or new user-owned work items.
  Reuse suitable agents for bounded work and avoid spawning a separate agent for every routine check.

## Gate de APIs externas

Aplica a APIs recientes, experimentales, sensibles a versión o con comportamiento distinto por
plataforma, como navegación, serialización/restauración, DI al arranque o lifecycle:

1. Confirmar la versión realmente resuelta por Gradle, no solo la solicitada en el catálogo.
2. Verificar el contrato usado contra documentación oficial vigente. Si la documentación no determina
   el comportamiento exacto, consultar el código fuente de esa versión. Registrar el enlace primario en
   el spec activo cuando el contrato no sea obvio.
3. Probar el punto de integración completo. Tests de piezas aisladas no sustituyen el contrato que las
   conecta; por ejemplo, serializers individuales no prueban que el back stack completo se restaure.
4. Identificar qué no puede demostrar un test host y validarlo en el target correspondiente. Compilar un
   APK no demuestra que la app arranque; compilar `commonMain` no demuestra el comportamiento en iOS.

La evidencia debe corresponder al árbol actual y al comportamiento afirmado. Si una validación requerida
no puede ejecutarse, la tarea queda explícitamente sin confirmar; no se infiere éxito desde checks
parciales.

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
- `wording.md` — voz y tono portada desde Moneta, que se conserva únicamente como referencia legacy.
  Es la única referencia de cómo se escribe la copy en cada idioma de Tallybook.
- `ui/architecture.md` — arquitectura visual compartida, componentes, tokens, safe areas, navegación y
  estrategia incremental del rediseño. Es la única referencia durable para implementar o revisar UI.
- `tasks/<slug>.md` — spec activo de una feature en desarrollo (copiar `tasks/_template.md`). Efímero:
  se borra o se compacta a `features/` al terminar, nunca se acumula. Si la feature es grande (varias
  waves, cada una con varias tareas que un subagente necesita ejecutar sin releer todo el spec),
  usar en cambio `tasks/<slug>/` como folder: un `README.md` con el Contexto/alcance general, un
  `wave-N/tasks.md` por wave (overview, qué se necesita a nivel general) y un `wave-N/taskM/task.md`
  por tarea con el detalle completo — objetivo, contexto, archivos, interfaces, pasos de
  implementación, y si bloquea o está bloqueada por otra tarea. Mismo ciclo de vida efímero que la
  versión de un solo archivo. Ver `tasks/fundacion/` como ejemplo real.
- `tasks/wave-N/` — use stages when a large feature has material dependency boundaries. Small
  features use a single stage and final independent review. Add an intermediate stage review only for
  a blocking architecture, contract, migration or platform-integration boundary under the proportional
  verification policy. The execution unit remains `taskN/task.md`; stages do not justify repeated
  checks, reviews or reports for each small change.
- `features/<nombre>.md` — doc chico post-confirmación, solo para features realmente importantes.
- `backlog.md` — pendientes por criticidad, sin historial. Se borra el ítem apenas se resuelve.
