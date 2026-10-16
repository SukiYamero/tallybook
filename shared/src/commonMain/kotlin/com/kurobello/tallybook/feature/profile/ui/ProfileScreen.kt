package com.kurobello.tallybook.feature.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Tags
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessage
import com.kurobello.tallybook.core.ui.component.TallybookFieldMessageContent
import com.kurobello.tallybook.core.ui.component.TallybookInputLabel
import com.kurobello.tallybook.core.ui.component.TallybookSectionHeading
import com.kurobello.tallybook.core.ui.component.TallybookSegmentOption
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControl
import com.kurobello.tallybook.core.ui.component.TallybookSegmentedControlStyle
import com.kurobello.tallybook.core.ui.component.TallybookSelectionField
import com.kurobello.tallybook.core.ui.component.TallybookSurface
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.component.TallybookTextButton
import com.kurobello.tallybook.core.ui.component.rememberTallybookTapFeedback
import com.kurobello.tallybook.core.ui.layout.TallybookScreenFrame
import com.kurobello.tallybook.core.ui.navigation.Categories
import com.kurobello.tallybook.core.ui.navigation.Destination
import com.kurobello.tallybook.core.ui.navigation.Tags
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileActions
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileExport
import com.kurobello.tallybook.feature.profile.viewmodel.ProfilePreferences
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileSetting
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileSheetKind
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.navigation_profile
import tallybook.shared.generated.resources.profile_categories_body
import tallybook.shared.generated.resources.profile_categories_label
import tallybook.shared.generated.resources.profile_currency_label
import tallybook.shared.generated.resources.profile_currency_note
import tallybook.shared.generated.resources.profile_currency_unavailable
import tallybook.shared.generated.resources.profile_export_action
import tallybook.shared.generated.resources.profile_export_body
import tallybook.shared.generated.resources.profile_export_error
import tallybook.shared.generated.resources.profile_export_title
import tallybook.shared.generated.resources.profile_exporting
import tallybook.shared.generated.resources.profile_language_label
import tallybook.shared.generated.resources.profile_language_note
import tallybook.shared.generated.resources.profile_local_body
import tallybook.shared.generated.resources.profile_local_title
import tallybook.shared.generated.resources.profile_preferences_error
import tallybook.shared.generated.resources.profile_save_error
import tallybook.shared.generated.resources.profile_section_about
import tallybook.shared.generated.resources.profile_section_data
import tallybook.shared.generated.resources.profile_section_preferences
import tallybook.shared.generated.resources.profile_tags_body
import tallybook.shared.generated.resources.profile_tags_label
import tallybook.shared.generated.resources.profile_theme_label
import tallybook.shared.generated.resources.profile_version_label
import tallybook.shared.generated.resources.retry

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    onOpenSecondary: (Destination) -> Unit,
    bottomBarClearance: Dp,
    modifier: Modifier = Modifier,
) {
  TallybookScreenFrame(
      modifier = modifier,
      windowInsets =
          WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
      bottomPaddingFloor = TallybookTheme.spacing.space0,
  ) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = TallybookTheme.spacing.space20,
                    bottom = bottomBarClearance + TallybookTheme.spacing.space16,
                ),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space24),
    ) {
      TallybookText(
          stringResource(Res.string.navigation_profile),
          modifier = Modifier.semantics { heading() },
          style = TallybookTheme.typography.headline,
      )
      ProfileSection(stringResource(Res.string.profile_section_data)) {
        LocalDataCard()
        NavigationCard(
            icon = Lucide.Tags,
            title = stringResource(Res.string.profile_categories_label),
            body = stringResource(Res.string.profile_categories_body),
            onClick = { onOpenSecondary(Categories) },
        )
        NavigationCard(
            icon = Lucide.Hash,
            title = stringResource(Res.string.profile_tags_label),
            body = stringResource(Res.string.profile_tags_body),
            onClick = { onOpenSecondary(Tags) },
        )
        ExportCard(uiState.export, actions)
      }
      ProfileSection(stringResource(Res.string.profile_section_preferences)) {
        PreferenceFields(uiState, actions)
      }
      ProfileSection(stringResource(Res.string.profile_section_about)) {
        VersionCard(uiState.appVersion)
      }
    }
  }
  ProfileSheetHost(uiState, actions)
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12)) {
    TallybookSectionHeading(title)
    content()
  }
}

@Composable
private fun LocalDataCard() {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = Lucide.Smartphone,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
      CardText(
          stringResource(Res.string.profile_local_title),
          stringResource(Res.string.profile_local_body),
      )
    }
  }
}

@Composable
private fun NavigationCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val tapFeedback = rememberTallybookTapFeedback()
  TallybookSurface(
      modifier =
          Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = TallybookTheme.dimensions.minimumTouchTarget)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null,
                  role = Role.Button,
                  onClick = {
                    tapFeedback.trigger()
                    onClick()
                  },
              ),
      isPressed = isPressed,
      hasTapFeedback = tapFeedback.isActive,
  ) {
    Row(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconLarge),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.brand.primary),
      )
      Box(modifier = Modifier.weight(1f)) { CardText(title, body) }
      Image(
          imageVector = Lucide.ChevronRight,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.fieldIconSize),
          colorFilter = ColorFilter.tint(TallybookTheme.colors.foreground.tertiary),
      )
    }
  }
}

@Composable
private fun ExportCard(export: ProfileExport, actions: ProfileActions) {
  val labels = movimientoCsvLabels()
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.padding(TallybookTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      CardText(
          stringResource(Res.string.profile_export_title),
          stringResource(Res.string.profile_export_body),
      )
      if (export == ProfileExport.FAILED) {
        TallybookFieldMessageContent(
            TallybookFieldMessage.Error(stringResource(Res.string.profile_export_error))
        )
      }
      TallybookTextButton(
          onClick = { actions.onExport(labels) },
          enabled = export != ProfileExport.EXPORTING,
      ) {
        TallybookText(
            stringResource(
                if (export == ProfileExport.EXPORTING) Res.string.profile_exporting
                else Res.string.profile_export_action
            )
        )
      }
    }
  }
}

@Composable
private fun CardText(title: String, body: String) {
  Column(
      modifier = Modifier.semantics(mergeDescendants = true) {},
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
  ) {
    TallybookText(title, style = TallybookTheme.typography.bodyEmphasis)
    TallybookText(
        body,
        color = TallybookTheme.colors.foreground.secondary,
        style = TallybookTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun PreferenceFields(uiState: ProfileUiState, actions: ProfileActions) {
  val stored = uiState.stored
  val unavailable = stringResource(Res.string.profile_currency_unavailable)
  val saveError = TallybookFieldMessage.Error(stringResource(Res.string.profile_save_error))
  TallybookSelectionField(
      label = stringResource(Res.string.profile_currency_label),
      value =
          stored?.let { currencyValueLabel(it.monedaPrincipal) }
              ?: unavailable.takeIf { uiState.preferences == ProfilePreferences.Unavailable },
      placeholder = "",
      onClick = { actions.onOpenSheet(ProfileSheetKind.CURRENCY) },
      modifier = Modifier.fillMaxWidth(),
      enabled = uiState.canEdit,
      message = saveError.takeIf { uiState.failedWrite == ProfileSetting.CURRENCY },
  )
  TallybookSelectionField(
      label = stringResource(Res.string.profile_language_label),
      value =
          stored?.let { languageLabel(it.appLocale) }
              ?: unavailable.takeIf { uiState.preferences == ProfilePreferences.Unavailable },
      placeholder = "",
      onClick = { actions.onOpenSheet(ProfileSheetKind.LANGUAGE) },
      modifier = Modifier.fillMaxWidth(),
      enabled = uiState.canEdit,
      message = saveError.takeIf { uiState.failedWrite == ProfileSetting.LANGUAGE },
  )
  ThemeField(uiState, actions, saveError.takeIf { uiState.failedWrite == ProfileSetting.THEME })
  if (uiState.preferences == ProfilePreferences.Unavailable) {
    TallybookFieldMessageContent(
        TallybookFieldMessage.Error(stringResource(Res.string.profile_preferences_error))
    )
    TallybookTextButton(actions::onRetry) { TallybookText(stringResource(Res.string.retry)) }
  }
}

@Composable
private fun ThemeField(
    uiState: ProfileUiState,
    actions: ProfileActions,
    message: TallybookFieldMessage?,
) {
  Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.dimensions.fieldLabelSpacing)) {
    TallybookInputLabel(text = stringResource(Res.string.profile_theme_label))
    TallybookSegmentedControl(
        options =
            ThemeMode.entries.map {
              TallybookSegmentOption(it, stringResource(it.labelResource()))
            },
        selected = uiState.stored?.themeMode ?: ThemeMode.SYSTEM,
        onSelect = actions::onSelectTheme,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.canEdit,
        style = TallybookSegmentedControlStyle.Neutral,
        labelStyle = TallybookTheme.typography.controlValue,
    )
    if (message != null) TallybookFieldMessageContent(message)
  }
}

@Composable
private fun VersionCard(version: String) {
  TallybookSurface(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(TallybookTheme.spacing.space16).semantics(
                mergeDescendants = true
            ) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
      TallybookText(
          stringResource(Res.string.profile_version_label),
          modifier = Modifier.weight(1f),
      )
      TallybookText(
          version,
          color = TallybookTheme.colors.foreground.secondary,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun ProfileSheetHost(uiState: ProfileUiState, actions: ProfileActions) {
  val sheet = uiState.sheet ?: return
  val stored = uiState.stored ?: sheet.openedWith
  // One composed sheet per opening, so its hide callback can only belong to the closing sheet.
  key(sheet.kind) {
    when (sheet.kind) {
      ProfileSheetKind.CURRENCY ->
          PreferenceOptionSheet(
              visible = !sheet.isClosing,
              title = stringResource(Res.string.profile_currency_label),
              note = stringResource(Res.string.profile_currency_note),
              options = currencyOptions(),
              selected = stored.monedaPrincipal,
              onSelect = actions::onSelectCurrency,
              onDismiss = actions::onDismissSheet,
              onHidden = actions::onSheetHidden,
          )
      ProfileSheetKind.LANGUAGE ->
          PreferenceOptionSheet(
              visible = !sheet.isClosing,
              title = stringResource(Res.string.profile_language_label),
              note = stringResource(Res.string.profile_language_note),
              options = languageOptions(),
              selected = stored.appLocale,
              onSelect = actions::onSelectLanguage,
              onDismiss = actions::onDismissSheet,
              onHidden = actions::onSheetHidden,
          )
    }
  }
}
