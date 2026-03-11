package com.example.androidapp.domain.model

/**
 * Domain model representing a quiz.
 */
data class Quiz(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String? = null,
    val authorName: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val questionCount: Int = 0,
    val attemptCount: Int = 0,
    val isPublic: Boolean = false,
    val shareCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)