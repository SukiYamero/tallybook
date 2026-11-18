@file:Suppress("MagicNumber")

package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.DayOfWeek

// Every table is keyed by search-normalized text (lowercase, accents folded except ñ), and the
// vocabulary is the union of es, es-AR, en and pt-BR because users mix languages.

internal fun <V> phrases(value: V, vararg entries: String): Map<List<String>, V> =
    entries.associate {
      it.split(' ') to value
    }

internal val tipoVerbs: PhraseTable<TipoMovimiento> =
    PhraseTable(
        phrases(
            TipoMovimiento.GASTO,
            "gaste",
            "pague",
            "compre",
            "me cobraron",
            "spent",
            "paid",
            "bought",
            "gastei",
            "paguei",
            "comprei",
        ) +
            phrases(
                TipoMovimiento.INGRESO,
                "recibi",
                "me pagaron",
                "cobre",
                "vendi",
                "me depositaron",
                "gane",
                "received",
                "got paid",
                "earned",
                "sold",
                "recebi",
                "ganhei",
                "me pagaram",
                "me depositaram",
            )
    )

internal val numberWordValues: Map<String, Long> =
    mapOf(
        "un" to 1,
        "uno" to 1,
        "una" to 1,
        "one" to 1,
        "um" to 1,
        "uma" to 1,
        "dos" to 2,
        "two" to 2,
        "dois" to 2,
        "duas" to 2,
        "tres" to 3,
        "three" to 3,
        "cuatro" to 4,
        "four" to 4,
        "quatro" to 4,
        "cinco" to 5,
        "five" to 5,
        "seis" to 6,
        "six" to 6,
        "siete" to 7,
        "seven" to 7,
        "sete" to 7,
        "ocho" to 8,
        "eight" to 8,
        "oito" to 8,
        "nueve" to 9,
        "nine" to 9,
        "nove" to 9,
        "diez" to 10,
        "ten" to 10,
        "dez" to 10,
        "once" to 11,
        "eleven" to 11,
        "onze" to 11,
        "doce" to 12,
        "twelve" to 12,
        "doze" to 12,
        "trece" to 13,
        "thirteen" to 13,
        "treze" to 13,
        "catorce" to 14,
        "fourteen" to 14,
        "catorze" to 14,
        "quatorze" to 14,
        "quince" to 15,
        "fifteen" to 15,
        "quinze" to 15,
        "dieciseis" to 16,
        "sixteen" to 16,
        "dezesseis" to 16,
        "diecisiete" to 17,
        "seventeen" to 17,
        "dezessete" to 17,
        "dieciocho" to 18,
        "eighteen" to 18,
        "dezoito" to 18,
        "diecinueve" to 19,
        "nineteen" to 19,
        "dezenove" to 19,
        "veinte" to 20,
        "twenty" to 20,
        "vinte" to 20,
        "veintiuno" to 21,
        "veintiun" to 21,
        "veintiuna" to 21,
        "veintidos" to 22,
        "veintitres" to 23,
        "veinticuatro" to 24,
        "veinticinco" to 25,
        "veintiseis" to 26,
        "veintisiete" to 27,
        "veintiocho" to 28,
        "veintinueve" to 29,
        "treinta" to 30,
        "thirty" to 30,
        "trinta" to 30,
        "cuarenta" to 40,
        "forty" to 40,
        "quarenta" to 40,
        "cincuenta" to 50,
        "fifty" to 50,
        "cinquenta" to 50,
        "sesenta" to 60,
        "sixty" to 60,
        "sessenta" to 60,
        "setenta" to 70,
        "seventy" to 70,
        "oitenta" to 80,
        "eighty" to 80,
        "ochenta" to 80,
        "noventa" to 90,
        "ninety" to 90,
        "cien" to 100,
        "ciento" to 100,
        "cem" to 100,
        "cento" to 100,
        "doscientos" to 200,
        "duzentos" to 200,
        "trescientos" to 300,
        "trezentos" to 300,
        "cuatrocientos" to 400,
        "quatrocentos" to 400,
        "quinientos" to 500,
        "quinhentos" to 500,
        "seiscientos" to 600,
        "seiscentos" to 600,
        "setecientos" to 700,
        "setecentos" to 700,
        "ochocientos" to 800,
        "oitocentos" to 800,
        "novecientos" to 900,
        "novecentos" to 900,
    )

internal const val HUNDRED_FACTOR = 100L
internal const val THOUSAND_FACTOR = 1_000L
internal const val MILLION_FACTOR = 1_000_000L

internal val scaleWords: Map<String, Long> =
    mapOf(
        "hundred" to HUNDRED_FACTOR,
        "mil" to THOUSAND_FACTOR,
        "thousand" to THOUSAND_FACTOR,
        "millon" to MILLION_FACTOR,
        "millones" to MILLION_FACTOR,
        "million" to MILLION_FACTOR,
        "millions" to MILLION_FACTOR,
        "milhao" to MILLION_FACTOR,
        "milhoes" to MILLION_FACTOR,
    )

// "dos pasajes" and "one coffee" are not amounts, but "dos mil", "cinco dólares" and "un millón"
// are.
internal val numberWordUnits: Set<String> = numberWordValues.filterValues { it in 1..9 }.keys

// "millones" alone is not an amount; "mil" and "hundred" are ("mil pesos", "hundred bucks").
internal val bareMagnitudeWords: Set<String> = scaleWords.filterValues { it >= MILLION_FACTOR }.keys

internal val numberWordConnectors: Set<String> = setOf("y", "e", "and")

// Shares words with numberWordConnectors safely: the "y" of "treinta y cinco" lies inside its
// amount, and only tokens between two amounts or after the last one are read as part connectors.
internal val partConnectors: Set<String> =
    setOf("y", "e", "and", "mas", "plus", "mais", "tambien", "also", "tambem")

// A comma inside "1.500,50" is part of the number token, so only a standalone comma gets here.
internal val partConnectorPunctuation: Set<String> = setOf(",", "+")

// Words about how the whole sentence was paid ("más lo cargué con la tarjeta"). They decide no
// tipo,
// so they are not tipo verbs; they only let a chunk after the last amount open the shared tail.
internal val sharedTailWords: Set<String> = setOf("cargue", "carguei", "charged", "it", "them")

internal val slangMultipliers: Map<String, Long> =
    mapOf(
        "lucas" to THOUSAND_FACTOR,
        "luca" to THOUSAND_FACTOR,
        "lukas" to THOUSAND_FACTOR,
        "palos" to MILLION_FACTOR,
        "palo" to MILLION_FACTOR,
        "mangos" to 1,
        "mango" to 1,
    )

/** Multipliers valid after digits: scale words (except "hundred") plus regional slang. */
internal val digitMultipliers: Map<String, Long> = (scaleWords - "hundred") + slangMultipliers

/** "20k" and "1.5M" only count when glued to the digits; a spaced "m" or "k" is just a word. */
internal val adjacentMultipliers: Map<String, Long> =
    mapOf("k" to THOUSAND_FACTOR, "m" to MILLION_FACTOR)

internal val currencyWords: Set<String> =
    setOf(
        "peso",
        "pesos",
        "dolar",
        "dolares",
        "dollar",
        "dollars",
        "bucks",
        "usd",
        "cop",
        "clp",
        "brl",
        "mxn",
        "ars",
        "pen",
        "dop",
        "reais",
        "reales",
        "soles",
        "euro",
        "euros",
    )

internal val weekdays: Map<String, DayOfWeek> =
    mapOf(
        "lunes" to DayOfWeek.MONDAY,
        "monday" to DayOfWeek.MONDAY,
        "segunda" to DayOfWeek.MONDAY,
        "martes" to DayOfWeek.TUESDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "terca" to DayOfWeek.TUESDAY,
        "miercoles" to DayOfWeek.WEDNESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "quarta" to DayOfWeek.WEDNESDAY,
        "jueves" to DayOfWeek.THURSDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "quinta" to DayOfWeek.THURSDAY,
        "viernes" to DayOfWeek.FRIDAY,
        "friday" to DayOfWeek.FRIDAY,
        "sexta" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )

internal val feiraWords: Set<String> = setOf("feira", "feiras")

// "segunda" alone is also "second" or "second-hand" in Spanish, so it needs "feira" or "na".
internal val ambiguousWeekdays: Set<String> = setOf("segunda")

internal val months: Map<String, Int> =
    mapOf(
        "enero" to 1,
        "january" to 1,
        "janeiro" to 1,
        "febrero" to 2,
        "february" to 2,
        "fevereiro" to 2,
        "marzo" to 3,
        "march" to 3,
        "marco" to 3,
        "abril" to 4,
        "april" to 4,
        "mayo" to 5,
        "may" to 5,
        "maio" to 5,
        "junio" to 6,
        "june" to 6,
        "junho" to 6,
        "julio" to 7,
        "july" to 7,
        "julho" to 7,
        "agosto" to 8,
        "august" to 8,
        "septiembre" to 9,
        "setiembre" to 9,
        "september" to 9,
        "setembro" to 9,
        "octubre" to 10,
        "october" to 10,
        "outubro" to 10,
        "noviembre" to 11,
        "november" to 11,
        "novembro" to 11,
        "diciembre" to 12,
        "december" to 12,
        "dezembro" to 12,
    )

/** Day offsets from today; positive offsets are future expressions. */
internal val relativeDays: PhraseTable<Int> =
    PhraseTable(
        phrases(0, "hoy", "today", "hoje") +
            phrases(-1, "ayer", "yesterday", "ontem") +
            phrases(
                -2,
                "anteayer",
                "antier",
                "anteontem",
                "antes de ayer",
                "day before yesterday",
            ) +
            phrases(1, "mañana", "manana", "tomorrow", "amanha") +
            phrases(
                2,
                "pasado mañana",
                "pasado manana",
                "day after tomorrow",
                "depois de amanha",
            )
    )

// "la mañana" is the morning, not tomorrow.
internal val morningDeterminers: Set<String> = setOf("la", "esta")

internal val datePrefixes: Set<String> = setOf("el", "dia", "on", "the", "em", "no", "na")

// A bare number is an amount unless one of these makes it a day of the month ("el 12").
internal val dayOfMonthMarkers: Set<String> = setOf("el", "dia", "on", "the")

internal val monthConnectors: Set<String> = setOf("de", "del", "of")

internal val ordinalSuffixes: Set<String> = setOf("th", "st", "nd", "rd")

internal val medioWords: PhraseTable<MedioWord> =
    PhraseTable(
        phrases(
            MedioWord.Explicit(MedioPago.EFECTIVO),
            "efectivo",
            "en efectivo",
            "cash",
            "en cash",
            "in cash",
            "with cash",
            "dinheiro",
            "em dinheiro",
            "no dinheiro",
            "dinheiro vivo",
            "em especie",
            "con efectivo",
        ) +
            phrases(
                MedioWord.Explicit(MedioPago.DEBITO),
                "con debito",
                "tarjeta debito",
                "tarjeta de debito",
                "con la de debito",
                "debit card",
                "with debit",
                "cartao de debito",
            ) +
            phrases(
                MedioWord.Explicit(MedioPago.CREDITO),
                "con credito",
                "tarjeta credito",
                "tarjeta de credito",
                "con la de credito",
                "credit card",
                "with credit",
                "cartao de credito",
            ) +
            phrases(
                MedioWord.Explicit(MedioPago.TRANSFERENCIA),
                "transferencia",
                "por transferencia",
                "con transferencia",
                "by transfer",
                "bank transfer",
                "wire transfer",
                "pix",
                "por pix",
                "via pix",
                "no pix",
                "pelo pix",
            ) +
            phrases(
                MedioWord.AnyCard,
                "con tarjeta",
                "con la tarjeta",
                "con mi tarjeta",
                "with card",
                "with a card",
                "with my card",
                "by card",
                "no cartao",
                "com cartao",
                "com o cartao",
                "com meu cartao",
            ) +
            // Category keywords that contain a method word but name what was bought or received.
            phrases(MedioWord.NotMedio, "cash back", "petty cash", "transferencia recibida")
    )

internal val negationWords: Set<String> = setOf("no", "sin", "not", "without", "sem", "nao")

// "sin tarjeta" and "sem cartão" negate a card without naming a method phrase.
internal val cardNouns: Set<String> = setOf("tarjeta", "cartao", "card")

/** First words of a method phrase that names the card itself rather than how it was used. */
internal val cardLeadWords: Set<String> = setOf("tarjeta", "cartao", "credit", "debit")

// A named card preceded by these is the thing being paid ("pago tarjeta de crédito", "fatura do
// cartão"), not the payment method; the category suggester still needs those words.
internal val cardObjectMarkers: Set<String> =
    setOf("de", "del", "do", "da", "of", "pago", "pagar", "pay", "fatura", "factura", "cuota")

internal val cardDeterminers: Set<String> = setOf("la", "el", "mi", "the", "my", "a", "o", "meu")

// A determiner only makes the card an object when no preposition introduces it ("con mi tarjeta").
internal val methodPrepositions: Set<String> =
    setOf("con", "en", "por", "with", "by", "on", "in", "com", "em", "no", "na", "pelo", "via")

internal val cardObjectFollowers: Set<String> = setOf("bill", "payment", "statement")
