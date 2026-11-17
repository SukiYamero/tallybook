package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.voice.IosVoiceDictation
import com.kurobello.tallybook.core.data.voice.VoiceDictation
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val voiceModule: Module = module {
  single<VoiceDictation> {
    IosVoiceDictation(mainDispatcher = get(named(DispatcherQualifier.MAIN)))
  }
}
