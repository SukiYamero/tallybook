package com.kurobello.tallybook.core.model

import kotlinx.datetime.LocalDate

/**
 * Every year from [FIRST_SELECTABLE_YEAR] through today, plus older years that hold movimientos or
 * that the caller retains (the selected one, or one whose movimientos were deleted while the entry
 * was open), newest first. Future years never appear.
 */
internal fun selectableYears(
    persistedYears: Collection<Int>,
    today: LocalDate,
    retainedYears: Collection<Int> = emptySet(),
): List<Int> =
    ((FIRST_SELECTABLE_YEAR..today.year) + persistedYears + retainedYears + today.year)
        .filter { it <= today.year }
        .distinct()
        .sortedDescending()
