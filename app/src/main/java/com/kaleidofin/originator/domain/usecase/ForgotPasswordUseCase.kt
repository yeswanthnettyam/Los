package com.kaleidofin.originator.domain.usecase

import com.kaleidofin.originator.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userName: String, email: String): Result<Unit> {
        if (userName.isBlank() || email.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid Username or Email address"))
        }
        return authRepository.forgotPassword(userName, email)
    }
}

