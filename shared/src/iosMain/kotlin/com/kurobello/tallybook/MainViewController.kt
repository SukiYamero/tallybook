package com.kurobello.tallybook

import androidx.compose.ui.window.ComposeUIViewController
import com.kurobello.tallybook.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
  initKoin()
  return ComposeUIViewController { App() }
}
