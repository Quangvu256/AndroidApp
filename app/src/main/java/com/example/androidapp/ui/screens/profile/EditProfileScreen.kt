package com.example.androidapp.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Edit Profile screen that allows the current user to update their display name
 * and avatar image.
 *
 * The screen uses [ActivityResultContracts.GetContent] to let the user pick an
 * image from the device gallery. The selected image is uploaded via
 * [EditProfileViewModel] and the resulting URL is stored in the UI state.
 * When the save operation completes successfully, the composable navigates back
 * via [onNavigateBack].
 *
 * @param onNavigateBack Callback invoked when the user presses the back button
 *   or after a successful save.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: EditProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditProfileViewModel(
                    authRepository = container.authRepository,
                    storageRepository = container.storageRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_edit_saved)

    // Navigate back once the save completes successfully
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Show error in a Snackbar and then clear it from state
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(EditProfileEvent.ClearError)
        }
    }

    // Image picker launcher – opens the system gallery for "image/*" content
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(EditProfileEvent.AvatarSelected(uri))
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_edit_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // "Lưu" text button in the top-right corner
                    TextButton(
                        onClick = { viewModel.onEvent(EditProfileEvent.SaveProfile) },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        EditProfileContent(
            uiState = uiState,
            onDisplayNameChanged = { viewModel.onEvent(EditProfileEvent.DisplayNameChanged(it)) },
            onChangeAvatarClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * Stateless content area for the Edit Profile screen.
 *
 * Displays the avatar (with a camera icon overlay for picking a new image),
 * a [TextInputField] for the display name, and a read-only email field.
 *
 * @param uiState The current [EditProfileUiState] to render.
 * @param onDisplayNameChanged Callback for display-name text changes.
 * @param onChangeAvatarClick Callback invoked when the user taps the avatar or the change-avatar button.
 * @param modifier Modifier for the root layout.
 */
@Composable
private fun EditProfileContent(
    uiState: EditProfileUiState,
    onDisplayNameChanged: (String) -> Unit,
    onChangeAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Avatar with camera-overlay button
        EditableAvatar(
            photoUrl = uiState.photoUrl,
            initial = uiState.displayName.firstOrNull()?.toString()
                ?: uiState.email.firstOrNull()?.toString()
                ?: "?",
            isLoading = uiState.isLoading,
            onChangeAvatarClick = onChangeAvatarClick
        )

        // "Đổi ảnh đại diện" button
        Button(
            onClick = onChangeAvatarClick,
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(text = stringResource(R.string.profile_edit_change_avatar))
        }

        // Display name input
        TextInputField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChanged,
            label = stringResource(R.string.profile_edit_display_name),
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        // Read-only email field
        TextInputField(
            value = uiState.email,
            onValueChange = {},
            label = stringResource(R.string.profile_edit_email),
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Circular avatar composable that renders either a remote image (via Coil's
 * [AsyncImage]) or a coloured circle containing the user's [initial].
 *
 * A semi-transparent camera icon overlay is placed at the bottom-right to
 * signal that the avatar is tappable. A [CircularProgressIndicator] covers the
 * avatar while an upload is in progress.
 *
 * @param photoUrl Optional remote URL for the avatar image.
 * @param initial Fallback character shown when no image is available.
 * @param isLoading Whether an upload operation is currently running.
 * @param onChangeAvatarClick Callback invoked when the avatar is tapped.
 * @param modifier Modifier for styling.
 */
@Composable
private fun EditableAvatar(
    photoUrl: String?,
    initial: String,
    isLoading: Boolean,
    onChangeAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Avatar image or initials circle
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            // Camera icon badge – bottom-right corner
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
            ) {
                IconButton(
                    onClick = onChangeAvatarClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.profile_edit_change_avatar),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Light",
    showBackground = true
)
@Composable
private fun EditProfileContentLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    photoUrl = null
                ),
                onDisplayNameChanged = {},
                onChangeAvatarClick = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Dark",
    showBackground = true
)
@Composable
private fun EditProfileContentDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    photoUrl = null
                ),
                onDisplayNameChanged = {},
                onChangeAvatarClick = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Edit Profile Content – Loading",
    showBackground = true
)
@Composable
private fun EditProfileContentLoadingPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    displayName = "Nguyen Van A",
                    email = "nguyenvana@example.com",
                    isLoading = true
                ),
                onDisplayNameChanged = {},
                onChangeAvatarClick = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Editable Avatar – Light",
    showBackground = true,
    widthDp = 200,
    heightDp = 200
)
@Composable
private fun EditableAvatarLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                EditableAvatar(
                    photoUrl = null,
                    initial = "N",
                    isLoading = false,
                    onChangeAvatarClick = {}
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Editable Avatar – Dark",
    showBackground = true,
    widthDp = 200,
    heightDp = 200
)
@Composable
private fun EditableAvatarDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                EditableAvatar(
                    photoUrl = null,
                    initial = "N",
                    isLoading = false,
                    onChangeAvatarClick = {}
                )
            }
        }
    }
}
