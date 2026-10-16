package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.movimiento.MovimientoBadgeSize
import com.kurobello.tallybook.core.ui.movimiento.MovimientoTypeBadge
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.movimiento_relative_today
import tallybook.shared.generated.resources.movimiento_relative_yesterday
import tallybook.shared.generated.resources.movimiento_voice_coming_soon
import tallybook.shared.generated.resources.movimiento_voice_title

@OptIn(ExperimentalResourceApi::class)
class MovimientoBadgesAndVoiceTest {
  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setUp() {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)
  }

  @Test
  fun typeBadgeShowsTheLocalizedLabelInBothSizes() {
    composeRule.setContent {
      TallybookTheme {
        MovimientoTypeBadge(TipoMovimiento.GASTO)
        MovimientoTypeBadge(TipoMovimiento.INGRESO, size = MovimientoBadgeSize.Large)
      }
    }

    composeRule.onNodeWithText(string(Res.string.expense)).assertExists()
    composeRule.onNodeWithText(string(Res.string.income)).assertExists()
  }

  @Test
  fun dateBadgeShowsTodayAndYesterdayRelativeToTheCivilToday() {
    val today = MovimientoEntrySamples.today
    composeRule.setContent {
      TallybookTheme {
        MovimientoDateBadge(today, today)
        MovimientoDateBadge(
            MovimientoEntrySamples.yesterday,
            today,
            size = MovimientoBadgeSize.Large,
        )
      }
    }

    composeRule.onNodeWithText(string(Res.string.movimiento_relative_today)).assertExists()
    composeRule.onNodeWithText(string(Res.string.movimiento_relative_yesterday)).assertExists()
  }

  @Test
  fun voicePlaceholderIsAnnouncedAsADisabledButtonWithComingSoonCopy() {
    composeRule.setContent { TallybookTheme { VoiceDictationPlaceholder() } }

    composeRule
        .onNode(hasText(string(Res.string.movimiento_voice_title)))
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        .assertIsNotEnabled()
    composeRule.onNode(hasText(string(Res.string.movimiento_voice_coming_soon))).assertExists()
  }

  private fun string(resource: StringResource): String = runBlocking { getString(resource) }
}
