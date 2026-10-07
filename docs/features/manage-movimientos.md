# Manage movimientos and Undo

The Xiaomi flow is accepted; the reviewed implementation is integrated into `main`.
Home, History and Search open Detail, Edit reuses the existing form, and confirmed deletion offers
Undo through a reusable snackbar. Tags, bulk actions, currency changes, sync and persistent trash
remain outside this delivery.

[Business logic](../business-logic.md#editing-and-deletion) owns mutation and identity rules.
[UI architecture](../ui/architecture.md#movimiento-management-and-feedback) owns presentation,
feedback windows and lifecycle; the [component catalog](../ui/components.md) lists reusable APIs.

## Integration

- `MovimientoRepository` exposes `getById`, typed `update`, transactional `deleteAndCapture` and
  conflict-safe `restoreDeleted`. SQLDelight is the persisted owner; no schema migration is added.
  Existing raw/idempotent delete remains compatible.
- `ManageMovimientoViewModel` and `PrimaryModalCoordinator` integrate shared routes, mutation
  feedback and restoration. The shared form contracts preserve Add's entry and validation behavior.
- Existing repository flows refresh Home/History/Search rather than patching a second list.
  Copy is localized in all existing resource locales.
- Reusable snackbar commit: `c59a4ee`; persistence: `f5262b5`; management integration: `20197ce`.
  The independent reviewer approved the actual feature diff and confirmed corrections.

## Verification boundaries

The modal-corrected tree passed 104 affected host tests across 18 suites, with zero failures,
errors or skips. The final shell callback extraction passed 24 navigation/owner regressions.
Real SQLite fixtures cover identity-safe edits, exact capture/restore, conflict/missing records and
capture eligibility. Owner tests use actual Activity recreation, Koin and SQLite; state tests cover
draft/no-op behavior, stale callbacks, Undo claims, failure/retry and category recovery.

Android production and device-test APK builds, iOS Arm64 production/test compilation, Detekt and
ktfmtCheck pass. Compilation does not establish device-test execution or iOS runtime behavior.

The current production APK was installed and launched on the confirmed Xiaomi M2012K11AG.
Manual checks cover row entry from all three destinations, no forced Edit keyboard, dirty discard,
native Back, complete Detail after Cancel/reread, nested category Back and IME footer removal/return.
The sole authorized temporary movimiento changed expense 123 to income 124, with reactive list updates
and saved feedback. Delete confirmation cancellation preserved it; Delete/Undo restored it.
Undo remained usable after 12 seconds in background and after more than 12 seconds covered by Add.
Final deletion plus Close removed the fixture; retained Search query showed 0 results. No original
user record was modified and no temporary movimiento remains.

The instrumented runner stalled before assertions. One bounded retry after waking the device also
stalled and was stopped; compiled fixtures are not runtime passes. Physical iOS teardown, safe areas,
VoiceOver, TalkBack and large-text snackbar/action usability remain unconfirmed and are tracked in
[the backlog](../backlog.md). User acceptance covers the delivered Xiaomi scope, not those checks.
Future checks follow [proportional verification](../README.md#proportional-verification).

## Supported-version evidence

Resolved common Compose 1.11.1 uses AndroidX UI/runtime 1.11.2 on Android. The implementation uses
Unstyled 2.9.0, Lifecycle 2.11.0-beta01, Koin 4.2.2, SQLDelight 2.3.2 and Navigation3 1.1.1; no dependency
upgrade was required.

- [Compose accessibility timeout contract](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/AccessibilityManager.kt)
  supplies timeout recommendations.
  [Skiko default manager](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/platform/DefaultAccessibilityManager.skiko.kt)
  returns the baseline, so it does not establish VoiceOver timeout adaptation.
- [Lifecycle 2.11.0-beta01 Native sources](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose-iosarm64/2.11.0-beta01/lifecycle-viewmodel-compose-iosarm64-2.11.0-beta01-sources.jar)
  establish the remembered child-owner/provider API used by owner integration tests.
- [Unstyled 2.9.0 Android modal sources](https://repo.maven.apache.org/maven2/com/composables/composeunstyled-modal-android/2.9.0/composeunstyled-modal-android-2.9.0-sources.jar)
  create a ComponentDialog/ComposeView;
  [ComponentDialog 1.13.0 sources](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0-sources.jar)
  install its navigation dispatcher.
  [Navigation-event-compose Android 1.0.2 sources](https://dl.google.com/dl/android/maven2/androidx/navigationevent/navigationevent-compose-android/1.0.2/navigationevent-compose-android-1.0.2-sources.jar)
  resolve that owner through the modal view tree. Physical KEYCODE_BACK confirms the integrated path.
- Modal measurement and focus sources remain in
  [Add's integration contract](add-movimiento.md#layout-integration); the distinct read-mode detents
  have rebuilt-Xiaomi reread evidence.
