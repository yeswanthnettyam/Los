package com.kaleidofin.originator.presentation.ui.state

import com.kaleidofin.originator.domain.model.User

data class LoginUiState(
    val userName: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val isLoginSuccessful: Boolean = false
) {
    val isFormValid: Boolean
        get() = userName.isNotBlank() && password.isNotBlank()
}


