package com.example.androidapp.ui.screens.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Possible UI states for the Quiz Detail screen.
 */
sealed class QuizDetailUiState {
    data object Loading : QuizDetailUiState()
    data class Success(
        val quiz: Quiz,
        val questions: List<Question>
    ) : QuizDetailUiState()
    data class Error(val message: String) : QuizDetailUiState()
}

/**
 * ViewModel for the Quiz Detail screen.
 * Loads quiz metadata and question list from the repository.
 */
class QuizDetailViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizDetailUiState>(QuizDetailUiState.Loading)

    /** Current UI state for the Quiz Detail screen. */
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    init {
        loadQuizDetail()
    }

    /**
     * Reloads the quiz detail data.
     */
    fun onRetry() {
        loadQuizDetail()
    }

    private fun loadQuizDetail() {
        viewModelScope.launch {
            _uiState.value = QuizDetailUiState.Loading
            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.value = QuizDetailUiState.Error("Không tìm thấy bài kiểm tra")
                return@launch
            }
            quizRepository.getQuestionsForQuiz(quizId).collect { questions ->
                _uiState.value = QuizDetailUiState.Success(quiz, questions)
            }
        }
    }
}

