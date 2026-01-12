package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FormApiService {
    /**
     * Runtime API - Single endpoint for all navigation
     * POST /runtime/next-screen
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
    
    @POST("runtime/next-screen")
    suspend fun nextScreen(@Body request: NextScreenRequestDto): NextScreenResponseDto
    
    @GET("master-data/{dataSource}")
    suspend fun getMasterData(@Path("dataSource") dataSource: String): Map<String, String>
    
    // Legacy APIs - Deprecated: Use Runtime API instead
    // Keep for backward compatibility with existing dummy implementations
    
    @Deprecated("Use Runtime API (POST /runtime/next-screen) instead")
    @POST("flow/start")
    suspend fun startFlow(@Body request: FlowStartRequestDto): FlowResponseDto
    
    @Deprecated("Use Runtime API (POST /runtime/next-screen) instead")
    @POST("flow/next")
    suspend fun navigateNext(@Body request: FlowNextRequestDto): FlowResponseDto
    
    @Deprecated("Use Runtime API (POST /runtime/next-screen) instead")
    @POST("flow/back")
    suspend fun navigateBack(@Body request: FlowBackRequestDto): FlowResponseDto
    
    @Deprecated("Use Runtime API instead")
    @GET("form/{target}")
    suspend fun getFormConfiguration(@Path("target") target: String): FormScreenDto
}
