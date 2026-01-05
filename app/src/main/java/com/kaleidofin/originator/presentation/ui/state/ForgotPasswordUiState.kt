package com.kaleidofin.originator.presentation.ui.state

data class ForgotPasswordUiState(
    val userName: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    val isFormValid: Boolean
        get() = userName.isNotBlank() && email.isNotBlank() && isValidEmail(email)
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return emailRegex.toRegex().matches(email)
    }
}

