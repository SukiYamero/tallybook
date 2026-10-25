package com.kurobello.tallybook.core.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface Destination : NavKey

@Serializable data object Home : Destination

@Serializable data object History : Destination

@Serializable data object Search : Destination

@Serializable data object Statistics : Destination

@Serializable data object Profile : Destination

@Serializable data object UiCatalog : Destination

@Serializable data object Categories : Destination

@Serializable data object Tags : Destination

@Serializable data object Accounts : Destination
