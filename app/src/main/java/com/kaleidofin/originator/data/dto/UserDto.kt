package com.kaleidofin.originator.data.dto

import com.kaleidofin.originator.domain.model.User

data class UserDto(
    val id: String,
    val userName: String,
    val email: String? = null
) {
    fun toDomain(): User {
        return User(
            id = id,
            userName = userName,
            email = email
        )
    }
}


