package com.example.androidapp.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.androidapp.domain.model.Question
import com.example.androidapp.ui.components.feedback.ErrorState
import com.example.androidapp.ui.components.feedback.LoadingSpinner
import com.example.androidapp.ui.components.quiz.DynamicChoiceList
import com.example.androidapp.ui.components.quiz.QuizProgressIndicator

/**
 * Take Quiz screen where user answers questions.
 * Stateless composable; all state is owned by [TakeQuizViewModel].
 *
 * @param quizId The ID of the quiz being taken.
 * @param onNavigateBack Callback to exit the quiz.
 * @param onQuizComplete Callback with the attempt ID when quiz is submitted.
 * @param modifier Modifier for styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeQuizScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onQuizComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: TakeQuizViewModel = viewModel(
        key = "take_$quizId",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TakeQuizViewModel(
                    quizId,
                    container.quizRepository,
                    container.attemptRepository,
                    container.authRepository
                ) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is TakeQuizUiState.Finished) {
            onQuizComplete((uiState as TakeQuizUiState.Finished).attemptId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? TakeQuizUiState.Active)?.quizTitle
                        ?: stringResource(R.string.take_quiz_title)
                    Text(text = title)
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is TakeQuizUiState.Loading -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is TakeQuizUiState.Error -> ErrorState(
                message = state.message,
                onRetry = onNavigateBack,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            is TakeQuizUiState.Active -> ActiveQuizContent(
                state = state,
                onAnswerSelected = { viewModel.onEvent(TakeQuizEvent.AnswerSelected(it)) },
                onNext = { viewModel.onEvent(TakeQuizEvent.NextQuestion) },
                onPrevious = { viewModel.onEvent(TakeQuizEvent.PreviousQuestion) },
                onSubmit = { viewModel.onEvent(TakeQuizEvent.SubmitQuiz) },
                modifier = Modifier.padding(innerPadding)
            )
            is TakeQuizUiState.Finished -> LoadingSpinner(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        }
    }
}

@Composable
private fun ActiveQuizContent(
    state: TakeQuizUiState.Active,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuizProgressIndicator(
            currentQuestionIndex = state.currentIndex,
            totalQuestions = state.totalQuestions,
            modifier = Modifier.fillMaxWidth()
        )

        // The QuizProgressIndicator already shows the question progress text,
        // so we skip duplicating it here

        Text(
            text = state.currentQuestion.content,
            style = MaterialTheme.typography.titleLarge
        )

        DynamicChoiceList(
            choices = state.currentQuestion.choices,
            selectedChoiceIds = state.selectedAnswers,
            onChoiceSelected = onAnswerSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.currentIndex > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.previous))
                }
            }
            val isLast = state.currentIndex == state.totalQuestions - 1
            Button(
                onClick = if (isLast) onSubmit else onNext,
                modifier = Modifier.weight(1f),
                enabled = !state.isSubmitting
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        if (isLast) stringResource(R.string.submit)
                        else stringResource(R.string.next)
                    )
                }
            }
        }
    }
}
