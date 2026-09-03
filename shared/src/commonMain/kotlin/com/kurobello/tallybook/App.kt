package com.kurobello.tallybook

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
@Preview
fun App() {
  TallybookTheme { TallybookNavHost() }
}
