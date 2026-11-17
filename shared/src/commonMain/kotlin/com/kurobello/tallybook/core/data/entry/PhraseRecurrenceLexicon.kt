@file:Suppress("MagicNumber")

package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Frecuencia
import kotlinx.datetime.DayOfWeek

// Keyed by search-normalized text like PhraseLexicon: "mês" is "mes", while "año" keeps its ñ.

internal enum class RecurrenceUnit {
  DAY,
  WEEK,
  MONTH,
  YEAR,
}

internal data class RecurrenceInterval(val count: Long, val unit: RecurrenceUnit)

internal sealed interface RecurrenceLead {
  /** Takes an optional count: "cada 30 días", "every month". */
  data object Every : RecurrenceLead

  /** Takes no count and only the [units] its language pairs it with: "al mes", "per month". */
  data class Per(val units: Set<String>) : RecurrenceLead
}

/**
 * [PLURAL] leads only make a weekday recurring in its plural form: "on mondays", not "on monday".
 */
internal enum class WeekdayLead {
  ANY,
  PLURAL,
}

/** Intervals without an entry have no [Frecuencia] and are flagged instead. */
internal val intervalFrecuencias: Map<RecurrenceInterval, Frecuencia> =
    mapOf(
        RecurrenceInterval(7, RecurrenceUnit.DAY) to Frecuencia.SEMANAL,
        RecurrenceInterval(1, RecurrenceUnit.WEEK) to Frecuencia.SEMANAL,
        RecurrenceInterval(4, RecurrenceUnit.WEEK) to Frecuencia.MENSUAL,
        RecurrenceInterval(1, RecurrenceUnit.MONTH) to Frecuencia.MENSUAL,
        RecurrenceInterval(52, RecurrenceUnit.WEEK) to Frecuencia.ANUAL,
        RecurrenceInterval(12, RecurrenceUnit.MONTH) to Frecuencia.ANUAL,
        RecurrenceInterval(1, RecurrenceUnit.YEAR) to Frecuencia.ANUAL,
    ) +
        (28L..31L).associate { RecurrenceInterval(it, RecurrenceUnit.DAY) to Frecuencia.MENSUAL } +
        (365L..366L).associate { RecurrenceInterval(it, RecurrenceUnit.DAY) to Frecuencia.ANUAL }

// Bare "diario" is left out on purpose: it is also a noun ("el diario", the newspaper).
internal val recurrenceAdverbs: PhraseTable<RecurrenceInterval> =
    PhraseTable(
        phrases(
            RecurrenceInterval(1, RecurrenceUnit.WEEK),
            "semanal",
            "semanales",
            "semanalmente",
            "semanais",
            "weekly",
        ) +
            phrases(
                RecurrenceInterval(1, RecurrenceUnit.MONTH),
                "mensual",
                "mensuales",
                "mensualmente",
                "mensal",
                "mensais",
                "mensalmente",
                "monthly",
            ) +
            phrases(
                RecurrenceInterval(1, RecurrenceUnit.YEAR),
                "anual",
                "anuales",
                "anualmente",
                "anuais",
                "annual",
                "annually",
                "yearly",
            ) +
            phrases(
                RecurrenceInterval(1, RecurrenceUnit.DAY),
                "diariamente",
                "daily",
            ) +
            phrases(
                RecurrenceInterval(15, RecurrenceUnit.DAY),
                "quincenal",
                "quincenales",
                "quinzenal",
                "quinzenais",
            ) +
            phrases(RecurrenceInterval(2, RecurrenceUnit.WEEK), "biweekly", "fortnightly") +
            phrases(
                RecurrenceInterval(2, RecurrenceUnit.MONTH),
                "bimestral",
                "bimestrales",
                "bimestrais",
                "bimonthly",
            ) +
            phrases(
                RecurrenceInterval(3, RecurrenceUnit.MONTH),
                "trimestral",
                "trimestrales",
                "trimestrais",
                "quarterly",
            ) +
            phrases(
                RecurrenceInterval(6, RecurrenceUnit.MONTH),
                "semestral",
                "semestrales",
                "semestrais",
                "semiannual",
                "semiannually",
            )
    )

// No per-lead takes a day: "200000 por día" is a daily price, not a recurrence. A bare "a" is only
// English, since Portuguese "a semana passada" is last week.
internal val intervalLeads: PhraseTable<RecurrenceLead> =
    PhraseTable(
        phrases(
            RecurrenceLead.Every,
            "cada",
            "a cada",
            "every",
            "each",
            "todos los",
            "todas las",
            "todo",
            "toda",
            "todos os",
            "todas as",
        ) +
            phrases(RecurrenceLead.Per(setOf("semana", "mes", "año", "ano")), "por") +
            phrases(RecurrenceLead.Per(setOf("mes", "año")), "al") +
            phrases(RecurrenceLead.Per(setOf("semana")), "a la") +
            phrases(RecurrenceLead.Per(setOf("mes", "ano")), "ao") +
            phrases(RecurrenceLead.Per(setOf("week", "month", "year")), "per", "a")
    )

internal val recurrenceUnits: Map<String, RecurrenceUnit> =
    mapOf(
        "dia" to RecurrenceUnit.DAY,
        "dias" to RecurrenceUnit.DAY,
        "day" to RecurrenceUnit.DAY,
        "days" to RecurrenceUnit.DAY,
        "semana" to RecurrenceUnit.WEEK,
        "semanas" to RecurrenceUnit.WEEK,
        "week" to RecurrenceUnit.WEEK,
        "weeks" to RecurrenceUnit.WEEK,
        "mes" to RecurrenceUnit.MONTH,
        "meses" to RecurrenceUnit.MONTH,
        "month" to RecurrenceUnit.MONTH,
        "months" to RecurrenceUnit.MONTH,
        "año" to RecurrenceUnit.YEAR,
        "años" to RecurrenceUnit.YEAR,
        "ano" to RecurrenceUnit.YEAR,
        "anos" to RecurrenceUnit.YEAR,
        "year" to RecurrenceUnit.YEAR,
        "years" to RecurrenceUnit.YEAR,
    )

// "for a month" and "in a week" are durations, "a year ago" and "al mes siguiente" dates, "por
// semana
// santa" and "por año nuevo" holidays, and "2 veces por semana" a frequency Repetir cannot express.
internal val perLeadBlockersBefore: Set<String> =
    setOf("for", "durante", "in", "within", "veces", "vez", "times", "vezes")
internal val perLeadBlockersAfter: Set<String> =
    setOf("ago", "santa", "siguiente", "seguinte", "nuevo", "novo", "proximo", "next")

internal val weekdayLeads: PhraseTable<WeekdayLead> =
    PhraseTable(
        phrases(
            WeekdayLead.ANY,
            "cada",
            "todos los",
            "todas las",
            "every",
            "each",
            "todo",
            "toda",
            "todos os",
            "todas as",
        ) + phrases(WeekdayLead.PLURAL, "los", "on", "aos", "as")
    )

/** Spoken without a lead they already mean every week ("gym mondays"). */
internal val englishPluralWeekdays: Map<String, DayOfWeek> =
    mapOf(
        "mondays" to DayOfWeek.MONDAY,
        "tuesdays" to DayOfWeek.TUESDAY,
        "wednesdays" to DayOfWeek.WEDNESDAY,
        "thursdays" to DayOfWeek.THURSDAY,
        "fridays" to DayOfWeek.FRIDAY,
        "saturdays" to DayOfWeek.SATURDAY,
        "sundays" to DayOfWeek.SUNDAY,
    )

// Spanish lunes to viernes are the same word in the plural.
internal val pluralWeekdays: Map<String, DayOfWeek> =
    englishPluralWeekdays +
        mapOf(
            "lunes" to DayOfWeek.MONDAY,
            "martes" to DayOfWeek.TUESDAY,
            "miercoles" to DayOfWeek.WEDNESDAY,
            "jueves" to DayOfWeek.THURSDAY,
            "viernes" to DayOfWeek.FRIDAY,
            "sabados" to DayOfWeek.SATURDAY,
            "domingos" to DayOfWeek.SUNDAY,
            "segundas" to DayOfWeek.MONDAY,
            "tercas" to DayOfWeek.TUESDAY,
            "quartas" to DayOfWeek.WEDNESDAY,
            "quintas" to DayOfWeek.THURSDAY,
            "sextas" to DayOfWeek.FRIDAY,
        )

internal val recurringWeekdays: Map<String, DayOfWeek> = weekdays + pluralWeekdays

/** "el 5 de cada mes", "the 5th of every month". */
internal val monthlyDaySuffixes: PhraseTable<Frecuencia> =
    PhraseTable(
        phrases(
            Frecuencia.MENSUAL,
            "de cada mes",
            "de todo mes",
            "de todos los meses",
            "de todos os meses",
            "of every month",
            "of each month",
            "every month",
            "each month",
        )
    )

/** "todos los 5", "todo dia 5". */
internal val monthlyDayLeads: PhraseTable<Frecuencia> =
    PhraseTable(phrases(Frecuencia.MENSUAL, "todos los", "todo dia"))
