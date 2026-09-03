<!-- Copiar a docs/tasks/<slug>.md al arrancar una feature. Borrar este comentario en la copia. -->

# <Nombre de la feature>

## Contexto

<Por qué se construye esto, en 1-3 líneas. Sin decisiones ni alternativas descartadas — eso queda en la
conversación, no acá.>

## Waves

<Agrupar el trabajo por dependencia real, no por tamaño. Una wave = todo lo que puede ejecutarse en
paralelo (varios subagentes a la vez) porque no depende entre sí; la siguiente wave arranca recién
cuando la anterior está verificada.>

- **Wave 1** (paralelo): <tarea a> · <tarea b>
- **Wave 2** (depende de Wave 1): <tarea c>

## Verificación

<Cómo se confirma que funciona en un dispositivo físico real — el criterio de "listo", no una lista de
QA exhaustiva.>
