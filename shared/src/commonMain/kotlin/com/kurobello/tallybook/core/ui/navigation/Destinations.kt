package com.kurobello.tallybook.core.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface Destination : NavKey

@Serializable data object Home : Destination

@Serializable data object Placeholder2 : Destination
