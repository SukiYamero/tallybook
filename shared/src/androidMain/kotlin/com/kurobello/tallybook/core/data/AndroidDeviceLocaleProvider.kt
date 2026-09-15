package com.kurobello.tallybook.core.data

import android.os.LocaleList

class AndroidDeviceLocaleProvider : DeviceLocaleProvider {
  override fun currentLanguageTag(): String = LocaleList.getDefault().get(0).toLanguageTag()
}
