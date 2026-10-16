package com.kurobello.tallybook.feature.profile.viewmodel

import com.kurobello.tallybook.core.model.AppPreferences

internal data class ProfileUiState(
    val preferences: ProfilePreferences = ProfilePreferences.Loading,
    val sheet: ProfileSheet? = null,
    val pendingWrite: ProfileSetting? = null,
    val failedWrite: ProfileSetting? = null,
    val export: ProfileExport = ProfileExport.IDLE,
    val appVersion: String = "",
) {
  val canEdit: Boolean
    get() = preferences is ProfilePreferences.Ready && pendingWrite == null

  val stored: AppPreferences?
    get() = (preferences as? ProfilePreferences.Ready)?.value
}

internal sealed interface ProfilePreferences {
  data object Loading : ProfilePreferences

  data class Ready(val value: AppPreferences) : ProfilePreferences

  data object Unavailable : ProfilePreferences
}

internal enum class ProfileSetting {
  CURRENCY,
  LANGUAGE,
  THEME,
}

internal enum class ProfileSheetKind {
  CURRENCY,
  LANGUAGE,
}

/**
 * A closing sheet keeps the shell modal-owned until its hide animation reports completion.
 * [openedWith] lets it keep rendering, and so finish hiding, after the stored preferences become
 * unavailable.
 */
internal data class ProfileSheet(
    val kind: ProfileSheetKind,
    val openedWith: AppPreferences,
    val isClosing: Boolean = false,
)

internal enum class ProfileExport {
  IDLE,
  EXPORTING,
  FAILED,
}
