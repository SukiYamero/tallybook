package com.kurobello.tallybook.core.data.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A recognizer that never listens, for host and device tests that wire the add-movimiento flow
 * without a microphone. It lives here because those test source sets do not share code.
 */
internal object UnavailableVoiceDictation : VoiceDictation {
  override fun permission(): DictationPermission = DictationPermission.NOT_DETERMINED

  override fun isAvailable(): Boolean = false

  override fun listen(languageTag: String, finishRequests: Flow<Unit>): Flow<DictationEvent> =
      flowOf(DictationEvent.Failed(DictationError.UNAVAILABLE))
}
