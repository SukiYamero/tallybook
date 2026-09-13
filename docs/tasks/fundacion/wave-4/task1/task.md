# Wave 4 / Task 1 — Cleanup y verificación final

## Objetivo

Cerrar la feature: confirmar que no queda código del wizard, que todo el lint/tests están en verde, y
que la app corre en el Android físico — el criterio de "listo" de todo este spec.

## Contexto

Esta tarea no agrega código de producto — es la pasada final antes de pedirle al usuario que pruebe en
su dispositivo, per el flujo de `docs/README.md` (pasos 3-6).

## Archivos

Ninguno nuevo — revisión y limpieza sobre lo ya creado en Wave 1-3.

## Implementación

- [x] **Step 1** — `grep -rn "Greeting\|GreetingUtil\|class App\b" shared/` — confirmar cero
      resultados (o justificar por qué queda algo).
- [ ] **Step 2** — `./gradlew detekt ktfmtCheck` sobre todo el módulo `shared` (no solo los archivos
      tocados) — Detekt tiene cero hallazgos y ktfmt pasa, pero el plugin estable Detekt 1.23.8 emite
      una deprecación de Gradle 10 desde `ReportingExtension.file(String)`.
- [x] **Step 3** — `./gradlew :shared:testAndroidHostTest` — todos los tests de Wave 1/2 en verde.
- [ ] **Step 4** — `./gradlew :shared:iosSimulatorArm64Test` — confirmar que compila y corre en el
      simulador iOS (no es lo mismo que el dispositivo físico, pero confirma que nada de Wave 1-3
      rompió el target iOS). Las fuentes y tests compilan para el target; la ejecución está bloqueada
      porque Xcode no tiene ningún runtime ni simulador instalado.
- [x] **Step 5** —
      `./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`
      — confirmar que abre sin crashear y se ve la pantalla smoke-test completa (theme + ícono +
      primitiva + navegación).
- [x] **Step 6** — actualizar `docs/backlog.md`: precisar por separado la compilación, la ejecución en
      simulador y la validación en dispositivo iOS real — no cerrar el ítem completo mientras quede
      alguno pendiente.
- [x] **Step 7** — reportar al usuario para que pruebe en su Android físico (per `docs/README.md`
      paso 6) — no mergear/cerrar sin su ok.
- [x] **Step 8** — reemplazar el crossfade predeterminado de 700 ms de `NavDisplay` por un
      fade-through compartido entre forward, Back y predictive Back: salida de 90 ms y entrada de
      220 ms con 90 ms de delay. Verificar en dispositivo que el destino entrante no dibuja antes de
      terminar la salida. Fuentes primarias:
      <https://developer.android.com/guide/navigation/navigation-3/recipes/animations> y
      <https://developer.android.com/develop/ui/compose/animation/composables-modifiers>.

## Bloqueante

Bloqueada por Wave 1, 2 y 3 completas. No bloquea nada — es el final del spec.
