package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.PoolRemoteDataSource
import com.example.androidapp.data.remote.toDomain
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.PoolRepository

class PoolRepositoryImpl(
    private val remoteDataSource: PoolRemoteDataSource
) : PoolRepository {

    override suspend fun contributeQuestion(poolItem: QuestionPoolItem): Result<Unit> {
        return try {
            remoteDataSource.addPoolItem(poolItem.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPoolQuestionsByTags(
        tags: List<String>
    ): Result<List<QuestionPoolItem>> {
        return try {
            val items = remoteDataSource.getPoolItemsByTags(tags)
                .map { it.toDomain() }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyContributions(userId: String): Result<List<QuestionPoolItem>> {
        return try {
            val items = remoteDataSource.getContributionsByUser(userId)
                .map { it.toDomain() }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeContribution(poolItemId: String): Result<Unit> {
        return try {
            remoteDataSource.deletePoolItem(poolItemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementUsageCount(poolItemId: String): Result<Unit> {
        return try {
            remoteDataSource.incrementUsageCount(poolItemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
