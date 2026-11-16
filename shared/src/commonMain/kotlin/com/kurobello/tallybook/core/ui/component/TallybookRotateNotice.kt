package com.kurobello.tallybook.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.rotate_notice_body
import tallybook.shared.generated.resources.rotate_notice_dismiss
import tallybook.shared.generated.resources.rotate_notice_title

private const val LANDSCAPE_DEGREES = 90f
private const val PORTRAIT_DEGREES = 0f
private const val ROTATION_CYCLE_MILLIS = 1_800
private const val ROTATION_START_MILLIS = 400
private const val ROTATION_END_MILLIS = 1_000

/**
 * Whether to suggest holding the phone upright. Only phones are nudged: on a larger shortest side
 * landscape is a comfortable way to use the app.
 */
internal fun shouldSuggestPortrait(width: Dp, height: Dp, phoneShortestSideMaximum: Dp): Boolean =
    width > height && minOf(width, height) < phoneShortestSideMaximum

/**
 * A floating, non-blocking card suggesting to rotate the phone back to portrait. It does not
 * intercept touches outside the card, and once dismissed it stays hidden for the session, including
 * across rotations, which recreate the Android activity.
 */
@Composable
internal fun TallybookRotateNotice(modifier: Modifier = Modifier) {
  var dismissed by rememberSaveable { mutableStateOf(false) }
  val containerSize = LocalWindowInfo.current.containerSize
  val phoneShortestSideMaximum = TallybookTheme.dimensions.phoneShortestSideMaximum
  val isPhoneLandscape =
      with(LocalDensity.current) {
        shouldSuggestPortrait(
            width = containerSize.width.toDp(),
            height = containerSize.height.toDp(),
            phoneShortestSideMaximum = phoneShortestSideMaximum,
        )
      }
  Box(
      modifier =
          modifier
              .fillMaxSize()
              .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                      WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                  )
              )
              .padding(TallybookTheme.spacing.space12),
      contentAlignment = Alignment.TopCenter,
  ) {
    AnimatedVisibility(
        visible = isPhoneLandscape && !dismissed,
        enter =
            fadeIn(TallybookOverlayMotion.enterAnimationSpec) +
                scaleIn(
                    animationSpec = TallybookOverlayMotion.enterAnimationSpec,
                    initialScale = TallybookOverlayMotion.initialScale,
                ),
        exit = fadeOut(TallybookOverlayMotion.exitAnimationSpec),
    ) {
      RotateNoticeCard(onDismiss = { dismissed = true })
    }
  }
}

@Composable
private fun RotateNoticeCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  TallybookSurface(
      modifier =
          modifier.widthIn(max = TallybookTheme.dimensions.rotateNoticeMaximumWidth).fillMaxWidth(),
      style =
          TallybookSurfaceDefaults.cardStyle()
              .copy(
                  shape = RoundedCornerShape(TallybookTheme.radii.radius16),
                  border =
                      BorderStroke(
                          TallybookTheme.dimensions.borderThin,
                          TallybookTheme.colors.border.subtle,
                      ),
              ),
  ) {
    Row(
        modifier =
            Modifier.padding(
                start = TallybookTheme.spacing.space32,
                top = TallybookTheme.spacing.space24,
                bottom = TallybookTheme.spacing.space24,
            ),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      RotatingPhoneIcon()
      RotateNoticeMessage(modifier = Modifier.weight(1f))
      TallybookButton(
          onClick = onDismiss,
          colors = TallybookButtonDefaults.textColors(),
          contentPadding =
              PaddingValues(
                  horizontal = TallybookTheme.spacing.space32,
                  vertical = TallybookTheme.spacing.space12,
              ),
      ) {
        TallybookText(
            text = stringResource(Res.string.rotate_notice_dismiss),
            style = TallybookTheme.typography.title,
        )
      }
    }
  }
}

@Composable
private fun RotateNoticeMessage(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
  ) {
    TallybookText(
        text = stringResource(Res.string.rotate_notice_title),
        style = TallybookTheme.typography.headline,
    )
    TallybookText(
        text = stringResource(Res.string.rotate_notice_body),
        style = TallybookTheme.typography.noticeBody,
        color = TallybookTheme.colors.foreground.secondary,
    )
  }
}

@Composable
private fun RotatingPhoneIcon(modifier: Modifier = Modifier) {
  val rotation =
      rememberInfiniteTransition(label = "rotate_notice_rotation")
          .animateFloat(
              initialValue = LANDSCAPE_DEGREES,
              targetValue = PORTRAIT_DEGREES,
              animationSpec =
                  infiniteRepeatable(
                      animation =
                          keyframes {
                            durationMillis = ROTATION_CYCLE_MILLIS
                            LANDSCAPE_DEGREES at ROTATION_START_MILLIS
                            PORTRAIT_DEGREES at
                                ROTATION_END_MILLIS using
                                TallybookTheme.motion.iosEasing
                          }
                  ),
              label = "rotate_notice_rotation_degrees",
          )
  Image(
      imageVector = Lucide.Smartphone,
      contentDescription = null,
      modifier =
          modifier.size(TallybookTheme.dimensions.rotateNoticeIcon).graphicsLayer {
            rotationZ = rotation.value
          },
      colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
  )
}
