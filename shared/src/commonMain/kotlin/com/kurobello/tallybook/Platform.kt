package com.kurobello.tallybook

interface Platform {
  val name: String
}

expect fun getPlatform(): Platform
