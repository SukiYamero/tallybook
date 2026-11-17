package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.voice.DictationError
import com.kurobello.tallybook.core.data.voice.DictationEvent
import com.kurobello.tallybook.core.data.voice.DictationPermission
import com.kurobello.tallybook.core.data.voice.VoiceDictation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

internal val DICTATION_MAX_DURATION: Duration = 25.seconds

// People lift the finger while saying the last word, and the recognizer finalizes only what it has
// already decoded: listening a moment longer keeps that word instead of an empty or cut result.
internal val DICTATION_RELEASE_TAIL: Duration = 700.milliseconds

// A recognizer that never answers a finish request must not leave the mic listening forever.
private val FINISH_GRACE: Duration = 3.seconds

/**
 * One held-to-talk sentence: the platform recognizer plus the limits Tallybook adds on top. The
 * user's release is the normal end; [DICTATION_MAX_DURATION] caps a press that is never released.
 */
internal class MovimientoDictation(private val voice: VoiceDictation) {
  fun permission(): DictationPermission = voice.permission()

  fun isAvailable(): Boolean = voice.isAvailable()

  /**
   * Finishes [DICTATION_RELEASE_TAIL] after [stopRequests] emits or after [DICTATION_MAX_DURATION],
   * and always ends with exactly one Final or Failed.
   */
  fun listen(languageTag: String, stopRequests: Flow<Unit>): Flow<DictationEvent> =
      channelFlow {
            val finish = Channel<Unit>(Channel.CONFLATED)
            val finishRequested = CompletableDeferred<Unit>()
            fun requestFinish() {
              finish.trySend(Unit)
              finishRequested.complete(Unit)
            }
            var lastText = ""

            launch {
              stopRequests.collect {
                launch {
                  delay(DICTATION_RELEASE_TAIL)
                  requestFinish()
                }
              }
            }
            launch {
              delay(DICTATION_MAX_DURATION)
              requestFinish()
            }
            val recognizer = launch {
              voice.listen(languageTag, finish.receiveAsFlow()).collect { event ->
                if (event is DictationEvent.Partial) lastText = event.text
                send(event)
              }
            }
            launch {
              finishRequested.await()
              delay(FINISH_GRACE)
              send(DictationEvent.Final(lastText))
            }
            recognizer.join()
            // Reaching here means the recognizer ended without its terminal event.
            send(DictationEvent.Final(lastText))
            coroutineContext.cancelChildren()
          }
          // The contract reports failures as events; a platform that throws anyway still ends
          // the session instead of crashing the screen's scope.
          .catch { emit(DictationEvent.Failed(DictationError.UNKNOWN)) }
          .transformWhile { event ->
            emit(event)
            !event.isTerminal
          }
}

internal val DictationEvent.isTerminal: Boolean
  get() = this is DictationEvent.Final || this is DictationEvent.Failed
