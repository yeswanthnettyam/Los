package com.kaleidofin.originator.data.datasource

import com.kaleidofin.originator.data.dto.UserDto

interface AuthDataSource {
    suspend fun login(userName: String, password: String): Result<UserDto>
    suspend fun forgotPassword(userName: String, email: String): Result<Unit>
    suspend fun getCurrentUser(): UserDto?
    suspend fun saveUser(user: UserDto)
    suspend fun clearUser()
}


