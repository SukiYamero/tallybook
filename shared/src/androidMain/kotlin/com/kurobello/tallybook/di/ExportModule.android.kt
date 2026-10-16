package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.export.AndroidMovimientoExportSharer
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val exportModule: Module = module {
  single<MovimientoExportSharer> {
    AndroidMovimientoExportSharer(
        context = androidContext(),
        ioDispatcher = get(named(DispatcherQualifier.IO)),
    )
  }
}
