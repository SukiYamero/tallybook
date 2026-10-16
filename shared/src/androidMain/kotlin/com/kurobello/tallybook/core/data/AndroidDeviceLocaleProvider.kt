package com.kurobello.tallybook.core.data

import android.content.res.Resources

class AndroidDeviceLocaleProvider : DeviceLocaleProvider {
  // The process default follows the in-app language; region inference needs the device's own.
  override fun currentLanguageTag(): String =
      Resources.getSystem().configuration.locales.get(0).toLanguageTag()
}
