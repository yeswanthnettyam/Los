package com.kaleidofin.originator.data.datasource.impl

import com.kaleidofin.originator.data.datasource.AuthDataSource
import com.kaleidofin.originator.data.dto.UserDto
import javax.inject.Inject

class AuthDataSourceImpl @Inject constructor() : AuthDataSource {
    private var currentUser: UserDto? = null

    override suspend fun login(userName: String, password: String): Result<UserDto> {
        // TODO: Replace with actual API call
        // For now, simulate a successful login
        return try {
            val user = UserDto(
                id = "1",
                userName = userName,
                email = "$userName@example.com"
            )
            currentUser = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): UserDto? {
        return currentUser
    }

    override suspend fun saveUser(user: UserDto) {
        currentUser = user
    }

    override suspend fun forgotPassword(userName: String, email: String): Result<Unit> {
        // TODO: Replace with actual API call
        // Simulate API call with delay
        return try {
            kotlinx.coroutines.delay(1000) // Simulate network delay
            // Simulate validation - for demo, accept any valid email format
            if (userName.isBlank() || email.isBlank()) {
                Result.failure(IllegalArgumentException("Invalid Username or Email address"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearUser() {
        currentUser = null
    }
}


