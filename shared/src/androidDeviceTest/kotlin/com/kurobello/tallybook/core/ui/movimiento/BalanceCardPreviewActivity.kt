package com.kurobello.tallybook.core.ui.movimiento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.ui.HistoryPeriodSummary
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection

class BalanceCardPreviewActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val period =
        RetainedLoadState.Ready(
            HistoryProjection.Content(
                MovimientoSummary(Money(5_000_000, Moneda.COP), Money(200_000_000, Moneda.COP)),
                emptyList(),
            )
        )
    val dark = intent.getBooleanExtra("dark", false)
    val fontScale = intent.getFloatExtra("fontScale", 1f)
    val widthDp = intent.getIntExtra("widthDp", 0)
    val home = intent.getBooleanExtra("home", false)
    setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        TallybookTheme(darkTheme = dark) {
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .background(MaterialTheme.colorScheme.background)
                      .padding(TallybookTheme.dimensions.screenHorizontal),
              contentAlignment = Alignment.TopCenter,
          ) {
            Box(
                modifier = if (widthDp > 0) Modifier.width(widthDp.dp) else Modifier.fillMaxWidth()
            ) {
              if (home) BalanceCard(period.value.summary, Modifier.fillMaxWidth())
              else HistoryPeriodSummary(period, {})
            }
          }
        }
      }
    }
  }
}
