package com.kurobello.tallybook

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
fun App() {
  TallybookTheme { TallybookNavHost() }
}
