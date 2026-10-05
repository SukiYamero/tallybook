package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida

@Composable
fun CategoryAvatar(categoria: Categoria, modifier: Modifier = Modifier) {
  CategoryAvatar(
      imageVector = categoria.icono.toImageVector(),
      colors = categoria.color.tintColors(),
      modifier = modifier,
  )
}

@Composable
internal fun CategoryAvatar(
    imageVector: ImageVector,
    colors: CategoryTintColors,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier =
          modifier
              .size(TallybookTheme.spacing.space40)
              .clip(CircleShape)
              .background(colors.container),
      contentAlignment = Alignment.Center,
  ) {
    Image(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(TallybookTheme.dimensions.iconMedium),
        colorFilter = ColorFilter.tint(colors.content),
    )
  }
}

@Preview
@Composable
internal fun CategoryAvatarLightPreview() {
  TallybookTheme(darkTheme = false) { CategoryAvatar(avatarPreviewCategoria()) }
}

@Preview
@Composable
internal fun CategoryAvatarDarkPreview() {
  TallybookTheme(darkTheme = true) { CategoryAvatar(avatarPreviewCategoria()) }
}

@Composable
private fun avatarPreviewCategoria() =
    Categoria(
        id = "cat_comida",
        nombre = stringResource(Res.string.category_comida),
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )
