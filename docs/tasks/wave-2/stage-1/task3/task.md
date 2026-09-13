# Wave 2 / Stage 1 / Task 3 — Strings localizados de Wave 2

## Objetivo

Crear la fuente única de copy visible para el primer movimiento local y para los 58 nombres de
categoría, con paridad de keys en español, español de Argentina, inglés y portugués de Brasil.

## Contexto y skills

- Puede ejecutarse en paralelo con task1/task2; no consume sus implementaciones.
- Leer `docs/wording.md`, `docs/business-logic.md`, `docs/tasks/wave-2/README.md` y la referencia legacy
  `web/moneta/src/lib/i18n/locales/` + `web/moneta/src/lib/seedConfig.ts`.
- Skills requeridas: `android-dev`, `grounded-writing` y `android-testing`.
- Tallybook es el nombre visible temporal. No copiar el nombre de la app anterior ni marcas de su
  UI.

## Archivos

- Crear: `shared/src/commonMain/composeResources/values/strings.xml` — español base.
- Crear: `shared/src/commonMain/composeResources/values-es-rAR/strings.xml`
- Crear: `shared/src/commonMain/composeResources/values-en/strings.xml`
- Crear: `shared/src/commonMain/composeResources/values-pt-rBR/strings.xml`
- Crear: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/resources/Wave2ResourcesTest.kt`

## Catálogo obligatorio

Los cuatro archivos contienen las mismas keys:

- App/bootstrap: `app_name`, `startup_error_title`, `startup_error_body`, `retry`.
- Home: `home_balance`, `home_income`, `home_expenses`, `home_recent`, `home_empty_title`,
  `home_empty_body`, `home_add_first`, `home_load_error_title`, `home_load_error_body`.
- Movimiento: `add_movimiento_pane_title`, `expense`, `income`, `amount`, `date`, `category`,
  `description_optional`, `save_expense`, `save_income`, `close`, `back`, `more_fields`,
  `search_categories`, `no_categories_found`, `unknown_category`, `loading`.
- Validación: `amount_required`, `amount_malformed`, `amount_not_positive`,
  `amount_too_many_decimals`, `category_required`, `future_date_not_allowed`,
  `movimiento_save_error`.
- Categorías: una key formada por `category_` más el ID sin el prefijo `cat_`, para cada uno de los 58
  IDs de `CATEGORIAS_SEMILLA`; por ejemplo `category_comida`, `category_supermercado` y
  `category_reembolso`.

Copy española base que define intención:

- Empty title/body: `Todavía no hay movimientos` / `Agrega tu primer gasto o ingreso para empezar a
  entender tu balance.`
- Error de escritura: `No pudimos guardar el movimiento. Lo que escribiste sigue aquí para que lo
  intentes de nuevo.`
- Error de inicio: `No pudimos preparar tus datos` / `Tus datos existentes siguen guardados. Intenta
  abrirlos de nuevo.`

Las otras traducciones preservan intención y brevedad, no estructura literal. `es-AR` usa voseo.

## Reglas exactas

- Ningún string visible de Stage 2 queda hardcodeado en Kotlin, previews o tests UI.
- Los nombres de categoría se portan de `SEED_CATEGORY_NAMES`; no se traducen nuevamente.
- El default `values/` es español. Los qualifiers siguen la sintaxis Compose Multiplatform, no la de
  carpetas Android `res` inventada por fuera de `composeResources`.
- Placeholders usan el mismo número, orden y tipo en los cuatro idiomas.
- No usar emojis, signos de exclamación decorativos ni jerga técnica.

## Implementación y verificación

- [ ] Crear primero el catálogo español y referenciar todas las keys desde un test/fixture compilable.
- [ ] Portar las 58 traducciones exactas por locale desde `seedConfig.ts`.
- [ ] Escribir las traducciones de UI aplicando `docs/wording.md`.
- [ ] Comparar por script los nombres de key de los cuatro XML: no puede faltar ni sobrar ninguna.
- [ ] Ejecutar generación/compilación de recursos Android e iOS.
- [ ] Buscar `Moneta|KuroBello` dentro de los archivos nuevos y exigir cero resultados.
- [ ] Ejecutar `./gradlew :shared:testAndroidHostTest`, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(resources): add localized copy for the first movement flow`.

## Bloqueos

No tiene bloqueos dentro de Wave 2. Bloquea Stage 2 y toda UI de Stage 5/6.
