package com.kaleidofin.originator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.mapper.toDomain
import com.kaleidofin.originator.domain.model.DashboardFlow
import com.kaleidofin.originator.presentation.ui.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel - Manages backend-driven dashboard
 * 
 * Dashboard is fully dynamic:
 * - Fetches flows from GET /api/v1/dashboard/flows
 * - Each flow includes UI metadata (colors, icons)
 * - On click: calls Runtime API with currentScreenId = null
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val formDataSource: FormDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardFlows()
    }

    /**
     * Load dashboard flows from backend
     * GET /api/v1/dashboard/flows
     * 
     * Backend returns:
     * - Flow metadata (title, description)
     * - UI customization (colors, icon keys)
     * - Startable flag
     */
    private fun loadDashboardFlows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val response = formDataSource.getDashboardFlows()
                val dashboardFlows = response.flows.map { it.toDomain() }
                
                _uiState.update {
                    it.copy(
                        dashboardFlows = dashboardFlows,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }
    
    /**
     * Start flow from dashboard card click
     * 
     * Calls Runtime API: POST /api/v1/runtime/next-screen
     * with currentScreenId = null (for flow initiation)
     * 
     * Backend responsibilities:
     * - Evaluate flow conditions
     * - Resolve first screen
     * - Return screenId + screenConfig atomically
     */
    fun onFlowClick(
        flow: DashboardFlow,
        onSuccess: (screenId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Call Runtime API - POST /api/v1/runtime/next-screen
                // currentScreenId = null to initiate flow
                // Backend resolves flow and returns first screen + config
                val response = formDataSource.nextScreen(
                    applicationId = null, // Not sent
                    currentScreenId = null, // null for first load
                    flowId = flow.flowId,
                    productCode = flow.productCode,
                    partnerCode = flow.partnerCode?.takeIf { it.isNotBlank() } ?: "SAMASTA", // Default to SAMASTA for testing
                    branchCode = flow.branchCode,
                    formData = emptyMap() // Empty object {} for first load
                )
                
                _uiState.update { it.copy(isLoading = false) }
                
                // Navigate to dynamic form with resolved screenId
                onSuccess(response.nextScreenId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start flow"
                    )
                }
                onError(e.message ?: "Failed to start flow")
            }
        }
    }
    
    /**
     * Retry loading dashboard flows
     */
    fun retry() {
        loadDashboardFlows()
    }
}
