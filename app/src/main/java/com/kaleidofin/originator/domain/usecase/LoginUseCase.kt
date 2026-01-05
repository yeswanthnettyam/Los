package com.kaleidofin.originator.domain.usecase

import com.kaleidofin.originator.domain.model.User
import com.kaleidofin.originator.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userName: String, password: String): Result<User> {
        if (userName.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password cannot be empty"))
        }
        return authRepository.login(userName, password)
    }
}


