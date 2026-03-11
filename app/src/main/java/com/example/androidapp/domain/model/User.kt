package com.example.androidapp.domain.model

/**
 * Domain model representing a user.
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val username: String = "",
    val photoUrl: String? = null
)