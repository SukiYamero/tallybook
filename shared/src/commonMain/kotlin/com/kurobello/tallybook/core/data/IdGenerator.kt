package com.kurobello.tallybook.core.data

fun interface IdGenerator {
  fun next(): String
}
