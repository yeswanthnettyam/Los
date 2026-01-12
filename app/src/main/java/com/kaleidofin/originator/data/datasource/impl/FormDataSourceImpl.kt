package com.kaleidofin.originator.data.datasource.impl

import com.kaleidofin.originator.data.api.FormApiService
import com.kaleidofin.originator.data.api.FormApiServiceDummy
import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.dto.*
import javax.inject.Inject

class FormDataSourceImpl @Inject constructor(
    private val formApiService: FormApiService
) : FormDataSource {
    /**
     * Dashboard API - Get available flows
     * GET /api/v1/dashboard/flows
     */
    override suspend fun getDashboardFlows(): DashboardResponseDto {
        return formApiService.getDashboardFlows()
    }
    
    /**
     * Runtime API - Single method for all navigation
     * Calls POST /runtime/next-screen
     */
    override suspend fun nextScreen(
        applicationId: String,
        currentScreenId: String?,
        formData: Map<String, Any>?
    ): NextScreenResponseDto {
        return formApiService.nextScreen(
            NextScreenRequestDto(
                applicationId = applicationId,
                currentScreenId = currentScreenId,
                formData = formData
            )
        )
    }
    
    override suspend fun getMasterData(dataSource: String): Map<String, String> {
        return formApiService.getMasterData(dataSource)
    }
    
    override fun updateFormData(screenId: String, formData: Map<String, Any>) {
        // Cast to FormApiServiceDummy for testing - only works with dummy implementation
        if (formApiService is com.kaleidofin.originator.data.api.FormApiServiceDummy) {
            formApiService.updateFormData(screenId, formData)
        }
    }
    
    // Legacy APIs - Deprecated: Use Runtime API instead
    @Deprecated("Use nextScreen() instead")
    override suspend fun startFlow(applicationId: String, flowType: String?): FlowResponseDto {
        return formApiService.startFlow(
            FlowStartRequestDto(
                applicationId = applicationId,
                flowType = flowType
            )
        )
    }
    
    @Deprecated("Use nextScreen() instead")
    override suspend fun navigateNext(applicationId: String, currentScreenId: String, formData: Map<String, Any>): FlowResponseDto {
        return formApiService.navigateNext(
            FlowNextRequestDto(
                applicationId = applicationId,
                currentScreenId = currentScreenId,
                formData = formData
            )
        )
    }
    
    @Deprecated("Use nextScreen() instead")
    override suspend fun navigateBack(applicationId: String, currentScreenId: String): FlowResponseDto {
        return formApiService.navigateBack(
            FlowBackRequestDto(
                applicationId = applicationId,
                currentScreenId = currentScreenId
            )
        )
    }
    
    @Deprecated("Use nextScreen() instead")
    override suspend fun getFormConfiguration(target: String): FormScreenDto {
        return formApiService.getFormConfiguration(target)
    }
}

