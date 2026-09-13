# Wave 2 / Stage 1 — Locale, preferencias y recursos

## Objetivo

Establecer una sola resolución inicial de idioma/región, persistir la moneda principal y crear los
recursos localizados que consumirán los stages siguientes.

## Tareas

- [**task1 — Locale y moneda por región**](task1/task.md): modelo común, resolución BCP 47 y bindings
  Android/iOS.
- [**task2 — Preferences DataStore**](task2/task.md): fuente reactiva y persistente de
  `monedaPrincipal`; depende de task1.
- [**task3 — Strings de Wave 2**](task3/task.md): recursos `es`, `es-AR`, `en`, `pt-BR`; puede
  ejecutarse en paralelo con task1/task2 porque no toca sus implementaciones.

## Gate de salida

- Tests puros cubren normalización de locale y las ocho regiones monetarias.
- Un test con DataStore real prueba primera inicialización, persistencia y lectura tras recrear el
  repositorio.
- El grafo Koin resuelve `DeviceLocaleProvider` y `AppPreferencesRepository` con una única instancia
  de DataStore por archivo.
- Los cuatro catálogos de recursos tienen exactamente las mismas keys y no contienen el nombre de la
  app web anterior.
- `:shared:testAndroidHostTest`, compilación iOS, `detekt` y `ktfmtCheck` pasan antes del reviewer.

## Revisión dedicada

El reviewer verifica especialmente que región y lenguaje no se confundan, que la moneda inferida se
escriba una sola vez y que errores/cancelación de DataStore no se conviertan silenciosamente en valores
por defecto.
