package com.example.androidapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.forms.CodeInputField
import com.example.androidapp.ui.components.quiz.QuizCard

/**
 * Home dashboard screen.
 * Stateless composable; all state is owned by [HomeViewModel].
 *
 * @param onNavigateToQuiz Callback to navigate to quiz detail screen.
 * @param onNavigateToSearch Callback to navigate to search screen.
 * @param modifier Modifier for styling.
 */
@Composable
fun HomeScreen(
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container.quizRepository, container.authRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium
        )

        QuizCodeCard(
            code = uiState.joinCode,
            onCodeChange = { viewModel.onEvent(HomeEvent.JoinCodeChanged(it)) },
            onJoinQuiz = { code -> if (code.length == 6) onNavigateToQuiz(code) }
        )

        SectionHeader(
            title = stringResource(R.string.home_recently_played),
            onSeeAllClick = onNavigateToSearch
        )
        if (uiState.recentQuizzes.isEmpty()) {
            EmptyState(message = stringResource(R.string.home_recently_played_empty))
        } else {
            QuizRow(quizzes = uiState.recentQuizzes, onQuizClick = onNavigateToQuiz)
        }

        SectionHeader(
            title = stringResource(R.string.home_my_quizzes),
            onSeeAllClick = onNavigateToSearch
        )
        if (uiState.myQuizzes.isEmpty()) {
            EmptyState(message = stringResource(R.string.home_my_quizzes_empty))
        } else {
            QuizRow(quizzes = uiState.myQuizzes, onQuizClick = onNavigateToQuiz)
        }

        SectionHeader(
            title = stringResource(R.string.home_trending_quizzes),
            onSeeAllClick = onNavigateToSearch
        )
        if (uiState.trendingQuizzes.isEmpty()) {
            EmptyState(message = stringResource(R.string.home_trending_empty))
        } else {
            QuizRow(quizzes = uiState.trendingQuizzes, onQuizClick = onNavigateToQuiz)
        }
    }
}

@Composable
private fun QuizCodeCard(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoinQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.quiz_code_hint),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            CodeInputField(value = code, onValueChange = onCodeChange)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onJoinQuiz(code) },
                enabled = code.length == 6
            ) {
                Text(stringResource(R.string.quiz_join))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onSeeAllClick) {
            Text(text = stringResource(R.string.home_see_all))
        }
    }
}

@Composable
private fun QuizRow(
    quizzes: List<Quiz>,
    onQuizClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quizzes) { quiz ->
            QuizCard(
                quiz = quiz,
                onClick = { onQuizClick(quiz.id) },
                modifier = Modifier.width(200.dp)
            )
        }
    }
}
