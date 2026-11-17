# Voice dictation and dictated recurrence

The sentence step of [Add movimiento](add-movimiento.md) can be dictated: the user holds the
microphone, speaks, and the words fill the same sentence the parser already reads; sliding left
while holding cancels and restores what was typed. The sentence can
also state a cadence ("cada 30 días", "every month") that turns Repetir on. Rules live in business
logic: [Natural-language entry](../business-logic.md#natural-language-entry) (Voice dictation,
Monto and Frecuencia) and [Recurrentes](../business-logic.md#recurrentes).

## Integration

- `core.data.voice`: the `VoiceDictation` contract (a cold `Flow` of `DictationEvent`s that ends in
  one `Final` or `Failed`; cancelling the collection aborts), `dictationLanguageTag` (es-ES, en-US,
  pt-BR) and the Koin `voiceModule` with one actual per platform.
  - Android: `AndroidVoiceDictation` uses only `createOnDeviceSpeechRecognizer`. On API 33+ it checks
    `checkRecognitionSupport` and picks an installed model of the same language
    (`chooseRecognizerLanguage`) or asks the system to download it (`triggerModelDownload`, whose
    queued request must run before `destroy()` or Android 13 crashes). The manifest declares
    `RECORD_AUDIO` and the `RecognitionService` `<queries>` entry.
  - iOS: `IosVoiceDictation` drives `SFSpeechRecognizer` with on-device recognition required and an
    `AVAudioEngine` tap, in Kotlin/Native. `Info.plist` carries the microphone and speech usage
    strings (English only).
- `feature.movimientos`: `MovimientoDictation` adds the 30 s cap, the 0.7 s release tail and a grace
  period for a recognizer that never answers; `AddMovimientoVoice` owns the permission flow and the
  single session; `VoiceDictationButton` is the hold-to-talk disc (a screen reader toggles instead).
  The permission request and app settings run through `AddMovimientoEffect` and the
  `rememberDictationPermissionLauncher` expect/actual.
- Parser: `PhraseRecurrence` and `PhraseRecurrenceLexicon` read cadences before dates and amounts;
  `ParsedMovimientoPhrase.frecuencia` and `PhraseReviewFlag.UNSUPPORTED_RECURRENCE` reach the
  ViewModel, which owns Repetir until the user edits it. The tokenizer reads space-grouped thousands.

## Verification boundaries

Host: the full shared suite (including `AddMovimientoVoiceTest`, `MovimientoPhraseRecurrenceTest`,
`AddMovimientoRepetirTest`, `RecognizerLanguageChoiceTest`, `AndroidVoiceDictationTest` and the
space-grouped amount cases), Detekt, ktfmtCheck, Native compilation, Android device-test compilation
and the debug APK, after an intermediate and a final independent review.

On the Xiaomi M2012K11AG (Android 13, phone in en-US, app in Spanish): the permission dialog and the
notice after a refusal; the system download prompt for es-ES (49.31 MB) without a crash; held
dictation that keeps listening through pauses and a drifting finger until it lifts (adb traces and
`MovimientoBadgesAndVoiceTest` instrumented, 7/7); and the user's own dictation of expenses with
amounts and "cada mes", accepted as working.

Not covered: iOS runtime (see the backlog's iOS item), Android 12 (no language check), devices with
another on-device recognizer, TalkBack with real speech, and dictation in en-US and pt-BR on device.
