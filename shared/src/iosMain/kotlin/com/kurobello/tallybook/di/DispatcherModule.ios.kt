package com.kurobello.tallybook.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// kotlinx.coroutines has no Dispatchers.IO on Native targets — Default is the closest equivalent.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
