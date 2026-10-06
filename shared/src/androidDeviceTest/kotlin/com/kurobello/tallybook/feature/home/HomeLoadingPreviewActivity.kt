package com.kurobello.tallybook.feature.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

class HomeLoadingPreviewActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val empty =
        HomeUiState.ready(
            HomeContent(HomeUiState.emptySummary(Moneda.USD), emptyList(), emptyMap())
        )
    val state =
        when {
          intent.getBooleanExtra("cached", false) -> empty.revalidating()
          intent.getBooleanExtra("empty", false) -> empty
          else -> HomeUiState.loading(Moneda.USD)
        }
    val dark = intent.getBooleanExtra("dark", false)
    val fontScale = intent.getFloatExtra("fontScale", 1f)
    setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        TallybookTheme(darkTheme = dark) { HomeScreen(state, {}, {}) }
      }
    }
  }
}
