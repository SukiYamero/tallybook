package com.kurobello.tallybook.core.text

/** Caps at [max] UTF-16 units without leaving half of a surrogate pair at the end. */
internal fun String.capTo(max: Int): String {
  if (length <= max) return this
  val capped = take(max)
  return if (capped.last().isHighSurrogate()) capped.dropLast(1) else capped
}
