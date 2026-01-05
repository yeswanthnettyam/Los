package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.HomeScreenDto
import retrofit2.http.GET

interface HomeApiService {
    @GET("home/dashboard")
    suspend fun getHomeDashboard(): HomeScreenDto
}

