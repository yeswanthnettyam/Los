package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.HomeScreenDto

interface HomeDataSource {
    suspend fun getHomeDashboard(): HomeScreenDto
}


