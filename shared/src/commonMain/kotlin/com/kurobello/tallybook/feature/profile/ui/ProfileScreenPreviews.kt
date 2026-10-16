package com.kurobello.tallybook.feature.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.data.export.MovimientoCsvLabels
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.AppPreferences
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileActions
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileExport
import com.kurobello.tallybook.feature.profile.viewmodel.ProfilePreferences
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileSetting
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileSheetKind
import com.kurobello.tallybook.feature.profile.viewmodel.ProfileUiState

private val previewPreferences =
    ProfilePreferences.Ready(AppPreferences(Moneda.COP, ThemeMode.SYSTEM, AppLocale.ES))

@Preview
@Composable
internal fun ProfileLightPreview() =
    ProfilePreview(ProfileUiState(previewPreferences, appVersion = "1.0"), dark = false)

@Preview
@Composable
internal fun ProfileDarkPreview() =
    ProfilePreview(ProfileUiState(previewPreferences, appVersion = "1.0"), dark = true)

@Preview
@Composable
internal fun ProfileFailuresPreview() =
    ProfilePreview(
        ProfileUiState(
            previewPreferences,
            failedWrite = ProfileSetting.THEME,
            export = ProfileExport.FAILED,
            appVersion = "1.0",
        ),
        dark = false,
    )

@Preview
@Composable
internal fun ProfileUnavailablePreview() =
    ProfilePreview(ProfileUiState(ProfilePreferences.Unavailable), dark = false)

@Preview
@Composable
internal fun LanguageOptionsPreview() {
  TallybookTheme(darkTheme = false) {
    PreferenceOptionContent("Idioma", "", languageOptions(), AppLocale.EN, onSelect = {})
  }
}

@Composable
private fun ProfilePreview(uiState: ProfileUiState, dark: Boolean) {
  TallybookTheme(darkTheme = dark) {
    ProfileScreen(uiState, PreviewProfileActions, {}, TallybookTheme.spacing.space0)
  }
}

private object PreviewProfileActions : ProfileActions {
  override fun onOpenSheet(kind: ProfileSheetKind) = false

  override fun onDismissSheet() = Unit

  override fun onSheetHidden() = Unit

  override fun onSelectCurrency(moneda: Moneda) = Unit

  override fun onSelectLanguage(appLocale: AppLocale?) = Unit

  override fun onSelectTheme(themeMode: ThemeMode) = Unit

  override fun onExport(labels: MovimientoCsvLabels) = Unit

  override fun onRetry() = Unit
}
