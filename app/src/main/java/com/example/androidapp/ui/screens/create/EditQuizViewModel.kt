package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.QuestionPoolItem
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.util.QuizValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Edit Quiz screen.
 *
 * ### Draft vs. Publish semantics
 * - A **draft** quiz is never public and never shared to the community pool.
 *   Both [isPublic] and [shareToPool] are forced to `false` while [isDraft] is `true`.
 * - Toggling [isPublic] or [shareToPool] to `true` implicitly sets [isDraft] to `false`
 *   (the user is signalling intent to publish), but does **not** publish on its own —
 *   the user must still press "Xuất bản".
 * - Pressing "Lưu nháp" always forces [isPublic] and [shareToPool] back to `false`
 *   before persisting, regardless of what the toggles show.
 *
 * @property quizId The ID of the quiz being edited.
 * @property title The quiz title.
 * @property description The quiz description.
 * @property thumbnailUrl Optional URL for the quiz cover image.
 * @property isPublic Whether the quiz will be publicly discoverable after publishing.
 *   Always `false` for drafts.
 * @property tags Comma-separated list of tags as raw input text.
 * @property questions The ordered list of question drafts.
 * @property isLoading Whether a save/publish operation is in progress or the quiz is being loaded.
 * @property isSaved Whether the quiz has been successfully saved (triggers navigation).
 * @property isDraft Whether the current version is saved only as a draft (not published).
 * @property isPublished Whether the quiz has been successfully published.
 * @property lastSavedAt Epoch millis of the last draft save, or null if never saved in this session.
 * @property shareToPool Whether to contribute each question to the community pool on publish.
 *   Always `false` for drafts.
 * @property error Current error message to display, or null when there is no error.
 */
data class EditQuizUiState(
    val quizId: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val questions: List<QuestionDraft> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDraft: Boolean = true,
    val isPublished: Boolean = false,
    val lastSavedAt: Long? = null,
    val shareToPool: Boolean = false,
    val error: String? = null
)

/**
 * Events that can be dispatched to [EditQuizViewModel].
 */
sealed class EditQuizEvent {
    /** Updates the quiz title. */
    data class TitleChanged(val title: String) : EditQuizEvent()

    /** Updates the quiz description. */
    data class DescriptionChanged(val description: String) : EditQuizEvent()

    /** Updates the quiz cover image URL. */
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : EditQuizEvent()

    /**
     * Toggles the public visibility of the quiz.
     *
     * Enabling this implicitly exits draft mode — [EditQuizUiState.isDraft] becomes `false`
     * so the user knows the next save will publish. Disabling returns to draft mode when
     * [EditQuizUiState.shareToPool] is also off.
     */
    data class IsPublicChanged(val isPublic: Boolean) : EditQuizEvent()

    /** Updates the raw comma-separated tags string. */
    data class TagsChanged(val tags: String) : EditQuizEvent()

    /** Appends a blank question to the end of the question list. */
    data object AddQuestion : EditQuizEvent()

    /** Replaces the question at [index] with [draft]. */
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : EditQuizEvent()

    /** Removes the question at [index] if more than one question exists. */
    data class RemoveQuestion(val index: Int) : EditQuizEvent()

    /** Moves the question at [index] one position up in the list. */
    data class MoveQuestionUp(val index: Int) : EditQuizEvent()

    /** Moves the question at [index] one position down in the list. */
    data class MoveQuestionDown(val index: Int) : EditQuizEvent()

    /**
     * Saves the current form as a private draft.
     *
     * Regardless of what [EditQuizUiState.isPublic] and [EditQuizUiState.shareToPool]
     * show in the UI, the persisted record will have both forced to `false`. The UI toggles
     * are also reset to `false` after a successful draft save so the displayed state stays
     * consistent with what was actually stored.
     */
    data object SaveDraft : EditQuizEvent()

    /**
     * Validates the quiz and saves it as a published quiz.
     * Sets [EditQuizUiState.isPublished] to true and triggers [EditQuizUiState.isSaved].
     */
    data object PublishQuiz : EditQuizEvent()

    /** Legacy save alias — behaves identically to [PublishQuiz]. */
    data object SaveQuiz : EditQuizEvent()

    /**
     * Toggles whether each question will be contributed to the community pool on publish.
     *
     * Enabling this implicitly exits draft mode — [EditQuizUiState.isDraft] becomes `false`.
     * Pool contribution only happens when the quiz is actually published, never on draft saves.
     */
    data class ShareToPoolChanged(val shareToPool: Boolean) : EditQuizEvent()

    /** Clears the current error message from the UI state. */
    data object ClearError : EditQuizEvent()
}

/**
 * ViewModel for the Edit Quiz screen.
 *
 * Pre-populates the form from the repository and saves changes back.
 * Enforces the invariant that **draft quizzes are always private and never shared to the
 * community pool**:
 *
 * - [EditQuizEvent.SaveDraft] forces `isPublic = false` and `shareToPool = false` on
 *   the persisted record, then resets both toggles in the UI state.
 * - [EditQuizEvent.IsPublicChanged] and [EditQuizEvent.ShareToPoolChanged] set
 *   [EditQuizUiState.isDraft] to `false` when enabled (signalling publish intent) and
 *   back to `true` when both are disabled (returning to draft mode).
 * - [EditQuizEvent.PublishQuiz] always sets `isPublic = true` on the persisted record.
 *
 * @param quizId The ID of the quiz to load and edit.
 * @param quizRepository Repository for persisting quizzes and questions.
 * @param authRepository Repository for retrieving the currently authenticated user.
 * @param poolRepository Repository for contributing questions to the community pool.
 */
class EditQuizViewModel(
    private val quizId: String,
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    private val poolRepository: PoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditQuizUiState(quizId = quizId, isLoading = true))

    /** Current UI state for the Edit Quiz screen. */
    val uiState: StateFlow<EditQuizUiState> = _uiState.asStateFlow()

    init {
        loadExistingQuiz()
    }

    /**
     * Dispatches an [EditQuizEvent] to update state or trigger a side effect.
     */
    fun onEvent(event: EditQuizEvent) {
        when (event) {
            is EditQuizEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is EditQuizEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is EditQuizEvent.ThumbnailUrlChanged ->
                _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }

            is EditQuizEvent.IsPublicChanged -> _uiState.update { state ->
                // Enabling public visibility exits draft mode.
                // Disabling returns to draft mode only when shareToPool is also off.
                val newIsDraft = if (event.isPublic) false else !state.shareToPool
                state.copy(isPublic = event.isPublic, isDraft = newIsDraft)
            }

            is EditQuizEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is EditQuizEvent.AddQuestion ->
                _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }

            is EditQuizEvent.UpdateQuestion ->
                _uiState.update { state ->
                    state.copy(questions = state.questions.toMutableList().apply {
                        this[event.index] = event.draft
                    })
                }

            is EditQuizEvent.RemoveQuestion ->
                _uiState.update { state ->
                    if (state.questions.size > 1) {
                        state.copy(
                            questions = state.questions.toMutableList().apply {
                                removeAt(event.index)
                            }
                        )
                    } else state
                }

            is EditQuizEvent.MoveQuestionUp ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx <= 0 || idx >= state.questions.size) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
                }

            is EditQuizEvent.MoveQuestionDown ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx < 0 || idx >= state.questions.size - 1) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
                }

            is EditQuizEvent.SaveDraft ->
                onSaveQuiz(publishAfterSave = false)

            is EditQuizEvent.PublishQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is EditQuizEvent.SaveQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is EditQuizEvent.ShareToPoolChanged -> _uiState.update { state ->
                // Enabling share-to-pool exits draft mode.
                // Disabling returns to draft mode only when isPublic is also off.
                val newIsDraft = if (event.shareToPool) false else !state.isPublic
                state.copy(shareToPool = event.shareToPool, isDraft = newIsDraft)
            }

            is EditQuizEvent.ClearError ->
                _uiState.update { it.copy(error = null) }
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
                    choices = question.choices
                        .sortedBy { it.position }
                        .map { c -> ChoiceDraft(id = c.id, content = c.content) },
                    correctIndices = question.choices
                        .mapIndexedNotNull { idx, c -> if (c.isCorrect) idx else null }
                        .toSet(),
                    isMultiSelect = question.isMultiSelect,
                    explanation = question.explanation ?: "",
                    mediaUrl = question.mediaUrl ?: "",
                    points = question.points
                )
            }.ifEmpty { listOf(QuestionDraft()) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = quiz.title,
                    description = quiz.description ?: "",
                    thumbnailUrl = quiz.thumbnailUrl ?: "",
                    isPublic = quiz.isPublic,
                    // A quiz that is not public is treated as a draft on load.
                    isDraft = !quiz.isPublic,
                    tags = quiz.tags.joinToString(", "),
                    questions = drafts
                )
            }
        }
    }

    /**
     * Validates and persists the quiz.
     *
     * ### Draft path (`publishAfterSave = false`)
     * - `isPublic` is forced to `false` — drafts are always private.
     * - `shareToPool` is skipped entirely — pool contribution never runs for drafts.
     * - Both [EditQuizUiState.isPublic] and [EditQuizUiState.shareToPool] are reset to
     *   `false` in the UI state after a successful save so the form reflects reality.
     * - [EditQuizUiState.isDraft] is set to `true` and [EditQuizUiState.lastSavedAt] updated.
     *
     * ### Publish path (`publishAfterSave = true`)
     * - `isPublic` is forced to `true`.
     * - `shareToPool` contribution runs if the toggle is on.
     * - [EditQuizUiState.isSaved] is set to `true` to trigger back navigation.
     *
     * @param publishAfterSave `true` to publish, `false` to save as draft.
     */
    private fun onSaveQuiz(publishAfterSave: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.title.isBlank()) {
                _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề bài kiểm tra") }
                return@launch
            }

            val validationResult = QuizValidator.validate(
                questions = state.questions,
                getChoices = { draft ->
                    draft.choices.mapIndexed { idx, choice ->
                        Pair(choice, idx in draft.correctIndices)
                    }
                },
                isCorrect = { (_, correct) -> correct }
            )
            if (!validationResult.isValid) {
                _uiState.update { it.copy(error = validationResult.errorMessage) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val user = authRepository.getCurrentUser()
            val tags = state.tags
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Drafts are ALWAYS private. Publish forces public.
            val effectiveIsPublic = publishAfterSave

            val quiz = Quiz(
                id = quizId,
                ownerId = user?.id ?: "",
                title = state.title,
                description = state.description.takeIf { it.isNotBlank() },
                thumbnailUrl = state.thumbnailUrl.takeIf { it.isNotBlank() },
                authorName = user?.displayName ?: "",
                tags = tags,
                isPublic = effectiveIsPublic,
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
                    mediaUrl = draft.mediaUrl.takeIf { it.isNotBlank() },
                    points = draft.points,
                    position = idx
                )
            }

            val result = quizRepository.updateQuiz(quiz, questions)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    if (publishAfterSave) {
                        // Pool contribution only happens on publish, never on draft saves.
                        if (state.shareToPool) {
                            questions.forEach { question ->
                                poolRepository.contributeQuestion(
                                    QuestionPoolItem(
                                        id = question.id,
                                        question = question,
                                        authorId = user?.id ?: "",
                                        tags = tags,
                                        usageCount = 0
                                    )
                                )
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isSaved = true,
                                isPublished = true,
                                isDraft = false,
                                isPublic = true
                            )
                        }
                    } else {
                        // Reset both publish-only toggles to false so the UI reflects
                        // what was actually stored (a private, non-pooled draft).
                        _uiState.update {
                            it.copy(
                                isDraft = true,
                                isPublished = false,
                                isPublic = false,
                                shareToPool = false,
                                lastSavedAt = System.currentTimeMillis()
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Không thể lưu bài kiểm tra")
                    }
                }
            )
        }
    }
}
