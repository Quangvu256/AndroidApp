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
import java.util.UUID

/**
 * Draft model for creating/editing a question.
 * Kept in the ViewModel layer, not in the domain.
 *
 * @property id Unique identifier for this draft, used to track identity across recompositions.
 * @property content The question text.
 * @property choices The list of answer choices (min 2, max 10).
 * @property correctIndices Set of indices that mark the correct choice(s).
 * @property isMultiSelect Whether the question allows multiple correct answers.
 * @property explanation Optional explanation shown after answering.
 * @property mediaUrl Optional URL for an image or video attached to the question.
 * @property points The point value awarded for a correct answer (1–10).
 */
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val choices: List<ChoiceDraft> = List(4) { ChoiceDraft() },
    val correctIndices: Set<Int> = setOf(0),
    val isMultiSelect: Boolean = false,
    val explanation: String = "",
    val mediaUrl: String = "",
    val points: Int = 1
)

/**
 * Draft model for a choice within a [QuestionDraft].
 *
 * @property id Unique identifier for this choice draft.
 * @property content The display text for this choice.
 */
data class ChoiceDraft(
    val id: String = UUID.randomUUID().toString(),
    val content: String = ""
)

/**
 * UI state for the Create Quiz screen.
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
 * @property title The quiz title.
 * @property description The quiz description.
 * @property thumbnailUrl Optional URL for the quiz cover image.
 * @property isPublic Whether the quiz will be publicly discoverable after publishing.
 *   Always `false` for drafts.
 * @property tags Comma-separated list of tags as raw input text.
 * @property questions The ordered list of question drafts.
 * @property isLoading Whether a save/publish operation is in progress.
 * @property isSaved Whether the quiz has been successfully saved (triggers navigation).
 * @property isDraft Whether the current version is saved only as a draft (not published).
 * @property isPublished Whether the quiz has been successfully published.
 * @property lastSavedAt Epoch millis of the last draft save, or null if never saved.
 * @property shareToPool Whether to contribute each question to the community pool on publish.
 *   Always `false` for drafts.
 * @property error Current error message to display, or null when there is no error.
 */
data class CreateQuizUiState(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val isPublic: Boolean = false,
    val tags: String = "",
    val questions: List<QuestionDraft> = listOf(QuestionDraft()),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDraft: Boolean = true,
    val isPublished: Boolean = false,
    val lastSavedAt: Long? = null,
    val shareToPool: Boolean = false,
    val error: String? = null
)

/**
 * Events that can be dispatched to [CreateQuizViewModel].
 */
sealed class CreateQuizEvent {
    /** Updates the quiz title. */
    data class TitleChanged(val title: String) : CreateQuizEvent()

    /** Updates the quiz description. */
    data class DescriptionChanged(val description: String) : CreateQuizEvent()

    /** Updates the quiz cover image URL. */
    data class ThumbnailUrlChanged(val thumbnailUrl: String) : CreateQuizEvent()

    /**
     * Toggles the public visibility of the quiz.
     *
     * Enabling this implicitly exits draft mode — [CreateQuizUiState.isDraft] becomes `false`
     * so the user knows the next save will publish. Disabling returns to draft mode.
     */
    data class IsPublicChanged(val isPublic: Boolean) : CreateQuizEvent()

    /** Updates the raw comma-separated tags string. */
    data class TagsChanged(val tags: String) : CreateQuizEvent()

    /** Appends a blank question to the end of the question list. */
    data object AddQuestion : CreateQuizEvent()

    /** Replaces the question at [index] with [draft]. */
    data class UpdateQuestion(val index: Int, val draft: QuestionDraft) : CreateQuizEvent()

    /** Removes the question at [index] if more than one question exists. */
    data class RemoveQuestion(val index: Int) : CreateQuizEvent()

    /** Moves the question at [index] one position up in the list. */
    data class MoveQuestionUp(val index: Int) : CreateQuizEvent()

    /** Moves the question at [index] one position down in the list. */
    data class MoveQuestionDown(val index: Int) : CreateQuizEvent()

    /**
     * Saves the current form as a private draft.
     *
     * Regardless of what [CreateQuizUiState.isPublic] and [CreateQuizUiState.shareToPool]
     * show in the UI, the persisted record will have both forced to `false`. The UI toggles
     * are also reset to `false` after a successful draft save so the displayed state stays
     * consistent with what was actually stored.
     */
    data object SaveDraft : CreateQuizEvent()

    /**
     * Validates the quiz and saves it as a published quiz.
     * Sets [CreateQuizUiState.isPublished] to true and triggers [CreateQuizUiState.isSaved].
     */
    data object PublishQuiz : CreateQuizEvent()

    /** Legacy save alias — behaves identically to [PublishQuiz]. */
    data object SaveQuiz : CreateQuizEvent()

    /**
     * Toggles whether each question will be contributed to the community pool on publish.
     *
     * Enabling this implicitly exits draft mode — [CreateQuizUiState.isDraft] becomes `false`.
     * Pool contribution only happens when the quiz is actually published, never on draft saves.
     */
    data class ShareToPoolChanged(val shareToPool: Boolean) : CreateQuizEvent()

    /** Clears the current error message from the UI state. */
    data object ClearError : CreateQuizEvent()
}

/**
 * ViewModel for the Create Quiz screen.
 *
 * Owns the multi-step form state and coordinates draft saving and publishing via the
 * repository. Enforces the invariant that **draft quizzes are always private and never
 * shared to the community pool**:
 *
 * - [CreateQuizEvent.SaveDraft] forces `isPublic = false` and `shareToPool = false` on
 *   the persisted record, then resets both toggles in the UI state.
 * - [CreateQuizEvent.IsPublicChanged] and [CreateQuizEvent.ShareToPoolChanged] set
 *   [CreateQuizUiState.isDraft] to `false` when enabled (signalling publish intent) and
 *   back to `true` when both are disabled (returning to draft mode).
 * - [CreateQuizEvent.PublishQuiz] always sets `isPublic = true` on the persisted record.
 *
 * @param quizRepository Repository for persisting quizzes and questions.
 * @param authRepository Repository for retrieving the currently authenticated user.
 * @param poolRepository Repository for contributing questions to the community pool.
 */
class CreateQuizViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    private val poolRepository: PoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())

    /** Current UI state for the Create Quiz screen. */
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    /**
     * Dispatches a [CreateQuizEvent] to update state or trigger a side effect.
     */
    fun onEvent(event: CreateQuizEvent) {
        when (event) {
            is CreateQuizEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title) }

            is CreateQuizEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.description) }

            is CreateQuizEvent.ThumbnailUrlChanged ->
                _uiState.update { it.copy(thumbnailUrl = event.thumbnailUrl) }

            is CreateQuizEvent.IsPublicChanged -> _uiState.update { state ->
                // Enabling public visibility exits draft mode.
                // Disabling it returns to draft mode only if shareToPool is also off.
                val newIsDraft = if (event.isPublic) false else !state.shareToPool
                state.copy(isPublic = event.isPublic, isDraft = newIsDraft)
            }

            is CreateQuizEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.tags) }

            is CreateQuizEvent.AddQuestion ->
                _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }

            is CreateQuizEvent.UpdateQuestion ->
                _uiState.update { state ->
                    state.copy(questions = state.questions.toMutableList().apply {
                        this[event.index] = event.draft
                    })
                }

            is CreateQuizEvent.RemoveQuestion ->
                _uiState.update { state ->
                    if (state.questions.size > 1) {
                        state.copy(
                            questions = state.questions.toMutableList().apply {
                                removeAt(event.index)
                            }
                        )
                    } else state
                }

            is CreateQuizEvent.MoveQuestionUp ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx <= 0 || idx >= state.questions.size) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
                }

            is CreateQuizEvent.MoveQuestionDown ->
                _uiState.update { state ->
                    val idx = event.index
                    if (idx < 0 || idx >= state.questions.size - 1) return@update state
                    val list = state.questions.toMutableList()
                    val temp = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = temp
                    state.copy(questions = list)
                }

            is CreateQuizEvent.SaveDraft ->
                onSaveQuiz(publishAfterSave = false)

            is CreateQuizEvent.PublishQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is CreateQuizEvent.SaveQuiz ->
                onSaveQuiz(publishAfterSave = true)

            is CreateQuizEvent.ShareToPoolChanged -> _uiState.update { state ->
                // Enabling share-to-pool exits draft mode.
                // Disabling it returns to draft mode only if isPublic is also off.
                val newIsDraft = if (event.shareToPool) false else !state.isPublic
                state.copy(shareToPool = event.shareToPool, isDraft = newIsDraft)
            }

            is CreateQuizEvent.ClearError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Validates and persists the quiz.
     *
     * ### Draft path (`publishAfterSave = false`)
     * - `isPublic` is forced to `false` — drafts are always private.
     * - `shareToPool` is skipped entirely — pool contribution never runs for drafts.
     * - Both [CreateQuizUiState.isPublic] and [CreateQuizUiState.shareToPool] are reset to
     *   `false` in the UI state after a successful save so the form reflects reality.
     * - [CreateQuizUiState.isDraft] is set to `true` and [CreateQuizUiState.lastSavedAt]
     *   is updated.
     *
     * ### Publish path (`publishAfterSave = true`)
     * - `isPublic` is forced to `true`.
     * - `shareToPool` contribution runs if the toggle is on.
     * - [CreateQuizUiState.isSaved] is set to `true` to trigger back navigation.
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
            val quizId = UUID.randomUUID().toString()
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
                questionCount = state.questions.size
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

            val result = quizRepository.saveQuiz(quiz, questions)
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
