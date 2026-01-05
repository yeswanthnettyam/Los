package com.kaleidofin.originator.data.datasource.impl

import com.kaleidofin.originator.data.api.FormApiService
import com.kaleidofin.originator.data.api.FormApiServiceDummy
import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.dto.FormScreenDto
import javax.inject.Inject

class FormDataSourceImpl @Inject constructor(
    private val formApiService: FormApiService
) : FormDataSource {
    override suspend fun getFormConfiguration(target: String): FormScreenDto {
        return formApiService.getFormConfiguration(target)
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
}

