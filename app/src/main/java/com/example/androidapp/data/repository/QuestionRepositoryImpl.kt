package com.example.androidapp.data.repository

import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.remote.toDto
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.repository.QuestionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class QuestionRepositoryImpl(
    private val questionDao: QuestionDao,
    private val choiceDao: ChoiceDao,
    private val remoteDataSource: QuestionRemoteDataSource
) : QuestionRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun getQuestionsForQuiz(quizId: String): Flow<List<Question>> {
        return questionDao.getQuestionsByQuizId(quizId).map { questionEntities ->
            questionEntities.map { qEntity ->
                val choiceEntities = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
                qEntity.toDomain(choiceEntities.map { it.toDomain() })
            }
        }
    }

    override suspend fun getQuestionsForQuizOnce(quizId: String): List<Question> {
        return questionDao.getQuestionsByQuizIdOnce(quizId).map { qEntity ->
            val choiceEntities = choiceDao.getChoicesByQuestionIdOnce(qEntity.id)
            qEntity.toDomain(choiceEntities.map { it.toDomain() })
        }
    }

    override suspend fun addQuestion(quizId: String, question: Question): Result<String> {
        return try {
            val questionId = question.id.ifBlank { UUID.randomUUID().toString() }
            val finalQuestion = question.copy(id = questionId, quizId = quizId)

            questionDao.insertQuestion(finalQuestion.toEntity())
            finalQuestion.choices.forEachIndexed { idx, choice ->
                val choiceId = choice.id.ifBlank { UUID.randomUUID().toString() }
                choiceDao.insertChoice(
                    choice.copy(id = choiceId, position = idx).toEntity(questionId)
                )
            }

            ioScope.launch {
                try {
                    val choiceDtos = finalQuestion.choices.mapIndexed { idx, c ->
                        val cId = c.id.ifBlank { UUID.randomUUID().toString() }
                        c.copy(id = cId, position = idx).toDto()
                    }
                    remoteDataSource.saveQuestion(quizId, finalQuestion.toDto(), choiceDtos)
                } catch (_: Exception) { }
            }

            Result.success(questionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuestion(question: Question): Result<Unit> {
        return try {
            questionDao.insertQuestion(question.toEntity())
            choiceDao.deleteChoicesByQuestionId(question.id)
            question.choices.forEachIndexed { idx, choice ->
                val choiceId = choice.id.ifBlank { UUID.randomUUID().toString() }
                choiceDao.insertChoice(
                    choice.copy(id = choiceId, position = idx).toEntity(question.id)
                )
            }

            ioScope.launch {
                try {
                    val choiceDtos = question.choices.mapIndexed { idx, c ->
                        val cId = c.id.ifBlank { UUID.randomUUID().toString() }
                        c.copy(id = cId, position = idx).toDto()
                    }
                    remoteDataSource.saveQuestion(question.quizId, question.toDto(), choiceDtos)
                } catch (_: Exception) { }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteQuestion(quizId: String, questionId: String): Result<Unit> {
        return try {
            val entity = questionDao.getQuestionsByQuizIdOnce(quizId)
                .find { it.id == questionId }
            if (entity != null) {
                questionDao.deleteQuestion(entity)
            }

            ioScope.launch {
                try {
                    remoteDataSource.deleteQuestion(quizId, questionId)
                } catch (_: Exception) { }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderQuestions(
        quizId: String,
        questionIds: List<String>
    ): Result<Unit> {
        return try {
            questionIds.forEachIndexed { index, questionId ->
                questionDao.updatePosition(questionId, index)
            }

            ioScope.launch {
                try {
                    val positionMap = questionIds.mapIndexed { index, id -> id to index }.toMap()
                    remoteDataSource.updateQuestionPositions(quizId, positionMap)
                } catch (_: Exception) { }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
