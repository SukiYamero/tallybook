# Documentación de este repo

Reglas del workflow de documentación. Reglas del producto van en `business-logic.md` y `features/`, no acá.

## Proceso

1. **Analizar exhaustivo antes de proponer** — leer todo lo relevante del código/dominio antes de escribir
   un spec, no asumir.
2. **Spec para el trabajo** — un plan que los subagentes siguen y pueden cuestionar. Vive en
   `docs/tasks/<slug>.md` mientras la feature está en desarrollo.
3. **Confirmar en un dispositivo físico real** antes de dar la feature por cerrada — no alcanza con que
   compile.
4. **Compactar el spec** una vez confirmado: sacar decisiones tomadas, historial, razones de por qué se
   eligió tal enfoque — dejar solo lo que es cierto hoy. Si la feature es importante, lo que sobrevive pasa
   a `docs/features/<nombre>.md`; si no, el `.md` de `tasks/` se borra.

## Reglas

- **Una sola referencia por tema.** El mismo dato nunca vive en dos `.md` — se actualiza donde vive, no se
  copia a otro lado.
- **Nada de historial de decisiones.** Git ya es el changelog; un doc describe el estado actual, no cómo
  se llegó a él.
- **Troubleshooting: lo mínimo indispensable.** Solo lo que un agente no puede re-derivar por su cuenta
  (una causa real no obvia), nunca una bitácora de todo lo que se probó.

## Estructura

- `business-logic.md` — reglas de negocio del dominio, la única referencia. Se actualiza en el lugar,
  nunca se bifurca en otro archivo.
- `tasks/<slug>.md` — spec activo de una feature en desarrollo. Efímero: se borra o se compacta a
  `features/` al terminar, nunca se acumula.
- `features/<nombre>.md` — doc chico post-confirmación, solo para features realmente importantes.
