package com.example.androidapp.data.remote.firebase

import com.example.androidapp.data.remote.model.ShareCodeDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ShareCodeRemoteDataSource(private val firestore: FirebaseFirestore) {

    suspend fun lookupShareCode(shareCode: String): ShareCodeDto? {
        return firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .get()
            .await()
            .toObject(ShareCodeDto::class.java)
    }

    suspend fun createShareCode(shareCode: String, quizId: String) {
        firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .set(mapOf("quizId" to quizId))
            .await()
    }

    suspend fun deleteShareCode(shareCode: String) {
        firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .delete()
            .await()
    }

    suspend fun shareCodeExists(shareCode: String): Boolean {
        return firestore.collection(FirestoreCollections.SHARE_CODES)
            .document(shareCode)
            .get()
            .await()
            .exists()
    }
}
