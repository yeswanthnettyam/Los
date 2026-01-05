package com.kaleidofin.originator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaleidofin.originator.domain.usecase.GetHomeActionsUseCase
import com.kaleidofin.originator.presentation.ui.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeActionsUseCase: GetHomeActionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeActions()
    }

    private fun loadHomeActions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val homeScreenData = getHomeActionsUseCase()
                _uiState.update {
                    it.copy(
                        actions = homeScreenData.actions,
                        layout = homeScreenData.layout,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load actions"
                    )
                }
            }
        }
    }

    fun retry() {
        loadHomeActions()
    }
}


