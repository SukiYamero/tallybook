package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.AppInfo
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual val appInfoModule: Module = module {
  single<AppInfo> {
    AppInfo {
      NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    }
  }
}
