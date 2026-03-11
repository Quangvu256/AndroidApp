package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Edit Quiz screen.
 * Extends [CreateQuizUiState] with the target quiz ID.
 */
data class EditQuizUiState(
    val quizId: String = "",
    val title: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val questions: List<QuestionDraft> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

/** Events that can be dispatched to [EditQuizViewModel]. */
sealed class EditQuizEvent {
    data class TitleChanged(val title: String) : EditQuizEvent()
    data class DescriptionChanged(val description: String) : EditQuizEvent()
    data class IsPublicChanged(val isPublic: Boolean) : EditQuizEvent()
    data class TagsChanged(val tags: String) : EditQuizEvent()
    data object AddQuestion : EditQuizEvent()
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : EditQuizEvent()
    data class RemoveQuestion(val index: Int) : EditQuizEvent()
    data object SaveQuiz : EditQuizEvent()
    data object ClearError : EditQuizEvent()
}

/**
 * ViewModel for the Edit Quiz screen.
 * Pre-populates the form from the repository and saves changes.
 */
class EditQuizViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditQuizUiState(quizId = quizId, isLoading = true))

    /** Current UI state for the Edit Quiz screen. */
    val uiState: StateFlow<EditQuizUiState> = _uiState.asStateFlow()

    init {
        loadExistingQuiz()
    }

    /**
     * Dispatches an [EditQuizEvent] to the ViewModel.
     */
    fun onEvent(event: EditQuizEvent) {
        when (event) {
            is EditQuizEvent.TitleChanged -> _uiState.update { it.copy(title = event.title) }
            is EditQuizEvent.DescriptionChanged -> _uiState.update { it.copy(description = event.description) }
            is EditQuizEvent.IsPublicChanged -> _uiState.update { it.copy(isPublic = event.isPublic) }
            is EditQuizEvent.TagsChanged -> _uiState.update { it.copy(tags = event.tags) }
            is EditQuizEvent.AddQuestion -> _uiState.update {
                it.copy(questions = it.questions + QuestionDraft())
            }
            is EditQuizEvent.UpdateQuestion -> _uiState.update { state ->
                state.copy(questions = state.questions.toMutableList().apply {
                    this[event.index] = event.draft
                })
            }
            is EditQuizEvent.RemoveQuestion -> _uiState.update { state ->
                if (state.questions.size > 1) {
                    state.copy(questions = state.questions.toMutableList().apply { removeAt(event.index) })
                } else state
            }
            is EditQuizEvent.SaveQuiz -> onSaveQuiz()
            is EditQuizEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadExistingQuiz() {
        viewModelScope.launch {
            val quiz = quizRepository.getQuizById(quizId)
            if (quiz == null) {
                _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy bài kiểm tra") }
                return@launch
            }
            val questions = quizRepository.getQuestionsForQuizOnce(quizId)
            val drafts = questions.map { question ->
                QuestionDraft(
                    id = question.id,
                    content = question.content,
                    choices = question.choices.map { c -> ChoiceDraft(id = c.id, content = c.content) },
                    correctIndices = question.choices.mapIndexedNotNull { idx, c -> if (c.isCorrect) idx else null }.toSet(),
                    isMultiSelect = question.isMultiSelect,
                    explanation = question.explanation ?: ""
                )
            }.ifEmpty { listOf(QuestionDraft()) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = quiz.title,
                    description = quiz.description ?: "",
                    isPublic = quiz.isPublic,
                    tags = quiz.tags.joinToString(", "),
                    questions = drafts
                )
            }
        }
    }

    private fun onSaveQuiz() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài kiểm tra") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.getCurrentUser()
            val tags = state.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val quiz = Quiz(
                id = quizId,
                ownerId = user?.id ?: "",
                title = state.title,
                description = state.description.takeIf { it.isNotBlank() },
                authorName = user?.displayName ?: "",
                tags = tags,
                isPublic = state.isPublic,
                questionCount = state.questions.size,
                updatedAt = System.currentTimeMillis()
            )
            val questions = state.questions.mapIndexed { idx, draft ->
                Question(
                    id = draft.id,
                    quizId = quizId,
                    content = draft.content,
                    choices = draft.choices.mapIndexed { cIdx, choice ->
                        Choice(
                            id = choice.id,
                            content = choice.content,
                            isCorrect = cIdx in draft.correctIndices,
                            position = cIdx
                        )
                    },
                    isMultiSelect = draft.isMultiSelect,
                    explanation = draft.explanation.takeIf { it.isNotBlank() },
                    position = idx
                )
            }
            val result = quizRepository.updateQuiz(quiz, questions)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaved = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Không thể lưu bài kiểm tra") } }
            )
        }
    }
}

