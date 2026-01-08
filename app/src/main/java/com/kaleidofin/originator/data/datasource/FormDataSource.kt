package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.*

interface FormDataSource {
    // Flow Engine APIs - All return flowId, currentScreenId, and full screenConfig
    suspend fun startFlow(applicationId: String, flowType: String? = null): FlowResponseDto
    suspend fun navigateNext(applicationId: String, currentScreenId: String, formData: Map<String, Any>): FlowResponseDto
    suspend fun navigateBack(applicationId: String, currentScreenId: String): FlowResponseDto
    
    // Legacy API - Deprecated: Use flow/start instead for navigation
    @Deprecated("Use startFlow() instead for navigation")
    suspend fun getFormConfiguration(target: String): FormScreenDto
    
    suspend fun getMasterData(dataSource: String): Map<String, String> // Returns comma-separated string
    fun updateFormData(screenId: String, formData: Map<String, Any>) // For testing: Update form data in dummy JSON
}

