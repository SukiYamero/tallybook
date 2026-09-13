# Wave 2 / Stage 1 / Task 1 — Locale y moneda por región

## Objetivo

Crear el contrato KMP que lee una vez el language tag del sistema y resuelve, en `commonMain`, el
idioma soportado y la moneda inicial. La plataforma solo entrega el tag; no contiene decisiones de
producto.

## Contexto y skills

- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/wording.md` y
  `docs/tasks/wave-2/README.md`.
- Skills requeridas: `android-dev`, `kmp-boundaries`, `kotlin-api-design`, `android-testing` y
  `test-driven-development`.
- Android usa el primer locale de `LocaleList.getDefault()`; iOS usa el primer elemento de
  `NSLocale.preferredLanguages`. Ambos devuelven BCP 47 y ningún tipo de plataforma sale del source
  set correspondiente.
- No se modifica el locale del sistema ni el resource environment. Esta wave sigue el sistema; el
  selector manual queda fuera de alcance.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/AppLocale.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/DeviceLocale.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/DeviceLocaleProvider.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/LocaleResolution.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/LocaleModule.kt`
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/core/data/AndroidDeviceLocaleProvider.kt`
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/di/LocaleModule.android.kt`
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/core/data/IosDeviceLocaleProvider.kt`
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/di/LocaleModule.ios.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/LocaleResolutionTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/di/LocaleModuleTest.kt`

## Contratos

```kotlin
enum class AppLocale(val languageTag: String) {
  ES("es"),
  ES_AR("es-AR"),
  EN("en"),
  PT_BR("pt-BR"),
}

data class DeviceLocale(val appLocale: AppLocale, val region: String?)

interface DeviceLocaleProvider {
  fun currentLanguageTag(): String
}

internal fun resolveDeviceLocale(languageTag: String): DeviceLocale

internal fun monedaForRegion(region: String?): Moneda
```

`localeModule` sigue el patrón de `databaseDriverModule`: una declaración `expect val` de tipo
`Module` y un binding `actual` por plataforma. El consumidor resuelve la interfaz, nunca la clase
Android/iOS.

## Reglas exactas

- Normalizar `_` a `-`, language en minúscula y region en mayúscula.
- Aceptar tags con script o extensiones, por ejemplo `es-Latn-CO` y `en-US-u-hc-h12`.
- `es-AR` → `AppLocale.ES_AR`; cualquier otro `es-*` → `ES`.
- `pt-BR` → `PT_BR`; otro portugués no soportado cae a `ES` por ahora.
- `en-*` → `EN`; cualquier lenguaje no soportado → `ES`.
- Preservar una región válida aunque el lenguaje no sea soportado: `fr-CA` conserva `CA`.
- Mapeo monetario: `CO→COP`, `MX→MXN`, `AR→ARS`, `CL→CLP`, `BR→BRL`, `US→USD`, `DO→DOP`,
  `PE→PEN`; región ausente/no soportada → `COP`.
- No inferir moneda desde el idioma. `es-US` usa USD y `en-CO` usa COP.

## Implementación TDD

- [ ] Escribir primero tests table-driven para tags simples, tags con script, `_`, región sin idioma
      soportado y tag vacío; confirmar RED.
- [ ] Implementar el parser mínimo sin dependencia de locale externa y confirmar GREEN.
- [ ] Escribir tests table-driven de las ocho regiones y fallback; confirmar RED y después GREEN.
- [ ] Implementar providers finos Android/iOS y los módulos Koin.
- [ ] Probar con un contenedor Koin aislado que el binding host puede sustituirse por un fake y que
      `DeviceLocaleProvider` se resuelve una sola vez por contenedor.
- [ ] Ejecutar `./gradlew :shared:testAndroidHostTest` y
      `./gradlew :shared:compileKotlinIosSimulatorArm64`.
- [ ] Ejecutar `./gradlew detekt ktfmtCheck`.
- [ ] Commit sugerido: `feat(locale): resolve device locale and initial currency`.

## Bloqueos

No depende de otra tarea de Wave 2. Bloquea Stage 1 / task2 y Stage 2 / task1.
