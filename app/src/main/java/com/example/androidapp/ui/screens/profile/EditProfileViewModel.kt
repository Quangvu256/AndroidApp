package com.example.androidapp.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Edit Profile screen.
 *
 * @property displayName The current value of the display-name text field.
 * @property email The user's email address (read-only; shown but not editable).
 * @property photoUrl The URL of the user's current avatar image, or null if none is set.
 * @property isLoading Whether a background operation (upload or save) is in progress.
 * @property isSaved Whether the profile was saved successfully; used to trigger navigation back.
 * @property error An error message to display, or null when there is no error.
 */
data class EditProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

/**
 * Events that can be dispatched to [EditProfileViewModel].
 */
sealed class EditProfileEvent {
    /**
     * Fired whenever the user edits the display-name field.
     * @property name The updated display name string.
     */
    data class DisplayNameChanged(val name: String) : EditProfileEvent()

    /**
     * Fired when the user selects a new avatar image from the system picker.
     * Triggers an upload via [StorageRepository] and updates [EditProfileUiState.photoUrl]
     * with the resulting download URL.
     * @property uri The content URI of the selected image.
     */
    data class AvatarSelected(val uri: Uri) : EditProfileEvent()

    /**
     * Fired when the user taps the "Lưu" (Save) button.
     * Persists the display name and photo URL via [AuthRepository.updateProfile].
     */
    data object SaveProfile : EditProfileEvent()

    /**
     * Fired to dismiss any visible error snackbar / dialog.
     */
    data object ClearError : EditProfileEvent()
}

/**
 * ViewModel for the Edit Profile screen.
 *
 * Loads the current user on init, handles avatar upload via [StorageRepository],
 * and persists display-name / photo-URL changes via [AuthRepository.updateProfile].
 *
 * @param authRepository Repository for authentication and user-profile operations.
 * @param storageRepository Repository for uploading avatar images to Firebase Storage.
 */
class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())

    /** Current UI state for the Edit Profile screen. */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Dispatches an [EditProfileEvent] to the ViewModel.
     */
    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.DisplayNameChanged -> onDisplayNameChanged(event.name)
            is EditProfileEvent.AvatarSelected -> onAvatarSelected(event.uri)
            is EditProfileEvent.SaveProfile -> onSaveProfile()
            is EditProfileEvent.ClearError -> clearError()
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        displayName = user.displayName,
                        email = user.email,
                        photoUrl = user.photoUrl
                    )
                }
            }
        }
    }

    private fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    private fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.id
                ?: run {
                    _uiState.update { it.copy(error = "Không tìm thấy người dùng") }
                    return@launch
                }

            _uiState.update { it.copy(isLoading = true) }

            storageRepository.uploadImage(userId, uri)
                .onSuccess { downloadUrl ->
                    _uiState.update { it.copy(photoUrl = downloadUrl, isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Tải ảnh lên thất bại"
                        )
                    }
                }
        }
    }

    private fun onSaveProfile() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val displayName = currentState.displayName.trim()

            if (displayName.isBlank()) {
                _uiState.update { it.copy(error = "Tên hiển thị không được để trống") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            authRepository.updateProfile(
                displayName = displayName,
                photoUrl = currentState.photoUrl
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Lưu hồ sơ thất bại"
                        )
                    }
                }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
