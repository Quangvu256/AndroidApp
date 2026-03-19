package com.example.androidapp.ui.screens.create

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
 * Represents the possible UI states for the Quiz Preview screen.
 */
sealed class QuizPreviewUiState {
    /** Data is being loaded from the repository. */
    data object Loading : QuizPreviewUiState()

    /**
     * Quiz and its questions have been loaded successfully.
     *
     * @property quiz The quiz metadata.
     * @property questions The ordered list of questions with their choices.
     */
    data class Success(
        val quiz: Quiz,
        val questions: List<Question>
    ) : QuizPreviewUiState()

    /**
     * An error occurred while loading the quiz.
     *
     * @property message A human-readable description of the error.
     */
    data class Error(val message: String) : QuizPreviewUiState()
}

/**
 * ViewModel for the Quiz Preview screen.
 *
 * Loads quiz metadata and the full question list from [QuizRepository] and
 * exposes them as a sealed [QuizPreviewUiState] for the UI to render in read-only mode.
 *
 * @param quizId The ID of the quiz to preview.
 * @param quizRepository Repository used to fetch quiz and question data.
 */
class QuizPreviewViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizPreviewUiState>(QuizPreviewUiState.Loading)

    /** Current UI state for the Quiz Preview screen. */
    val uiState: StateFlow<QuizPreviewUiState> = _uiState.asStateFlow()

    init {
        loadPreview()
    }

    /**
     * Retries loading the quiz and questions after a previous failure.
     */
    fun onRetry() {
        loadPreview()
    }

    private fun loadPreview() {
        viewModelScope.launch {
            _uiState.value = QuizPreviewUiState.Loading

            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.value = QuizPreviewUiState.Error("Không tìm thấy bài kiểm tra")
                return@launch
            }

            // Collect the first emission from the questions Flow and stay updated.
            quizRepository.getQuestionsForQuiz(quizId).collect { questions ->
                _uiState.value = QuizPreviewUiState.Success(
                    quiz = quiz,
                    questions = questions.sortedBy { it.position }
                )
            }
        }
    }
}
