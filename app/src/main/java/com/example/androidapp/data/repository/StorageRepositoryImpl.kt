package com.example.androidapp.data.repository

import android.net.Uri
import com.example.androidapp.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepositoryImpl(
    private val storage: FirebaseStorage
) : StorageRepository {

    override suspend fun uploadImage(userId: String, imageUri: Uri): Result<String> {
        return uploadMedia(userId, imageUri, "image/jpeg")
    }

    override suspend fun uploadMedia(
        userId: String,
        mediaUri: Uri,
        mediaType: String
    ): Result<String> {
        return try {
            val extension = when {
                mediaType.startsWith("image/png") -> "png"
                mediaType.startsWith("image/") -> "jpg"
                mediaType.startsWith("video/") -> "mp4"
                else -> "bin"
            }
            val filename = "${UUID.randomUUID()}.$extension"
            val storageRef = storage.reference.child("users/$userId/media/$filename")

            storageRef.putFile(mediaUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMedia(mediaUrl: String): Result<Unit> {
        return try {
            val ref = storage.getReferenceFromUrl(mediaUrl)
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownloadUrl(storagePath: String): Result<String> {
        return try {
            val url = storage.reference.child(storagePath).downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
