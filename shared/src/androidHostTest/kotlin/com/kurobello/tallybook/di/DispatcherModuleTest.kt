package com.kurobello.tallybook.di

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication

class DispatcherModuleTest {

  @Test
  fun `IO qualifier resolves the IO dispatcher, not MAIN or DEFAULT`() {
    val koin = koinApplication { modules(dispatcherModule) }.koin

    val io = koin.get<CoroutineDispatcher>(named(DispatcherQualifier.IO))
    val default = koin.get<CoroutineDispatcher>(named(DispatcherQualifier.DEFAULT))
    val main = koin.get<CoroutineDispatcher>(named(DispatcherQualifier.MAIN))

    assertEquals(Dispatchers.IO, io)
    assertEquals(Dispatchers.Default, default)
    assertEquals(Dispatchers.Main, main)
    assertNotEquals(io, main)
    assertNotEquals(io, default)
  }
}
