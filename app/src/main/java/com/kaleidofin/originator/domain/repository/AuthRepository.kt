package com.kaleidofin.originator.domain.repository

import com.kaleidofin.originator.domain.model.User

interface AuthRepository {
    suspend fun login(userName: String, password: String): Result<User>
    suspend fun forgotPassword(userName: String, email: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
}


