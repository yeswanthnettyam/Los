package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FormApiService {
    /**
     * Runtime API - Single endpoint for all navigation
     * POST /api/v1/runtime/next-screen
     * 
     * Usage:
     * 1. First load: currentScreenId = null, formData = null
     * 2. Next screen: currentScreenId = current, formData = form values
     * 
     * Backend responsibilities:
     * - Evaluate flow conditions
     * - Resolve screen config (BRANCH → PARTNER → PRODUCT → BASE)
     * - Manage flow snapshot
     * - Return nextScreenId + full screenConfig
     */
    /**
     * Dashboard API - Get available flows with UI metadata
     * GET /api/v1/dashboard/flows
     * 
     * Returns list of flows that can be started from dashboard
     * Each flow includes UI customization (colors, icons) from FlowConfig.dashboardMeta
     */
    @GET("api/v1/dashboard/flows")
    suspend fun getDashboardFlows(): DashboardResponseDto
    
    @POST("api/v1/runtime/next-screen")
    suspend fun nextScreen(@Body request: NextScreenRequestDto): NextScreenResponseDto
    
    /**
     * Master Data API - Get all master data
     * GET /api/v1/master-data
     * 
     * Returns all master data entries
     */
    @GET("api/v1/master-data")
    suspend fun getAllMasterData(): Map<String, List<String>>
}
