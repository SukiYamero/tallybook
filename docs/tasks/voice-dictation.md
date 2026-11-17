# Dictado por voz — iOS

## Contexto

El dictado por voz quedó integrado y aceptado en Android ([feature](../features/voice-dictation.md)).
Falta llevar a iPhone la misma experiencia: `IosVoiceDictation` y el permiso iOS compilan, pero no se
ejecutaron en un dispositivo.

## Waves

- **Wave 1**: preparar el entorno (plataforma iOS 26.5 en Xcode › Settings › Components, iPhone
  conectado) y compilar `iosApp` para dispositivo; confirmar que Speech/AVFAudio enlazan desde el
  framework estático (si no, `OTHER_LDFLAGS` en `iosApp/Configuration/Config.xcconfig`).
- **Wave 2**: correr en el iPhone y corregir lo que aparezca en `IosVoiceDictation`,
  `DictationPermissionLauncher.ios.kt` e `Info.plist`.

## Verificación

- **APIs sensibles a versión/plataforma**: `SFSpeechRecognizer`, `SFSpeechAudioBufferRecognitionRequest`,
  `AVAudioEngine`, `AVAudioApplication` (iOS 17+), on-device obligatorio
  (https://developer.apple.com/documentation/speech/sfspeechrecognizer).
- **Runtime por target** (iPhone físico): permisos de reconocimiento y micrófono (conceder, negar,
  abrir ajustes), mantener presionado con el dedo desplazándose, dictado en español con monto y
  "cada mes", idioma sin modelo on-device, Siri/Dictado desactivado. Lo no ejecutado queda sin
  confirmar.
