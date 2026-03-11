package com.example.androidapp.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.ScoreUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible UI states for the Quiz Result screen.
 */
sealed class QuizResultUiState {
    data object Loading : QuizResultUiState()
    data class Success(
        val quiz: Quiz,
        val attempt: Attempt,
        val percentage: Int,
        val starRating: Int
    ) : QuizResultUiState()
    data class Error(val message: String) : QuizResultUiState()
}

/**
 * ViewModel for the Quiz Result screen.
 * Loads the attempt result and quiz metadata.
 */
class QuizResultViewModel(
    private val quizId: String,
    private val attemptId: String,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizResultUiState>(QuizResultUiState.Loading)

    /** Current UI state for the Quiz Result screen. */
    val uiState: StateFlow<QuizResultUiState> = _uiState.asStateFlow()

    init {
        loadResult()
    }

    /**
     * Retries loading the result data.
     */
    fun onRetry() {
        loadResult()
    }

    private fun loadResult() {
        viewModelScope.launch {
            _uiState.value = QuizResultUiState.Loading
            val quiz = quizRepository.getQuizById(quizId)
            val attempt = attemptRepository.getAttemptById(attemptId)
            if (quiz == null || attempt == null) {
                _uiState.value = QuizResultUiState.Error("Không tìm thấy kết quả")
                return@launch
            }
            val percentage = ScoreUtil.calculatePercentage(attempt.score, attempt.totalQuestions)
            val starRating = ScoreUtil.calculateStarRating(percentage)
            _uiState.value = QuizResultUiState.Success(quiz, attempt, percentage, starRating)
        }
    }
}

