package com.kurobello.tallybook.core.model

/**
 * Entries are in picker order, grouped by hue. [storageKey] is the persisted value and never
 * follows a rename, which is why TEAL, INDIGO, RED and ORANGE keep keys that no longer match their
 * names.
 */
enum class CategoryTint(val storageKey: String) {
  EMERALD("emerald"),
  TEAL("success"),
  BLUE("blue"),
  INDIGO("info"),
  PURPLE("purple"),
  PINK("pink"),
  RED("rose"),
  ORANGE("danger"),
  AMBER("amber"),
  NEUTRAL("neutral"),
}
