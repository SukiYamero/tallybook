package com.kurobello.tallybook

import android.app.Application
import com.kurobello.tallybook.di.initKoinAndroid

class TallybookApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    initKoinAndroid(this)
  }
}
