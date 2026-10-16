package com.kurobello.tallybook.core.text

/** Unlike [Char.isDigit], excludes the digits of other scripts that never parse as amounts. */
internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
