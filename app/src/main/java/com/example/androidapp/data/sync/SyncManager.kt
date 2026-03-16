package com.example.androidapp.data.sync

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.entity.PendingSyncEntity
import com.example.androidapp.data.local.entity.PendingSyncStatus
import com.example.androidapp.data.local.entity.SyncEntityType
import com.example.androidapp.data.local.entity.SyncOperation
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
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
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
                pendingSyncDao.updateStatus(operation.id, PendingSyncStatus.IN_PROGRESS.name)
                executeOperation(operation)
                pendingSyncDao.updateStatus(operation.id, PendingSyncStatus.COMPLETED.name)
            } catch (e: Exception) {
                hasErrors = true
                pendingSyncDao.incrementRetryCount(
                    operation.id,
                    e.message ?: "Unknown error"
                )
            }
        }

        // Remove successfully completed operations so the table does not grow unbounded.
        pendingSyncDao.deleteCompletedOperations()

        _syncState.value = if (hasErrors) SyncState.ERROR else SyncState.IDLE
    }

    suspend fun enqueueSync(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: String = ""
    ) {
        pendingSyncDao.insertOperation(
            PendingSyncEntity(
                entityType = entityType.name,
                entityId = entityId,
                operation = operation.name,
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
            SyncEntityType.QUIZ.name -> executeQuizSync(operation)
            else -> throw IllegalArgumentException(
                "Unsupported entityType '${operation.entityType}' for operation ${operation.id}"
            )
        }
    }

    private suspend fun executeQuizSync(operation: PendingSyncEntity) {
        when (operation.operation) {
            SyncOperation.CREATE.name, SyncOperation.UPDATE.name -> {
                val quizEntity = quizDao.getQuizById(operation.entityId)
                    ?: throw IllegalStateException(
                        "Quiz '${operation.entityId}' not found locally; cannot sync."
                    )
                val quiz = quizEntity.toDomain()

                // Load questions (with choices) from Room to sync the full quiz graph.
                val questionDtos = questionDao.getQuestionsByQuizIdOnce(operation.entityId)
                    .map { qEntity ->
                        val choices = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
                        qEntity.toDomain(choices.map { it.toDomain() }).toDto()
                    }

                quizRemoteDataSource.saveQuiz(
                    operation.entityId,
                    quiz.toDto(),
                    questionDtos
                )
                quizDao.updateSyncStatus(operation.entityId, "SYNCED")
            }
            SyncOperation.DELETE.name -> {
                quizRemoteDataSource.permanentlyDeleteQuiz(operation.entityId)
            }
        }
    }
}
