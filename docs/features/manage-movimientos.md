# Manage movimientos and Undo

The direct movement editor is delivered and accepted for the verified Xiaomi scope.
Home, History and Search open the prefilled editor using the existing form, and confirmed deletion offers
Undo through a reusable snackbar with queued delivery. Bulk actions, currency changes, sync and
persistent trash remain outside this delivery.

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
- Feedback stays in a caller-owned FIFO queue. The shared snackbar host owns its visible window,
  monotonic deadline and backlog compaction; the component catalog exercises the same delivery policy.

## Verification boundaries

Targeted host regressions cover direct editor entry, clean/dirty closing, normalized no-op Save,
confirmation admission, draft-preserving deletion failure/cancellation, queue order, per-notification
Undo ownership, failure/retry, stale callbacks, atomic claims, lifecycle admission and retained owners.
Virtual-time tests cover active
deadline shortening, complete windows for queued messages, last-message compaction, empty-queue reset,
hidden/busy suspension, accessibility recommendations and single timeout delivery. Real SQLite
fixtures cover identity-safe edits, exact capture/restore, conflict/missing records and eligibility.

Android debug builds, Android device-test compilation, iOS Arm64 shared-source compilation, Detekt
and ktfmtCheck pass. The final independent review has no pending findings. Compilation does not
establish device-test execution or iOS runtime behavior.

The adjusted APK is verified on the confirmed Xiaomi M2012K11AG for direct row entry, header deletion
confirmation/cancellation, the soft-red Trash background, footer placement, keyboard transitions and
clean/dirty closing. Creation's default form remains available. This read-only smoke does not mutate
business records; Delete/Undo
mutation and FIFO ownership are covered by targeted host fixtures.

The instrumented runner remains blocked before assertions; compiled fixtures are not runtime passes.
Physical iOS teardown, safe areas, VoiceOver, TalkBack and large-text snackbar/action usability remain
unconfirmed and are tracked in [the backlog](../backlog.md). Acceptance applies to the delivered
Xiaomi scope; those checks require separate evidence.
Future checks follow [proportional verification](../README.md#proportional-verification).

## Supported-version evidence

Resolved common Compose 1.11.1 uses AndroidX UI/runtime 1.11.2 on Android. The implementation uses
Unstyled 2.9.0, Lifecycle 2.11.0-beta01, Koin 4.2.2, SQLDelight 2.3.2 and Navigation3 1.1.1; no dependency
upgrade was required.

- [Kotlin monotonic time-source contract](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-time-source/)
  supports elapsed-deadline tracking in shared code. The virtual-clock integration uses
  [`TestCoroutineScheduler.timeSource`](https://github.com/Kotlin/kotlinx.coroutines/blob/1.9.0/kotlinx-coroutines-test/common/src/TestCoroutineScheduler.kt)
  so elapsed time and coroutine delays share one clock in the timing regressions.
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
  [Add's integration contract](add-movimiento.md#layout-integration); editor detents retain the shared
  content measurement and focus rules.
