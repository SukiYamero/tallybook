package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.export.IosMovimientoExportSharer
import com.kurobello.tallybook.core.data.export.MovimientoExportSharer
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual val exportModule: Module = module {
  single<MovimientoExportSharer> {
    IosMovimientoExportSharer(
        ioDispatcher = get(named(DispatcherQualifier.IO)),
        mainDispatcher = get(named(DispatcherQualifier.MAIN)),
    )
  }
}
