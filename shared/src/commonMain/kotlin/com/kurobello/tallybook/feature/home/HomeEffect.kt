package com.kurobello.tallybook.feature.home

internal sealed interface HomeEffect {

  data object NavigateNext : HomeEffect
}
