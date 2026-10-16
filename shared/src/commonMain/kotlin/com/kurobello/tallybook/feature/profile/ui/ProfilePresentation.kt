package com.kurobello.tallybook.feature.profile.ui

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.data.export.MovimientoCsvLabels
import com.kurobello.tallybook.core.model.AppLocale
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.ThemeMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.csv_header_amount
import tallybook.shared.generated.resources.csv_header_category
import tallybook.shared.generated.resources.csv_header_currency
import tallybook.shared.generated.resources.csv_header_date
import tallybook.shared.generated.resources.csv_header_description
import tallybook.shared.generated.resources.csv_header_parent_category
import tallybook.shared.generated.resources.csv_header_type
import tallybook.shared.generated.resources.currency_name_ars
import tallybook.shared.generated.resources.currency_name_brl
import tallybook.shared.generated.resources.currency_name_clp
import tallybook.shared.generated.resources.currency_name_cop
import tallybook.shared.generated.resources.currency_name_dop
import tallybook.shared.generated.resources.currency_name_mxn
import tallybook.shared.generated.resources.currency_name_pen
import tallybook.shared.generated.resources.currency_name_usd
import tallybook.shared.generated.resources.expense
import tallybook.shared.generated.resources.income
import tallybook.shared.generated.resources.profile_language_device
import tallybook.shared.generated.resources.profile_theme_dark
import tallybook.shared.generated.resources.profile_theme_light
import tallybook.shared.generated.resources.profile_theme_system

internal fun Moneda.nameResource(): StringResource =
    when (this) {
      Moneda.COP -> Res.string.currency_name_cop
      Moneda.MXN -> Res.string.currency_name_mxn
      Moneda.ARS -> Res.string.currency_name_ars
      Moneda.CLP -> Res.string.currency_name_clp
      Moneda.BRL -> Res.string.currency_name_brl
      Moneda.USD -> Res.string.currency_name_usd
      Moneda.DOP -> Res.string.currency_name_dop
      Moneda.PEN -> Res.string.currency_name_pen
    }

@Composable
internal fun currencyValueLabel(moneda: Moneda): String =
    "${stringResource(moneda.nameResource())} (${moneda.name})"

@Composable
internal fun currencyOptions(): List<PreferenceOption<Moneda>> =
    Moneda.entries.map {
      PreferenceOption(it, stringResource(it.nameResource()), "${it.name} · ${it.symbol}")
    }

/** Endonyms: each language is named in itself so it stays recognizable from any current locale. */
internal fun AppLocale.endonym(): String =
    when (this) {
      AppLocale.ES -> "Español"
      AppLocale.ES_AR -> "Español (Argentina)"
      AppLocale.EN -> "English"
      AppLocale.PT_BR -> "Português (Brasil)"
    }

@Composable
internal fun languageLabel(appLocale: AppLocale?): String =
    appLocale?.endonym() ?: stringResource(Res.string.profile_language_device)

@Composable
internal fun languageOptions(): List<PreferenceOption<AppLocale?>> =
    listOf(PreferenceOption<AppLocale?>(null, languageLabel(null), null)) +
        AppLocale.entries.map { PreferenceOption(it, it.endonym(), null) }

internal fun ThemeMode.labelResource(): StringResource =
    when (this) {
      ThemeMode.SYSTEM -> Res.string.profile_theme_system
      ThemeMode.LIGHT -> Res.string.profile_theme_light
      ThemeMode.DARK -> Res.string.profile_theme_dark
    }

@Composable
internal fun movimientoCsvLabels(): MovimientoCsvLabels =
    MovimientoCsvLabels(
        dateHeader = stringResource(Res.string.csv_header_date),
        typeHeader = stringResource(Res.string.csv_header_type),
        categoryHeader = stringResource(Res.string.csv_header_category),
        parentCategoryHeader = stringResource(Res.string.csv_header_parent_category),
        descriptionHeader = stringResource(Res.string.csv_header_description),
        amountHeader = stringResource(Res.string.csv_header_amount),
        currencyHeader = stringResource(Res.string.csv_header_currency),
        expenseLabel = stringResource(Res.string.expense),
        incomeLabel = stringResource(Res.string.income),
    )
