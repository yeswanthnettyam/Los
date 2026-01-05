package com.kaleidofin.originator.data.repository

import com.kaleidofin.originator.data.datasource.AuthDataSource
import com.kaleidofin.originator.domain.model.User
import com.kaleidofin.originator.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {
    override suspend fun login(userName: String, password: String): Result<User> {
        return authDataSource.login(userName, password)
            .map { it.toDomain() }
            .onSuccess { user ->
                authDataSource.saveUser(
                    com.kaleidofin.originator.data.dto.UserDto(
                        id = user.id,
                        userName = user.userName,
                        email = user.email
                    )
                )
            }
    }

    override suspend fun logout() {
        authDataSource.clearUser()
    }

    override suspend fun forgotPassword(userName: String, email: String): Result<Unit> {
        return authDataSource.forgotPassword(userName, email)
    }

    override suspend fun getCurrentUser(): User? {
        return authDataSource.getCurrentUser()?.toDomain()
    }
}


