package com.kurobello.tallybook.core.ui.component

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.datetime.LocalDate

class DatePickerPreviewActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val widthDp = intent.getIntExtra("widthDp", 320)
    val heightDp = intent.getIntExtra("heightDp", 0)
    val daySizeDp = intent.getIntExtra("daySizeDp", 48)
    val dark = intent.getBooleanExtra("dark", false)
    setContent {
      val density = LocalDensity.current
      val window = LocalWindowInfo.current
      val compactWindow =
          object : WindowInfo {
            override val isWindowFocused = window.isWindowFocused
            override val containerSize =
                with(density) {
                  IntSize(
                      widthDp.dp.roundToPx(),
                      if (heightDp > 0) heightDp.dp.roundToPx() else window.containerSize.height,
                  )
                }
          }
      CompositionLocalProvider(LocalWindowInfo provides compactWindow) {
        TallybookTheme(darkTheme = dark) {
          var selected by remember { mutableStateOf(LocalDate(2026, 9, 15)) }
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .background(MaterialTheme.colorScheme.background)
                      .padding(TallybookTheme.spacing.space20),
              verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space20),
          ) {
            TallybookText("Viewport: ${widthDp}dp")
            TallybookDateChipPicker(
                value = selected,
                onValueChange = { selected = it },
                label = "Date",
                maxDate = LocalDate(2026, 9, 30),
                calendarDaySize = daySizeDp.dp,
            )
            TallybookText("Selected: $selected")
          }
        }
      }
    }
  }
}
