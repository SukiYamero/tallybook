package com.kurobello.tallybook.core.ui.component

/**
 * Keeps returning the last value seen while a surface was visible, so its exit animation plays over
 * what the user was looking at even when the state behind it resets on the way out. A holder
 * instead of Compose state because it is written during composition and must not trigger one.
 */
internal class LastWhileVisible<T>(initial: T) {
  private var value = initial

  fun resolve(visible: Boolean, current: T): T {
    if (visible) value = current
    return value
  }
}
