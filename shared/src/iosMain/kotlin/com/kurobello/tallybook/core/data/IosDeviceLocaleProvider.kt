package com.kurobello.tallybook.core.data

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

class IosDeviceLocaleProvider : DeviceLocaleProvider {
  override fun currentLanguageTag(): String =
      NSLocale.preferredLanguages.firstOrNull() as? String ?: ""
}
