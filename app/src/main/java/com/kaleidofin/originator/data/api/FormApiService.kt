package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FormApiService {
    // Flow Engine APIs - All return flowId, currentScreenId, and full screenConfig
    @POST("flow/start")
    suspend fun startFlow(@Body request: FlowStartRequestDto): FlowResponseDto
    
    @POST("flow/next")
    suspend fun navigateNext(@Body request: FlowNextRequestDto): FlowResponseDto
    
    @POST("flow/back")
    suspend fun navigateBack(@Body request: FlowBackRequestDto): FlowResponseDto
    
    // Legacy API - Deprecated: Use flow/start instead for navigation
    // Keep for backward compatibility or non-navigation use cases
    @GET("form/{target}")
    @Deprecated("Use flow/start API instead for navigation. This API should only be used for non-navigation config fetching.")
    suspend fun getFormConfiguration(@Path("target") target: String): FormScreenDto
    
    @GET("master-data/{dataSource}")
    suspend fun getMasterData(@Path("dataSource") dataSource: String): Map<String, String> // Returns comma-separated string
}
