package com.example.androidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Profile screen.
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false
)

/** Events that can be dispatched to [ProfileViewModel]. */
sealed class ProfileEvent {
    data object Logout : ProfileEvent()
}

/**
 * ViewModel for the Profile screen.
 * Observes auth state and exposes user info.
 */
class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())

    /** Current UI state for the Profile screen. */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isLoggedIn = user != null) }
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

