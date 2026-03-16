package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.QuestionPoolItem

interface PoolRepository {
    suspend fun contributeQuestion(poolItem: QuestionPoolItem): Result<Unit>
    suspend fun getPoolQuestionsByTags(tags: List<String>): Result<List<QuestionPoolItem>>
    suspend fun getMyContributions(userId: String): Result<List<QuestionPoolItem>>
    suspend fun revokeContribution(poolItemId: String): Result<Unit>
    suspend fun incrementUsageCount(poolItemId: String): Result<Unit>
}
