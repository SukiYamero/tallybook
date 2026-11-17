package com.kurobello.tallybook.core.data.voice

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/** Tests push recognizer events through [events]; each session ends at its Final or Failed. */
class FakeVoiceDictation(
    var permission: DictationPermission = DictationPermission.GRANTED,
    var available: Boolean = true,
) : VoiceDictation {
  val events = Channel<DictationEvent>(Channel.UNLIMITED)
  val languageTags = mutableListOf<String>()
  var finishRequestCount = 0
    private set

  var activeSessions = 0
    private set

  override fun permission(): DictationPermission = permission

  override fun isAvailable(): Boolean = available

  override fun listen(languageTag: String, finishRequests: Flow<Unit>): Flow<DictationEvent> =
      channelFlow {
        languageTags += languageTag
        activeSessions++
        val finishWatcher = launch { finishRequests.collect { finishRequestCount++ } }
        try {
          for (event in events) {
            send(event)
            if (event is DictationEvent.Final || event is DictationEvent.Failed) break
          }
        } finally {
          finishWatcher.cancel()
          activeSessions--
        }
      }
}
