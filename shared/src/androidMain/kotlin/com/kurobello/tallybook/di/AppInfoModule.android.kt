package com.kurobello.tallybook.di

import android.content.Context
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import com.kurobello.tallybook.core.data.AppInfo
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val appInfoModule: Module = module {
  single<AppInfo> {
    val context: Context = androidContext()
    val info =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getPackageInfo(context.packageName, PackageInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
        }
    val versionName = info.versionName.orEmpty()
    AppInfo { versionName }
  }
}
