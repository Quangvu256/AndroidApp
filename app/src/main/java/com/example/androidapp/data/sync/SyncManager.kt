package com.example.androidapp.data.sync

import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.entity.PendingSyncEntity
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncState {
    IDLE,
    SYNCING,
    PENDING,
    ERROR
}

class SyncManager(
    private val pendingSyncDao: PendingSyncDao,
    private val quizDao: QuizDao,
    private val quizRemoteDataSource: QuizRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    processPendingOperations()
                }
            }
        }
    }

    suspend fun processPendingOperations() {
        val pending = pendingSyncDao.getPendingOperations()
        if (pending.isEmpty()) {
            _syncState.value = SyncState.IDLE
            return
        }

        _syncState.value = SyncState.SYNCING

        var hasErrors = false
        for (operation in pending) {
            try {
                pendingSyncDao.updateStatus(operation.id, "IN_PROGRESS")
                executeOperation(operation)
                pendingSyncDao.updateStatus(operation.id, "COMPLETED")
            } catch (e: Exception) {
                hasErrors = true
                pendingSyncDao.incrementRetryCount(
                    operation.id,
                    e.message ?: "Unknown error"
                )
            }
        }

        _syncState.value = if (hasErrors) SyncState.ERROR else SyncState.IDLE
    }

    suspend fun enqueueSync(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String = ""
    ) {
        pendingSyncDao.insertOperation(
            PendingSyncEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
        _syncState.value = SyncState.PENDING

        if (networkMonitor.isOnline.value) {
            scope.launch { processPendingOperations() }
        }
    }

    suspend fun getPendingCount(): Int {
        return pendingSyncDao.getPendingCount()
    }

    suspend fun retryFailedOperations() {
        pendingSyncDao.resetFailedToPending()
        if (networkMonitor.isOnline.value) {
            scope.launch { processPendingOperations() }
        }
    }

    private suspend fun executeOperation(operation: PendingSyncEntity) {
        when (operation.entityType) {
            "QUIZ" -> executeQuizSync(operation)
        }
    }

    private suspend fun executeQuizSync(operation: PendingSyncEntity) {
        when (operation.operation) {
            "CREATE", "UPDATE" -> {
                val quizEntity = quizDao.getQuizById(operation.entityId) ?: return
                val quiz = quizEntity.toDomain()
                quizRemoteDataSource.saveQuiz(
                    operation.entityId,
                    quiz.toDto(),
                    emptyList()
                )
                quizDao.updateSyncStatus(operation.entityId, "SYNCED")
            }
            "DELETE" -> {
                quizRemoteDataSource.permanentlyDeleteQuiz(operation.entityId)
            }
        }
    }
}
