package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.ShareCode

interface ShareCodeRepository {
    suspend fun lookupQuizId(shareCode: String): Result<String?>
    suspend fun generateShareCode(quizId: String): Result<String>
    suspend fun deleteShareCode(shareCode: String): Result<Unit>
    suspend fun regenerateShareCode(quizId: String, oldShareCode: String): Result<String>
}
