package com.kaleidofin.originator.presentation.ui.state

import com.kaleidofin.originator.domain.model.DashboardFlow
import com.kaleidofin.originator.domain.model.HomeAction
import com.kaleidofin.originator.domain.model.HomeLayout

/**
 * HomeUiState - State for backend-driven dashboard
 * 
 * Supports both:
 * - Legacy: HomeAction list (deprecated, for backward compatibility)
 * - New: DashboardFlow list (from GET /api/v1/dashboard/flows)
 */
data class HomeUiState(
    // Legacy support (deprecated)
    @Deprecated("Use dashboardFlows instead")
    val actions: List<HomeAction> = emptyList(),
    @Deprecated("Use dashboardFlows instead")
    val layout: HomeLayout? = null,
    
    // New backend-driven dashboard
    val dashboardFlows: List<DashboardFlow> = emptyList(),
    
    // Common state
    val isLoading: Boolean = false,
    val error: String? = null
)


