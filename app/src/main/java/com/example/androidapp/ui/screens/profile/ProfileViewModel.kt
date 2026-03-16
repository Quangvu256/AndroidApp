package com.example.androidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Profile screen.
 *
 * @property user The currently authenticated user, or null if logged out.
 * @property isLoggedIn Whether a user is currently authenticated.
 * @property isLoading Whether a background operation (e.g. logout) is in progress.
 * @property quizCount The total number of quizzes owned by the user.
 * @property attemptCount The total number of quiz attempts by the user.
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val quizCount: Int = 0,
    val attemptCount: Int = 0
)

/** Events that can be dispatched to [ProfileViewModel]. */
sealed class ProfileEvent {
    /** Triggers a sign-out operation. */
    data object Logout : ProfileEvent()
}

/**
 * ViewModel for the Profile screen.
 * Observes auth state and exposes user info alongside quiz and attempt stats.
 *
 * @param authRepository Repository for auth operations and current-user observation.
 * @param quizRepository Repository for quiz data; used to count the user's quizzes.
 * @param attemptRepository Repository for attempt data; used to count the user's attempts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())

    /** Current UI state for the Profile screen. */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine the user flow with per-user quiz and attempt flows.
            // When the user is null (logged out) both stat flows emit empty lists.
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(Triple(null, emptyList<Any>(), emptyList<Any>()))
                    } else {
                        combine(
                            quizRepository.getMyQuizzes(user.id),
                            attemptRepository.getAttemptsByUser(user.id)
                        ) { quizzes, attempts ->
                            Triple(user, quizzes, attempts)
                        }
                    }
                }
                .collect { (user, quizzes, attempts) ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isLoggedIn = user != null,
                            quizCount = quizzes.size,
                            attemptCount = attempts.size
                        )
                    }
                }
        }
    }

    /**
     * Dispatches a [ProfileEvent] to the ViewModel.
     */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Logout -> onLogout()
        }
    }

    private fun onLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false, user = null, isLoggedIn = false) }
        }
    }
}
