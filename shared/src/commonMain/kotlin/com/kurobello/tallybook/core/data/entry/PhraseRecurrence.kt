package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.text.isAsciiDigit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * A recurrence the sentence states. [frecuencia] is null when the cadence has no [Frecuencia]
 * equivalent ("cada 15 días"). [anchor] is the most recent date the cadence names ("todos los
 * lunes", "el 5 de cada mes"), never after today.
 */
internal data class PhraseRecurrence(val frecuencia: Frecuencia?, val anchor: LocalDate?)

private data class RecurrenceHit(
    val start: Int,
    val endExclusive: Int,
    val recurrence: PhraseRecurrence,
    val keepsWords: Boolean = false,
)

private data class Count(val value: Long, val length: Int)

/**
 * Consumes every recurrence phrase so none of its words becomes a date, an amount or the subject,
 * except a lone adjective, which stays in the subject. The first stated cadence decides; an
 * adjective only decides when nothing more explicit is said ("compra semanal cada mes" is monthly).
 */
internal fun consumePhraseRecurrence(cursor: PhraseCursor, today: LocalDate): PhraseRecurrence? {
  val scanner = PhraseRecurrenceScanner(cursor, today)
  var firstStated: PhraseRecurrence? = null
  var firstAdjective: PhraseRecurrence? = null
  var index = 0
  while (index < cursor.tokens.size) {
    val hit = scanner.matchAt(index)
    if (hit != null && hit.keepsWords) {
      firstAdjective = firstAdjective ?: hit.recurrence
    } else if (hit != null) {
      cursor.consume(hit.start, hit.endExclusive)
      firstStated = firstStated ?: hit.recurrence
    }
    index = hit?.endExclusive ?: (index + 1)
  }
  return firstStated ?: firstAdjective
}

private class PhraseRecurrenceScanner(
    private val cursor: PhraseCursor,
    private val today: LocalDate,
) {
  // Monthly days go before intervals so "todo dia 5" is not read as "todo dia" (daily) plus 5.
  private val matchers: List<(Int) -> RecurrenceHit?> =
      listOf(
          this::dayOfEveryMonth,
          this::monthlyDayAfterLead,
          this::weekdayAfterLead,
          this::englishPluralWeekday,
          this::interval,
          this::adverb,
      )

  fun matchAt(index: Int): RecurrenceHit? =
      if (cursor.isFree(index)) matchers.firstNotNullOfOrNull { it(index) } else null

  // A bare number before "every month" is the amount ("netflix 15 every month"); only a marker
  // ("el 5", "the 1st") makes it the day.
  private fun dayOfEveryMonth(index: Int): RecurrenceHit? {
    val ordinalLength = cursor.ordinalLengthAfter(index)
    val isMarkedDay = ordinalLength > 0 || cursor.wordAt(index - 1) in dayOfMonthMarkers
    val day = cursor.dayNumberAt(index)?.takeIf { isMarkedDay }
    val suffixIndex = index + 1 + ordinalLength
    val suffix = monthlyDaySuffixes.matchAt(cursor, suffixIndex)
    return if (day == null || suffix == null) {
      null
    } else {
      var start = index
      while (cursor.wordAt(start - 1) in datePrefixes) start--
      monthlyDay(day, start, suffixIndex + suffix.length)
    }
  }

  private fun monthlyDayAfterLead(index: Int): RecurrenceHit? {
    val lead = monthlyDayLeads.matchAt(cursor, index)
    val dayIndex = index + (lead?.length ?: 0)
    val day = lead?.let { cursor.dayNumberAt(dayIndex) }
    val dayEnd = dayIndex + 1 + cursor.ordinalLengthAfter(dayIndex)
    val isOtherReading =
        cursor.wordAt(dayEnd) in recurrenceUnits || cursor.isAmountSuffixAfter(dayEnd - 1)
    return if (day == null || isOtherReading) {
      null
    } else {
      val suffixLength = monthlyDaySuffixes.matchAt(cursor, dayEnd)?.length ?: 0
      monthlyDay(day, index, dayEnd + suffixLength)
    }
  }

  private fun monthlyDay(day: Int, start: Int, endExclusive: Int): RecurrenceHit =
      RecurrenceHit(
          start,
          endExclusive,
          PhraseRecurrence(Frecuencia.MENSUAL, mostRecentOccurrence(today, day, month = null)),
      )

  private fun weekdayAfterLead(index: Int): RecurrenceHit? {
    val lead = weekdayLeads.matchAt(cursor, index)
    val dayIndex = index + (lead?.length ?: 0)
    val word = lead?.let { cursor.wordAt(dayIndex) }
    val dayOfWeek =
        if (lead?.value == WeekdayLead.PLURAL) pluralWeekdays[word] else recurringWeekdays[word]
    return dayOfWeek?.let { weekly(it, index, dayIndex) }
  }

  private fun englishPluralWeekday(index: Int): RecurrenceHit? =
      englishPluralWeekdays[cursor.wordAt(index)]?.let { weekly(it, index, index) }

  // "cada segunda semana" is every second week, not every Monday.
  private fun weekly(dayOfWeek: DayOfWeek, start: Int, dayIndex: Int): RecurrenceHit? {
    val end = dayIndex + 1 + cursor.feiraLengthAfter(dayIndex)
    return if (cursor.wordAt(end) in recurrenceUnits) {
      null
    } else {
      RecurrenceHit(
          start,
          end,
          PhraseRecurrence(Frecuencia.SEMANAL, mostRecentWeekday(today, dayOfWeek)),
      )
    }
  }

  private fun interval(index: Int): RecurrenceHit? {
    val leadHit = intervalLeads.matchAt(cursor, index)
    val lead = leadHit?.value
    val countIndex = index + (leadHit?.length ?: 0)
    val count = if (lead == RecurrenceLead.Every) cursor.countAt(countIndex) else null
    val unitIndex = countIndex + (count?.length ?: 0)
    val unitWord = cursor.wordAt(unitIndex)
    val isUnitAllowed =
        when (lead) {
          RecurrenceLead.Every -> true
          is RecurrenceLead.Per ->
              unitWord in lead.units &&
                  cursor.wordAt(index - 1) !in perLeadBlockersBefore &&
                  cursor.wordAt(unitIndex + 1) !in perLeadBlockersAfter
          null -> false
        }
    return recurrenceUnits[unitWord]
        ?.takeIf { isUnitAllowed }
        ?.let { intervalHit(index, unitIndex + 1, RecurrenceInterval(count?.value ?: 1L, it)) }
  }

  // "compra semanal", "salario mensual" and "cuota anual" are category keywords, and a lone
  // adjective can never be misread as a date or an amount, so it stays in the subject.
  private fun adverb(index: Int): RecurrenceHit? =
      recurrenceAdverbs.matchAt(cursor, index)?.let {
        intervalHit(index, index + it.length, it.value).copy(keepsWords = true)
      }

  private fun intervalHit(start: Int, endExclusive: Int, interval: RecurrenceInterval) =
      RecurrenceHit(
          start,
          endExclusive,
          PhraseRecurrence(intervalFrecuencias[interval], anchor = null),
      )
}

/** A NUMBER token or spelled number words ("treinta", "seven"); a decimal is never a count. */
private fun PhraseCursor.countAt(index: Int): Count? {
  val token = tokenAt(index)
  return if (token?.kind == PhraseTokenKind.NUMBER) {
    token.text.takeIf { text -> text.all(Char::isAsciiDigit) }?.toLongOrNull()?.let { Count(it, 1) }
  } else {
    val words = generateSequence(index) { it + 1 }.map(this::wordAt).takeWhile { it != null }
    evaluateNumberWords(words.filterNotNull().toList())?.let { Count(it.amount, it.wordCount) }
  }
}
