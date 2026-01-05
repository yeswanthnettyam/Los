package com.kaleidofin.originator.presentation.ui.state

import com.kaleidofin.originator.domain.model.HomeAction
import com.kaleidofin.originator.domain.model.HomeLayout

data class HomeUiState(
    val actions: List<HomeAction> = emptyList(),
    val layout: HomeLayout? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)


