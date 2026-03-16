package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.QuestionPoolItemDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PoolRemoteDataSource(private val firestore: FirebaseFirestore) {

    suspend fun addPoolItem(poolItemDto: QuestionPoolItemDto) {
        val docId = poolItemDto.id.ifBlank { null }
        val ref = if (docId != null) {
            firestore.collection(FirestoreCollections.QUESTION_POOL).document(docId)
        } else {
            firestore.collection(FirestoreCollections.QUESTION_POOL).document()
        }
        ref.set(poolItemDto).await()
    }

    suspend fun getPoolItemsByTags(tags: List<String>): List<QuestionPoolItemDto> {
        if (tags.isEmpty()) return emptyList()
        val queryTags = tags.take(10)
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereArrayContainsAny("tags", queryTags)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
    }

    suspend fun getContributionsByUser(userId: String): List<QuestionPoolItemDto> {
        return firestore.collection(FirestoreCollections.QUESTION_POOL)
            .whereEqualTo("authorId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(QuestionPoolItemDto::class.java) }
    }

    suspend fun deletePoolItem(poolItemId: String) {
        firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)
            .delete()
            .await()
    }

    suspend fun incrementUsageCount(poolItemId: String) {
        firestore.collection(FirestoreCollections.QUESTION_POOL)
            .document(poolItemId)
            .update("usageCount", FieldValue.increment(1))
            .await()
    }
}
