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
     * Calls POST /api/v1/runtime/next-screen
     */
    override suspend fun nextScreen(
        applicationId: String?,
        currentScreenId: String?,
        flowId: String?,
        productCode: String?,
        partnerCode: String?,
        branchCode: String?,
        formData: Map<String, Any>?
    ): NextScreenResponseDto {
        return formApiService.nextScreen(
            NextScreenRequestDto(
                applicationId = applicationId,
                currentScreenId = currentScreenId,
                flowId = flowId,
                productCode = productCode,
                partnerCode = partnerCode?.takeIf { it.isNotBlank() } ?: "SAMASTA", // Default to SAMASTA for testing
                branchCode = branchCode,
                formData = formData
            )
        )
    }
    
    override suspend fun getMasterData(dataSource: String): Map<String, String> {
        // Use getAllMasterData() from swagger API and filter by dataSource on client side
        val allMasterData = formApiService.getAllMasterData()
        val dataList = allMasterData[dataSource] ?: emptyList()
        // Convert list to comma-separated string format (for backward compatibility)
        return mapOf(dataSource to dataList.joinToString(","))
    }
    
    override suspend fun getAllMasterData(): Map<String, List<String>> {
        return formApiService.getAllMasterData()
    }
    
    override fun updateFormData(screenId: String, formData: Map<String, Any>) {
        // Cast to FormApiServiceDummy for testing - only works with dummy implementation
        if (formApiService is com.kaleidofin.originator.data.api.FormApiServiceDummy) {
            formApiService.updateFormData(screenId, formData)
        }
    }
    
    // Legacy APIs removed - Only swagger APIs are used now
    // All navigation must use nextScreen() which calls POST /api/v1/runtime/next-screen
}

