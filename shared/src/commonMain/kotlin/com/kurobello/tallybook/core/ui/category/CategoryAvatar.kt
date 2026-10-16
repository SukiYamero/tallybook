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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun CategoryAvatar(categoria: Categoria, modifier: Modifier = Modifier, pending: Boolean = false) {
  CategoryAvatar(
      imageVector = categoria.icono.toImageVector(),
      colors = if (pending) neutralCategoryTintColors() else categoria.color.tintColors(),
      modifier = modifier,
      pending = pending,
  )
}

@Composable
internal fun CategoryAvatar(
    imageVector: ImageVector,
    colors: CategoryTintColors,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
) {
  Box(
      modifier =
          modifier
              .size(TallybookTheme.spacing.space40)
              .clip(CircleShape)
              .then(
                  if (pending) Modifier.dashedCircleOutline(colors.content)
                  else Modifier.background(colors.container)
              ),
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

@Composable
private fun Modifier.dashedCircleOutline(color: Color): Modifier {
  val strokeWidth = TallybookTheme.spacing.space2
  val dashLength = TallybookTheme.spacing.space4
  return drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    drawCircle(
        color = color,
        radius = (size.minDimension - strokeWidthPx) / 2,
        style =
            Stroke(
                width = strokeWidthPx,
                pathEffect =
                    PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), dashLength.toPx())),
            ),
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

@Preview
@Composable
internal fun CategoryAvatarPendingPreview() {
  TallybookTheme(darkTheme = false) { CategoryAvatar(avatarPreviewCategoria(), pending = true) }
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
