# Wave 2 / Stage 6 / Task 2 — Integración, cleanup y verificación física

## Objetivo

Cerrar Wave 2 con un contrato de integración real, retirar placeholders de fundación, ejecutar todos
los checks y entregar un diff revisado para prueba del usuario en Android físico.

## Contexto y skills

- Requiere Stage 6 / task1 y todos los stages anteriores revisados.
- Leer los seis `tasks.md`, los once task files, `docs/README.md`, `docs/backlog.md` y el diff completo
  desde el cierre de fundación.
- Skills requeridas para el implementador: `android-dev`, `android-testing`,
  `verification-before-completion` y `requesting-code-review`.
- El reviewer final es un subagente nuevo. Debe usar las skills Android específicas del código que
  inspeccione, corregir hallazgos y volver a verificar; no basta con producir una lista.

## Archivos

- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/TallybookNavHost.kt`
- Modificar: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/navigation/DestinationSerializationTest.kt`
- Crear: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/FirstMovimientoIntegrationTest.kt`
- Modificar: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/navigation/NavigationMotionTest.kt`
  solo si el test aún depende del placeholder; conservar cobertura del contrato de transición útil.
- Modificar: `docs/backlog.md` — borrar solo ítems resueltos y mantener bloqueos reales.
- Actualizar checkboxes/evidencia en `docs/tasks/wave-2/` al cerrar cada task.
- Modificar otros archivos únicamente para corregir hallazgos reales del review de Wave 2.

## Cleanup exacto

- Eliminar `Placeholder2` y toda copy `Probar diálogo`, `Compose Unstyled activo`,
  `Ir al segundo destino`, `Placeholder 2` o `Validación de componentes`.
- `Destination` conserva `Home`; no inventar Search/History vacíos.
- Mantener Navigation 3 como shell aunque hoy tenga un destino: la siguiente pantalla real ampliará el
  back stack.
- Eliminar dependencias/imports del diálogo de smoke solo si ningún componente real las usa. No retirar
  `composeunstyled-dialog` si otra UI de Wave 2 lo consume.
- Buscar strings visibles hardcodeados en los packages nuevos y moverlos a Compose Resources.
- Borrar el ítem `sqldelight:coroutines-extensions` del backlog. Actualizar taxonomía de errores solo en
  la medida realmente implementada; no marcar errores de red resueltos.

## Contrato de integración

`FirstMovimientoIntegrationTest` usa driver SQLDelight en memoria, repositorios y bootstrap reales;
solo reemplaza paths/plataforma, Clock, zona e ID por deterministas. Debe probar:

1. Bootstrap inicializa moneda y 58 categorías.
2. Home observa balance cero y recientes vacíos.
3. El flujo de creación inserta un gasto válido.
4. Sin recrear Home ni llamar refresh, el collector recibe balance negativo y el movimiento reciente.
5. Insertar un ingreso actualiza balance con aritmética exacta.
6. Una moneda diferente no entra al summary activo.
7. Recrear repositorios sobre el mismo almacenamiento conserva datos.

No convertir este test en un full UI test; la UI ya tiene pruebas por contrato. El propósito es la unión
DataStore/bootstrap/repositorios/Flow/ViewModel.

## Verificación automatizada

- [x] `./gradlew :shared:testAndroidHostTest`
- [x] `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [x] Ejecutar los Android device tests relevantes en el dispositivo conectado.
- [x] `./gradlew detekt ktfmtCheck`
- [x] `./gradlew :androidApp:assembleDebug`
- [x] Confirmar con `rg` cero placeholders/copy legacy y cero imports Android/iOS en `commonMain`.
- [x] Revisar `git diff --check` y `git status --short`; preservar `.claude/` y cambios ajenos.

## Verificación Android física

- [x] Instalar el APK con el comando de `AGENTS.md` y lanzar la app.
- [x] En instalación limpia, confirmar moneda regional, Home vacío y 58 categorías disponibles.
- [x] Intentar submit inválido y confirmar que no cierra ni pierde campos.
- [x] Crear un gasto y un ingreso; confirmar orden reciente y balance exacto.
- [x] Cambiar light/dark del sistema y confirmar legibilidad.
- [x] Probar teclado, selector, date picker, scroll, gesto/scrim/Back del sheet.
- [x] Matar el proceso, volver a abrir y confirmar persistencia.
- [x] Capturar screenshot y árbol de elementos si `mobile-mcp` está disponible.

## Review final y entrega

- [x] Pedir a un reviewer nuevo que compare el diff completo con cada requisito de Wave 2.
- [x] El reviewer corrige hallazgos, ejecuta nuevamente checks afectados y reporta evidencia.
- [x] El orquestador informó la evidencia del dispositivo; el usuario aprobó el cierre funcional y dejó
      el rediseño visual para una iteración separada.
- [x] Amend final de Stage 6 sobre `feat(home): complete the first local movement flow`, autorizado
      después de la aprobación funcional explícita del usuario.

## Evidencia de control

- Android device tests: `102/102` GREEN en `aaa9cea2` (M2012K11AG, Android 13) en 61.826 s. Los gates
  host, compilación iOS,
  Detekt, ktfmt y `:androidApp:assembleDebug` están GREEN sobre el árbol actual.
- Instalación limpia en Android físico: Home vacío con USD y 58 categorías; submit inválido conserva el
  formulario; gasto `$12.50` e ingreso `$20.00` dejan balance `$7.50` y orden reciente correcto.
  Light/dark, teclado, selector, date picker, scroll, scrim, gesto, Back y persistencia tras
  force-stop/relaunch quedaron verificados. La base de datos física contiene 58 categorías y 2
  movimientos; hay capturas y XML de accessibility.
- Un reviewer Sol medium revisó y aprobó explícitamente el diff posterior a `3ff8ea3` sin hallazgos.
  El usuario aprobó el cierre funcional el 2026-09-15; la apariencia actual no se considera aprobada y
  se rediseñará en una iteración posterior sin reabrir el alcance funcional de Wave 2.

## Bloqueos

Sin bloqueos funcionales. El rediseño visual queda fuera del cierre de Wave 2.
