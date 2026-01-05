package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.FormScreenDto

interface FormDataSource {
    suspend fun getFormConfiguration(target: String): FormScreenDto
    suspend fun getMasterData(dataSource: String): Map<String, String> // Returns comma-separated string
    fun updateFormData(screenId: String, formData: Map<String, Any>) // For testing: Update form data in dummy JSON
}

