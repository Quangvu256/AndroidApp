package com.example.androidapp.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadImage(userId: String, imageUri: Uri): Result<String>
    suspend fun uploadMedia(userId: String, mediaUri: Uri, mediaType: String): Result<String>
    suspend fun deleteMedia(mediaUrl: String): Result<Unit>
    suspend fun getDownloadUrl(storagePath: String): Result<String>
}
