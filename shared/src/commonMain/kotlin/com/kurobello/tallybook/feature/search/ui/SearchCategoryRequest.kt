package com.kurobello.tallybook.feature.search.ui

import androidx.compose.runtime.Immutable

/** Asks Search to show only [categoryId]; [token] tells repeated requests for one id apart. */
@Immutable internal data class SearchCategoryRequest(val token: Long, val categoryId: String)
