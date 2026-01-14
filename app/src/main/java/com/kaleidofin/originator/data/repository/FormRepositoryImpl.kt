package com.kaleidofin.originator.data.repository

import com.kaleidofin.originator.data.datasource.FormDataSource
import com.kaleidofin.originator.data.mapper.toDomain
import com.kaleidofin.originator.domain.model.FormScreen
import com.kaleidofin.originator.domain.repository.FormRepository
import javax.inject.Inject

class FormRepositoryImpl @Inject constructor(
    private val formDataSource: FormDataSource
) : FormRepository {
    override suspend fun getMasterData(dataSource: String): List<String> {
        val result = formDataSource.getMasterData(dataSource)
        val commaSeparatedString = result[dataSource] ?: ""
        // Parse comma-separated string to list
        return if (commaSeparatedString.isBlank()) {
            emptyList()
        } else {
            commaSeparatedString.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }
}

