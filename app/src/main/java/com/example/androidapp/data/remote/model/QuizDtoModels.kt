package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChoiceDto(
    val id: String = "",
    val content: String = "",
    val isCorrect: Boolean = false,
    val position: Int = 0
)

data class QuestionDto(
    val id: String = "",
    val content: String = "",
    val choices: List<ChoiceDto> = emptyList(),
    val isMultiSelect: Boolean = false,
    val explanation: String? = null,
    val mediaUrl: String? = null,
    val points: Int = 1,
    val position: Int = 0
)

data class QuizDto(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String? = null,
    val authorName: String = "",
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val questionCount: Int = 0,
    val attemptCount: Int = 0,
    val isPublic: Boolean = false,
    val shareCode: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)