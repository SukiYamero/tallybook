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

- [ ] **Step 1** — `grep -rn "Greeting\|GreetingUtil\|class App\b" shared/` — confirmar cero
      resultados (o justificar por qué queda algo).
- [ ] **Step 2** — `./gradlew detekt ktfmtCheck` sobre todo el módulo `shared` (no solo los archivos
      tocados) — cero warnings.
- [ ] **Step 3** — `./gradlew :shared:testAndroidHostTest` — todos los tests de Wave 1/2 en verde.
- [ ] **Step 4** — `./gradlew :shared:iosSimulatorArm64Test` — confirmar que compila y corre en el
      simulador iOS (no es lo mismo que el dispositivo físico, pero confirma que nada de Wave 1-3
      rompió el target iOS).
- [ ] **Step 5** —
      `./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`
      — confirmar que abre sin crashear y se ve la pantalla smoke-test completa (theme + ícono +
      primitiva + navegación).
- [ ] **Step 6** — actualizar `docs/backlog.md`: precisar el ítem de iOS end-to-end si
      `iosSimulatorArm64Test` ya pasa (compila y corre en simulador, sigue pendiente dispositivo
      físico real) — no cerrar el ítem completo.
- [ ] **Step 7** — reportar al usuario para que pruebe en su Android físico (per `docs/README.md`
      paso 6) — no mergear/cerrar sin su ok.

## Bloqueante

Bloqueada por Wave 1, 2 y 3 completas. No bloquea nada — es el final del spec.
