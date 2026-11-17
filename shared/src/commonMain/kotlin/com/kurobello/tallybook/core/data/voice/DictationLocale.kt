package com.kurobello.tallybook.core.data.voice

import com.kurobello.tallybook.core.model.AppLocale

// Offline recognizers ship few regional models (Google's has es-ES and es-US, no es-AR or es-CO),
// so each app language dictates in the variant most devices can install.
private val dictationTags: Map<AppLocale, String> =
    mapOf(
        AppLocale.ES to "es-ES",
        AppLocale.ES_AR to "es-ES",
        AppLocale.EN to "en-US",
        AppLocale.PT_BR to "pt-BR",
    )

fun dictationLanguageTag(appLocale: AppLocale): String = dictationTags.getValue(appLocale)
