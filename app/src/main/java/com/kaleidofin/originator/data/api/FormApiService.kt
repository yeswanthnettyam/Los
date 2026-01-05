package com.kaleidofin.originator.data.api

import com.kaleidofin.originator.data.dto.FormScreenDto
import retrofit2.http.GET
import retrofit2.http.Path

interface FormApiService {
    @GET("form/{target}")
    suspend fun getFormConfiguration(@Path("target") target: String): FormScreenDto
    
    @GET("master-data/{dataSource}")
    suspend fun getMasterData(@Path("dataSource") dataSource: String): Map<String, String> // Returns comma-separated string
}
