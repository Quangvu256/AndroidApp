package com.example.androidapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val recentQuizzes: List<Quiz> = emptyList(),
    val myQuizzes: List<Quiz> = emptyList(),
    val trendingQuizzes: List<Quiz> = emptyList(),
    val joinCode: String = "",
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val displayName: String = ""
)

/** Events that can be dispatched to [HomeViewModel]. */
sealed class HomeEvent {
    data class JoinCodeChanged(val code: String) : HomeEvent()
    data class JoinQuiz(val code: String) : HomeEvent()
    data object Refresh : HomeEvent()
    data object ClearError : HomeEvent()
}

/**
 * ViewModel for the Home screen.
 * Loads recent, owned, and trending quizzes using the local-first pattern.
 */
class HomeViewModel(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** Current UI state for the Home screen. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null, displayName = user?.displayName ?: "") }
                if (user != null) {
                    loadHomeData(user.id)
                }
            }
        }
    }

    /**
     * Dispatches a [HomeEvent] to the ViewModel.
     */
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.JoinCodeChanged -> _uiState.update { it.copy(joinCode = event.code) }
            is HomeEvent.JoinQuiz -> { /* Navigation handled by screen */ }
            is HomeEvent.Refresh -> {
                val userId = _uiState.value.let { if (it.isLoggedIn) it.displayName else "" }
                // Re-trigger collection by resetting state
            }
            is HomeEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadHomeData(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            quizRepository.getHomeQuizzes(userId).collect { homeQuizzes ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        recentQuizzes = homeQuizzes.recentAttemptQuizzes,
                        myQuizzes = homeQuizzes.myQuizzes,
                        trendingQuizzes = homeQuizzes.trendingQuizzes
                    )
                }
            }
        }
    }
}

