package com.kaleidofin.originator.data.datasource.impl

import com.kaleidofin.originator.data.api.HomeApiService
import com.kaleidofin.originator.data.api.HomeApiServiceDummy
import com.kaleidofin.originator.data.datasource.HomeDataSource
import com.kaleidofin.originator.data.dto.HomeScreenDto
import javax.inject.Inject

class HomeDataSourceImpl @Inject constructor() : HomeDataSource {
    private val apiService: HomeApiService = HomeApiServiceDummy()
    
    override suspend fun getHomeDashboard(): HomeScreenDto {
        return apiService.getHomeDashboard()
    }
}


