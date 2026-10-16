package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.LocalTallybookContentColor
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.movimiento.BalanceCard
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRow
import com.kurobello.tallybook.core.ui.navigation.TallybookBottomNavigation
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.confirm
import tallybook.shared.generated.resources.navigation_add_movement
import tallybook.shared.generated.resources.navigation_history
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.ui_catalog_actions
import tallybook.shared.generated.resources.ui_catalog_dark
import tallybook.shared.generated.resources.ui_catalog_data_display
import tallybook.shared.generated.resources.ui_catalog_foundations
import tallybook.shared.generated.resources.ui_catalog_light
import tallybook.shared.generated.resources.ui_catalog_sample_body_emphasis
import tallybook.shared.generated.resources.ui_catalog_sample_body_small
import tallybook.shared.generated.resources.ui_catalog_sample_caption
import tallybook.shared.generated.resources.ui_catalog_sample_label
import tallybook.shared.generated.resources.ui_catalog_sample_text
import tallybook.shared.generated.resources.ui_catalog_snackbar_title
import tallybook.shared.generated.resources.ui_catalog_system
import tallybook.shared.generated.resources.ui_catalog_title

internal enum class CatalogThemeMode {
  System,
  Light,
  Dark,
}

private const val CATALOG_CHIP_COUNT = 12

@Composable
internal fun UiCatalogRoute() {
  var themeMode by remember { mutableStateOf(CatalogThemeMode.System) }

  TallybookTheme(darkTheme = themeMode.isDarkTheme()) {
    UiCatalogScreen(
        themeMode = themeMode,
        onThemeModeChange = { themeMode = it },
    )
  }
}

@Composable
internal fun UiCatalogScreen(
    themeMode: CatalogThemeMode,
    onThemeModeChange: (CatalogThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
  val categoryName = stringResource(Res.string.category_comida)
  val fixtures = remember(categoryName) { uiCatalogFixtures(categoryName) }
  val foundations = stringResource(Res.string.ui_catalog_foundations)
  val actions = stringResource(Res.string.ui_catalog_actions)
  val dataDisplay = stringResource(Res.string.ui_catalog_data_display)
  val snackbarTitle = stringResource(Res.string.ui_catalog_snackbar_title)
  TallybookScreenFrame(
      modifier = modifier,
      bottomBar = { TallybookBottomNavigation(onAddMovement = {}, selectedDestination = null) },
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
          onThemeModeChange = onThemeModeChange,
      )
      catalogActions(title = actions)
      catalogSection(key = "snackbar", title = snackbarTitle) { UiCatalogSnackbar() }
      catalogControlMatrices()
      catalogEntryComponents(fixtures)
      catalogDataDisplay(title = dataDisplay, fixtures = fixtures)
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
internal fun CatalogThemeMode.label(): String =
    stringResource(
        when (this) {
          CatalogThemeMode.System -> Res.string.ui_catalog_system
          CatalogThemeMode.Light -> Res.string.ui_catalog_light
          CatalogThemeMode.Dark -> Res.string.ui_catalog_dark
        }
    )

private fun LazyListScope.catalogFoundations(
    title: String,
    themeMode: CatalogThemeMode,
    onThemeModeChange: (CatalogThemeMode) -> Unit,
) {
  catalogSection(key = "foundations", title = title) {
    CatalogSurface {
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_text),
          style = TallybookTheme.typography.body,
      )
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_caption),
          style = TallybookTheme.typography.caption,
      )
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_label),
          style = TallybookTheme.typography.label,
      )
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_body_small),
          style = TallybookTheme.typography.bodySmall,
      )
      TallybookText(
          text = stringResource(Res.string.ui_catalog_sample_body_emphasis),
          style = TallybookTheme.typography.bodyEmphasis,
      )
      Column(
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      ) {
        CatalogThemeMode.entries.forEach { mode ->
          TallybookButton(
              onClick = { onThemeModeChange(mode) },
              modifier = Modifier.fillMaxWidth().semantics { selected = themeMode == mode },
          ) {
            TallybookText(text = mode.label())
          }
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
      TallybookTextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        CatalogActionIcon(icon = Lucide.ArrowUpRight)
        TallybookText(text = stringResource(Res.string.navigation_history))
      }
      TallybookButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        CatalogActionIcon(icon = Lucide.Plus)
        TallybookText(text = stringResource(Res.string.navigation_add_movement))
      }
      TallybookButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
        TallybookText(text = stringResource(Res.string.confirm))
      }
    }
  }
}

@Composable
private fun CatalogActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Image(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
      colorFilter = ColorFilter.tint(LocalTallybookContentColor.current),
  )
}

private fun LazyListScope.catalogDataDisplay(title: String, fixtures: UiCatalogFixtures) {
  catalogSection(key = "data-display", title = title) {
    CatalogSurface {
      CategoryAvatar(categoria = fixtures.categoria)
      CategoryChip(categoria = fixtures.categoria, selected = true, onClick = {})
      CategoryChip(
          categoria = fixtures.categoria,
          selected = false,
          onClick = {},
          trailingContent = {
            TallybookText(
                text = CATALOG_CHIP_COUNT.toString(),
                color = TallybookTheme.colors.foreground.tertiary,
                style = TallybookTheme.typography.controlValue,
            )
          },
      )
      BalanceCard(summary = fixtures.summary, modifier = Modifier.fillMaxWidth())
      MovimientoRow(
          movimiento = fixtures.movimiento,
          categoria = fixtures.categoria,
          modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

internal fun LazyListScope.catalogSection(
    key: String,
    title: String,
    content: @Composable () -> Unit,
) {
  item(key = key) {
    CatalogSection(
        title = title,
        automationTag = catalogSectionAutomationTag(key),
        content = content,
    )
  }
}

@Composable
internal fun CatalogSection(
    title: String,
    automationTag: String? = null,
    content: @Composable () -> Unit,
) {
  Column(
      modifier = if (automationTag == null) Modifier else Modifier.testTag(automationTag),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
  ) {
    TallybookText(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = TallybookTheme.typography.title,
    )
    content()
  }
}
