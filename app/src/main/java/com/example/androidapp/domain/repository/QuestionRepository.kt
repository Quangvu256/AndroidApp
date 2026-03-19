package com.example.androidapp.domain.repository

import com.example.androidapp.domain.model.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    fun getQuestionsForQuiz(quizId: String): Flow<List<Question>>
    suspend fun getQuestionsForQuizOnce(quizId: String): List<Question>
    suspend fun addQuestion(quizId: String, question: Question): Result<String>
    suspend fun updateQuestion(question: Question): Result<Unit>
    suspend fun deleteQuestion(quizId: String, questionId: String): Result<Unit>
    suspend fun reorderQuestions(quizId: String, questionIds: List<String>): Result<Unit>
}
