package com.kurobello.tallybook

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost

@Composable
@Preview
fun App() {
  MaterialTheme { TallybookNavHost() }
}
