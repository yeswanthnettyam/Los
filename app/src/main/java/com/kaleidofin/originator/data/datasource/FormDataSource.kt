package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.*

interface FormDataSource {
    /**
     * Runtime API - Single method for all navigation
     * Calls POST /runtime/next-screen
     * 
     * Usage:
     * 1. First load: currentScreenId = null, formData = null
     * 2. Next screen: currentScreenId = current, formData = form values
     * 
     * Backend handles:
     * - Flow evaluation
     * - Config resolution
     * - Snapshot management
     */
    /**
     * Dashboard API - Get available flows
     * GET /api/v1/dashboard/flows
     */
    suspend fun getDashboardFlows(): DashboardResponseDto
    
    suspend fun nextScreen(
        applicationId: String,
        currentScreenId: String? = null,
        formData: Map<String, Any>? = null
    ): NextScreenResponseDto
    
    suspend fun getMasterData(dataSource: String): Map<String, String>
    
    fun updateFormData(screenId: String, formData: Map<String, Any>) // For testing: Update dummy JSON
    
    // Legacy APIs - Deprecated: Use Runtime API instead
    @Deprecated("Use nextScreen() instead")
    suspend fun startFlow(applicationId: String, flowType: String? = null): FlowResponseDto
    
    @Deprecated("Use nextScreen() instead")
    suspend fun navigateNext(applicationId: String, currentScreenId: String, formData: Map<String, Any>): FlowResponseDto
    
    @Deprecated("Use nextScreen() instead")
    suspend fun navigateBack(applicationId: String, currentScreenId: String): FlowResponseDto
    
    @Deprecated("Use nextScreen() instead")
    suspend fun getFormConfiguration(target: String): FormScreenDto
}

