package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.AppLocale
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.plus

/** A date expression covering tokens `[start, endExclusive)`, resolved relative to `today`. */
internal data class PhraseDate(val date: LocalDate, val start: Int, val endExclusive: Int)

internal fun findPhraseDate(
    cursor: PhraseCursor,
    today: LocalDate,
    locale: AppLocale,
): PhraseDate? = PhraseDateScanner(cursor, today, locale).find()

private data class DateMatch(val date: LocalDate, val length: Int, val takesPrefix: Boolean)

private class PhraseDateScanner(
    private val cursor: PhraseCursor,
    private val today: LocalDate,
    private val locale: AppLocale,
) {
  private val matchers: List<(Int) -> DateMatch?> =
      listOf(
          this::relativeDay,
          this::weekday,
          this::slashDate,
          this::dayThenMonth,
          this::monthThenDay,
          this::dayOfMonth,
      )

  fun find(): PhraseDate? =
      cursor.tokens.indices.firstNotNullOfOrNull { index ->
        matchers.firstNotNullOfOrNull { it(index) }?.toPhraseDate(index)
      }

  private fun DateMatch.toPhraseDate(index: Int): PhraseDate {
    var start = index
    while (takesPrefix && cursor.wordAt(start - 1) in datePrefixes) start--
    return PhraseDate(date, start, index + length)
  }

  private fun relativeDay(index: Int): DateMatch? {
    val hit = relativeDays.matchAt(cursor, index)
    val isMorning = hit != null && hit.value > 0 && cursor.wordAt(index - 1) in morningDeterminers
    return hit?.takeUnless { isMorning }
        ?.let { DateMatch(today.plus(it.value, DateTimeUnit.DAY), it.length, takesPrefix = false) }
  }

  private fun weekday(index: Int): DateMatch? {
    val word = cursor.wordAt(index)
    val dayOfWeek = weekdays[word] ?: return null
    val feiraLength = cursor.feiraLengthAfter(index)
    val needsContext =
        word in ambiguousWeekdays && feiraLength == 0 && cursor.wordAt(index - 1) != "na"
    val daysBack = (today.dayOfWeek.ordinal - dayOfWeek.ordinal).mod(DAYS_IN_WEEK)
    return DateMatch(today.plus(-daysBack, DateTimeUnit.DAY), 1 + feiraLength, takesPrefix = true)
        .takeUnless { needsContext }
  }

  private fun slashDate(index: Int): DateMatch? =
      cursor
          .tokenAt(index)
          ?.takeIf { it.kind == PhraseTokenKind.SLASH_DATE }
          ?.let { resolveSlashDate(it.text.split('/')) }
          ?.let { DateMatch(it, 1, takesPrefix = true) }

  private fun resolveSlashDate(parts: List<String>): LocalDate? {
    val (first, second) = parts.take(2).map(String::toIntOrNull)
    val (day, month) = if (locale == AppLocale.EN) second to first else first to second
    return when {
      day == null || month == null -> null
      parts.size == 2 -> mostRecentOccurrence(today, day, month)
      else -> expandYear(parts[2])?.let { safeDate(it, month, day) }
    }
  }

  private fun dayThenMonth(index: Int): DateMatch? {
    val day = cursor.dayNumberAt(index)
    var monthIndex = index + 1 + cursor.ordinalLengthAfter(index)
    if (cursor.wordAt(monthIndex) in monthConnectors) monthIndex++
    val month = months[cursor.wordAt(monthIndex)]
    return if (day == null || month == null) {
      null
    } else {
      mostRecentOccurrence(today, day, month)?.let {
        DateMatch(it, monthIndex + 1 - index, takesPrefix = true)
      }
    }
  }

  private fun monthThenDay(index: Int): DateMatch? {
    val month = months[cursor.wordAt(index)]
    var dayIndex = index + 1
    if (cursor.wordAt(dayIndex) in monthConnectors) dayIndex++
    val day = cursor.dayNumberAt(dayIndex)
    val length = dayIndex + 1 + cursor.ordinalLengthAfter(dayIndex) - index
    return if (month == null || day == null || cursor.isAmountSuffixAfter(index + length - 1)) {
      null
    } else {
      mostRecentOccurrence(today, day, month)?.let { DateMatch(it, length, takesPrefix = true) }
    }
  }

  private fun dayOfMonth(index: Int): DateMatch? {
    val day = cursor.dayNumberAt(index)
    val length = 1 + cursor.ordinalLengthAfter(index)
    val isMarked = cursor.wordAt(index - 1) in dayOfMonthMarkers
    return if (day == null || !isMarked || cursor.isAmountSuffixAfter(index + length - 1)) {
      null
    } else {
      mostRecentOccurrence(today, day, month = null)?.let {
        DateMatch(it, length, takesPrefix = true)
      }
    }
  }
}

// Portuguese "segunda-feira" tokenizes as segunda, "-", feira.
private fun PhraseCursor.feiraLengthAfter(index: Int): Int {
  val hyphen = if (tokenAt(index + 1)?.text == "-") 1 else 0
  return if (wordAt(index + 1 + hyphen) == "feira") hyphen + 1 else 0
}

private fun PhraseCursor.dayNumberAt(index: Int): Int? =
    tokenAt(index)
        ?.takeIf { it.kind == PhraseTokenKind.NUMBER && it.text.length <= 2 }
        ?.text
        ?.toIntOrNull()
        ?.takeIf { it in 1..MAX_DAY_OF_MONTH }

private fun PhraseCursor.ordinalLengthAfter(index: Int): Int {
  val suffix = tokenAt(index + 1)
  val isOrdinal =
      suffix != null && suffix.isAdjacentAfter(tokens[index]) && suffix.norm in ordinalSuffixes
  return if (isOrdinal) 1 else 0
}

// "octubre 12 mil" and "el 5 pesos" read as amounts, not as days.
private fun PhraseCursor.isAmountSuffixAfter(lastIndex: Int): Boolean {
  val word = wordAt(lastIndex + 1)
  return word != null &&
      (word in digitMultipliers ||
          word in currencyWords ||
          (word in adjacentMultipliers && tokens[lastIndex + 1].isAdjacentAfter(tokens[lastIndex])))
}

private fun expandYear(digits: String): Int? =
    when (digits.length) {
      TWO_DIGIT_YEAR -> TWO_DIGIT_YEAR_BASE + digits.toInt()
      FOUR_DIGIT_YEAR -> digits.toInt()
      else -> null
    }

/** The latest date not after [today] with this day and optional month; null if none exists. */
private fun mostRecentOccurrence(today: LocalDate, day: Int, month: Int?): LocalDate? =
    (0..MAX_LOOKBACK_PERIODS)
        .asSequence()
        .mapNotNull { back ->
          if (month != null) {
            safeDate(today.year - back, month, day)
          } else {
            val monthIndex = today.year * MONTHS_PER_YEAR + today.month.ordinal - back
            safeDate(monthIndex / MONTHS_PER_YEAR, monthIndex % MONTHS_PER_YEAR + 1, day)
          }
        }
        .firstOrNull { it <= today }

private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
    Month.entries
        .getOrNull(month - 1)
        ?.takeIf { day in 1..YearMonth(year, it).numberOfDays }
        ?.let { LocalDate(year, it, day) }

private val DAYS_IN_WEEK = DayOfWeek.entries.size
private const val MONTHS_PER_YEAR = 12
private const val MAX_DAY_OF_MONTH = 31
private const val MAX_LOOKBACK_PERIODS = 12
private const val TWO_DIGIT_YEAR = 2
private const val FOUR_DIGIT_YEAR = 4
private const val TWO_DIGIT_YEAR_BASE = 2000
