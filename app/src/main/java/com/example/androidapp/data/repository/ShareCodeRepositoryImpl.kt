package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.firebase.ShareCodeRemoteDataSource
import com.example.androidapp.domain.repository.ShareCodeRepository

class ShareCodeRepositoryImpl(
    private val remoteDataSource: ShareCodeRemoteDataSource
) : ShareCodeRepository {

    override suspend fun lookupQuizId(shareCode: String): Result<String?> {
        return try {
            val dto = remoteDataSource.lookupShareCode(shareCode)
            Result.success(dto?.quizId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateShareCode(quizId: String): Result<String> {
        return try {
            var code: String
            var attempts = 0
            val maxAttempts = 10
            do {
                code = generateRandomCode()
                val exists = remoteDataSource.shareCodeExists(code)
                attempts++
                if (!exists) break
            } while (attempts < maxAttempts)

            if (attempts >= maxAttempts) {
                return Result.failure(
                    IllegalStateException("Failed to generate unique share code after $maxAttempts attempts")
                )
            }

            remoteDataSource.createShareCode(code, quizId)
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShareCode(shareCode: String): Result<Unit> {
        return try {
            remoteDataSource.deleteShareCode(shareCode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateShareCode(
        quizId: String,
        oldShareCode: String
    ): Result<String> {
        return try {
            // Create the new code first; only delete the old one after the new one is persisted.
            val newCodeResult = generateShareCode(quizId)
            if (newCodeResult.isSuccess) {
                remoteDataSource.deleteShareCode(oldShareCode)
            }
            newCodeResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..SHARE_CODE_LENGTH).map { chars.random() }.joinToString("")
    }

    companion object {
        private const val SHARE_CODE_LENGTH = 6
    }
}
