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

- [ ] `./gradlew :shared:testAndroidHostTest`
- [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [ ] Ejecutar los Android device tests relevantes en el dispositivo conectado.
- [ ] `./gradlew detekt ktfmtCheck`
- [ ] `./gradlew :androidApp:assembleDebug`
- [ ] Confirmar con `rg` cero placeholders/copy legacy y cero imports Android/iOS en `commonMain`.
- [ ] Revisar `git diff --check` y `git status --short`; preservar `.claude/` y cambios ajenos.

## Verificación Android física

- [ ] Instalar el APK con el comando de `AGENTS.md` y lanzar la app.
- [ ] En instalación limpia, confirmar moneda regional, Home vacío y 58 categorías disponibles.
- [ ] Intentar submit inválido y confirmar que no cierra ni pierde campos.
- [ ] Crear un gasto y un ingreso; confirmar orden reciente y balance exacto.
- [ ] Cambiar light/dark del sistema y confirmar legibilidad.
- [ ] Probar teclado, selector, date picker, scroll, gesto/scrim/Back del sheet.
- [ ] Matar el proceso, volver a abrir y confirmar persistencia.
- [ ] Capturar screenshot y árbol de elementos si `mobile-mcp` está disponible.

## Review final y entrega

- [ ] Pedir a un reviewer nuevo que compare el diff completo con cada requisito de Wave 2.
- [ ] El reviewer corrige hallazgos, ejecuta nuevamente checks afectados y reporta evidencia.
- [ ] El orquestador informa al usuario qué probar en el dispositivo; no mergea/cierra sin su OK.
- [ ] Commit sugerido después del review: `feat(movements): complete the first local movement flow`.

## Bloqueos

Bloqueada por toda Wave 2. No habilita otra wave hasta review final y aprobación física del usuario.
