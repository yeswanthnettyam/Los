package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.*

interface FormDataSource {
    /**
     * Runtime API - Single method for all navigation
     * Calls POST /api/v1/runtime/next-screen
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
        applicationId: String? = null,
        currentScreenId: String? = null,
        flowId: String? = null,
        productCode: String? = null,
        partnerCode: String? = null,
        branchCode: String? = null,
        formData: Map<String, Any>? = null
    ): NextScreenResponseDto
    
    suspend fun getMasterData(dataSource: String): Map<String, String>
    
    /**
     * Master Data API - Get all master data
     * GET /api/v1/master-data
     */
    suspend fun getAllMasterData(): Map<String, List<String>>
    
    fun updateFormData(screenId: String, formData: Map<String, Any>) // For testing: Update dummy JSON
    
    // Legacy APIs removed - Only swagger APIs are used now
    // All navigation must use nextScreen() which calls POST /api/v1/runtime/next-screen
}

