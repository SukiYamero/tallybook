package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

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
      modifier = modifier.size(40.dp).clip(CircleShape).background(colors.container),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = colors.content,
    )
  }
}

@Preview
@Composable
internal fun CategoryAvatarLightPreview() {
  TallybookTheme(darkTheme = false) { CategoryAvatar(avatarPreviewCategoria) }
}

@Preview
@Composable
internal fun CategoryAvatarDarkPreview() {
  TallybookTheme(darkTheme = true) { CategoryAvatar(avatarPreviewCategoria) }
}

private val avatarPreviewCategoria =
    Categoria(
        id = "cat_comida",
        nombre = "Comida",
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )
