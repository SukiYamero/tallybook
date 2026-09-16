package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.movimiento.BalanceCard
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.confirm
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_light
import tallybook.shared.generated.resources.ui_catalog_navigation
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_system
import tallybook.shared.generated.resources.ui_catalog_text_extra_large
import tallybook.shared.generated.resources.ui_catalog_text_large
import tallybook.shared.generated.resources.ui_catalog_text_normal
import tallybook.shared.generated.resources.ui_catalog_title

internal enum class CatalogThemeMode {
  System,
  Light,
  Dark,
}

@Suppress("MagicNumber")
internal enum class CatalogFontScale(val multiplier: Float) {
  Normal(1f),
  Large(1.5f),
  ExtraLarge(2f),
}

@Composable
internal fun UiCatalogRoute() {
  var themeMode by remember { mutableStateOf(CatalogThemeMode.System) }
  var fontScale by remember { mutableStateOf(CatalogFontScale.Normal) }
  val currentDensity = LocalDensity.current
  val density = Density(density = currentDensity.density, fontScale = fontScale.multiplier)

  TallybookTheme(darkTheme = themeMode.isDarkTheme()) {
    CompositionLocalProvider(LocalDensity provides density) {
      UiCatalogScreen(
          themeMode = themeMode,
          fontScale = fontScale,
          onThemeModeChange = { themeMode = it },
          onFontScaleChange = { fontScale = it },
      )
    }
  }
}

@Composable
internal fun UiCatalogScreen(
    themeMode: CatalogThemeMode,
    fontScale: CatalogFontScale,
    onThemeModeChange: (CatalogThemeMode) -> Unit,
    onFontScaleChange: (CatalogFontScale) -> Unit,
    modifier: Modifier = Modifier,
) {
  val fixtures = uiCatalogFixtures(stringResource(Res.string.category_comida))
  val foundations = stringResource(Res.string.ui_catalog_foundations)
  val actions = stringResource(Res.string.ui_catalog_actions)
  val dataDisplay = stringResource(Res.string.ui_catalog_data_display)
  val navigation = stringResource(Res.string.ui_catalog_navigation)
  TallybookScreenFrame(
      modifier = modifier,
      bottomBar = { TallybookBottomNavigation(onAddMovement = {}) },
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(bottom = TallybookTheme.dimensions.bottomNavigationContentHeight),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      item(key = "catalog-title") {
        TallybookText(
            text = stringResource(Res.string.ui_catalog_title),
            modifier = Modifier.semantics { heading() },
            style = TallybookTheme.typography.headline,
        )
      }
      catalogFoundations(
          title = foundations,
          themeMode = themeMode,
          fontScale = fontScale,
          onThemeModeChange = onThemeModeChange,
          onFontScaleChange = onFontScaleChange,
      )
      catalogActions(title = actions)
      catalogDataDisplay(title = dataDisplay, fixtures = fixtures)
      catalogSection(key = "navigation", title = navigation) {}
    }
  }
}

@Composable
private fun CatalogThemeMode.isDarkTheme(): Boolean =
    when (this) {
      CatalogThemeMode.System -> isSystemInDarkTheme()
      CatalogThemeMode.Light -> false
      CatalogThemeMode.Dark -> true
    }

@Composable
private fun CatalogThemeMode.label(): String =
    stringResource(
        when (this) {
          CatalogThemeMode.System -> Res.string.ui_catalog_system
          CatalogThemeMode.Light -> Res.string.ui_catalog_light
          CatalogThemeMode.Dark -> Res.string.ui_catalog_dark
        }
    )

@Composable
private fun CatalogFontScale.label(): String =
    stringResource(
        when (this) {
          CatalogFontScale.Normal -> Res.string.ui_catalog_text_normal
          CatalogFontScale.Large -> Res.string.ui_catalog_text_large
          CatalogFontScale.ExtraLarge -> Res.string.ui_catalog_text_extra_large
        }
    )

private fun LazyListScope.catalogFoundations(
    title: String,
    themeMode: CatalogThemeMode,
    fontScale: CatalogFontScale,
    onThemeModeChange: (CatalogThemeMode) -> Unit,
    onFontScaleChange: (CatalogFontScale) -> Unit,
) {
  catalogSection(key = "foundations", title = title) {
    CatalogSurface {
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_text),
          style = TallybookTheme.typography.title,
      )
      CatalogThemeMode.entries.forEach { mode ->
        TallybookButton(
            onClick = { onThemeModeChange(mode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = themeMode != mode,
        ) {
          TallybookText(text = mode.label())
        }
      }
      CatalogFontScale.entries.forEach { scale ->
        TallybookButton(
            onClick = { onFontScaleChange(scale) },
            modifier = Modifier.fillMaxWidth(),
            enabled = fontScale != scale,
        ) {
          TallybookText(text = scale.label())
        }
      }
    }
  }
}

private fun LazyListScope.catalogActions(title: String) {
  catalogSection(key = "actions", title = title) {
    CatalogSurface {
      TallybookButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        TallybookText(text = stringResource(Res.string.retry))
      }
      TallybookButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
        TallybookText(text = stringResource(Res.string.confirm))
      }
    }
  }
}

private fun LazyListScope.catalogDataDisplay(title: String, fixtures: UiCatalogFixtures) {
  catalogSection(key = "data-display", title = title) {
    CatalogSurface {
      CategoryAvatar(categoria = fixtures.categoria)
      CategoryChip(categoria = fixtures.categoria, selected = true, onClick = {})
      BalanceCard(summary = fixtures.summary, modifier = Modifier.fillMaxWidth())
      MovimientoRow(
          movimiento = fixtures.movimiento,
          categoria = fixtures.categoria,
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private fun LazyListScope.catalogSection(
    key: String,
    title: String,
    content: @Composable () -> Unit,
) {
  item(key = key) {
    Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
      TallybookText(
          text = title,
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.title,
      )
      content()
    }
  }
}

@Composable
private fun CatalogSurface(content: @Composable () -> Unit) {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
    ) {
      content()
    }
  }
}
