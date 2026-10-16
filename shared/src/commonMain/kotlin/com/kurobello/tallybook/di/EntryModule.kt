package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.data.entry.CategoryAppearanceSuggester
import com.kurobello.tallybook.core.data.entry.CategoryKeywordDictionary
import com.kurobello.tallybook.core.data.entry.CategorySuggester
import com.kurobello.tallybook.core.data.entry.EtiquetaMatcher
import com.kurobello.tallybook.core.data.entry.MovimientoPhraseParser
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoEntryEngine
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val entryModule = module {
  single { MovimientoPhraseParser() }
  single { CategoryKeywordDictionary.builtIn }
  single { CategorySuggester(get()) }
  single { EtiquetaMatcher() }
  single { CategoryAppearanceSuggester(get(), CategorySeedCatalog.categories) }
  single {
    MovimientoEntryEngine(
        parser = get(),
        suggester = get(),
        etiquetaMatcher = get(),
        localeProvider = get(),
        computationDispatcher = get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT)),
    )
  }
}
