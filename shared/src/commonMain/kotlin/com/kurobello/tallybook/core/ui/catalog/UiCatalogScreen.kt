package com.kurobello.tallybook.core.ui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.ui_catalog_title

@Composable
internal fun UiCatalogRoute() {
  UiCatalogScreen()
}

@Composable
internal fun UiCatalogScreen(modifier: Modifier = Modifier) {
  TallybookScreenFrame(modifier = modifier) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      TallybookText(
          text = stringResource(Res.string.ui_catalog_title),
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.headline,
      )
    }
  }
}
