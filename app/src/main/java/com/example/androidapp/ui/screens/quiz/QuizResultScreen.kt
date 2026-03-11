package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.feedback.ScoreCard

/**
 * Quiz result screen showing score and options after completing a quiz.
 * Stateless composable; all state is owned by [QuizResultViewModel].
 *
 * @param quizId The ID of the completed quiz.
 * @param attemptId The ID of the attempt (for loading results).
 * @param onNavigateHome Callback to go back to home.
 * @param onRetryQuiz Callback to retry the quiz.
 * @param onReviewAnswers Callback to review answers.
 * @param modifier Modifier for styling.
 */
@Composable
fun QuizResultScreen(
    quizId: String,
    attemptId: String,
    onNavigateHome: () -> Unit,
    onRetryQuiz: () -> Unit,
    onReviewAnswers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: QuizResultViewModel = viewModel(
        key = "result_${attemptId}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuizResultViewModel(quizId, attemptId, container.quizRepository, container.attemptRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { innerPadding ->
        when (val state = uiState) {
            is QuizResultUiState.Loading -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is QuizResultUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.onRetry() },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is QuizResultUiState.Success -> ResultContent(
                quiz = state.quiz,
                attempt = state.attempt,
                percentage = state.percentage,
                starRating = state.starRating,
                onNavigateHome = onNavigateHome,
                onRetryQuiz = onRetryQuiz,
                onReviewAnswers = onReviewAnswers,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ResultContent(
    quiz: Quiz,
    attempt: Attempt,
    percentage: Int,
    starRating: Int,
    onNavigateHome: () -> Unit,
    onRetryQuiz: () -> Unit,
    onReviewAnswers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = quiz.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        ScoreCard(
            score = attempt.score,
            maxScore = attempt.totalQuestions,
            correctCount = attempt.score,
            wrongCount = attempt.totalQuestions - attempt.score,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.quiz_result_go_home))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRetryQuiz,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Replay, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.quiz_result_try_again))
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onReviewAnswers,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quiz_result_review_answers))
        }
    }
}
