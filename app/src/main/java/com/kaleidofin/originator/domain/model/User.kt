package com.kaleidofin.originator.domain.model

data class User(
    val id: String,
    val userName: String,
    val email: String? = null
)


