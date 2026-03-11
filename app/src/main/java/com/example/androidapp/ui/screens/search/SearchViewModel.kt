package com.example.androidapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Search screen.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<Quiz> = emptyList(),
    val trendingQuizzes: List<Quiz> = emptyList(),
    val availableFilters: List<String> = emptyList(),
    val selectedFilter: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Events that can be dispatched to [SearchViewModel]. */
sealed class SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent()
    data class FilterSelected(val filter: String) : SearchEvent()
    data object ClearQuery : SearchEvent()
    data object ClearError : SearchEvent()
}

/**
 * ViewModel for the Search screen.
 * Debounces search input and provides results from the local cache first.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val quizRepository: QuizRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    /** Current UI state for the Search screen. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        loadTrending()
        viewModelScope.launch {
            _queryFlow.debounce(400L).collectLatest { query ->
                if (query.isBlank()) {
                    _uiState.update { it.copy(results = emptyList(), isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = true) }
                    quizRepository.searchQuizzes(query).collect { quizzes ->
                        _uiState.update { it.copy(results = quizzes, isLoading = false) }
                    }
                }
            }
        }
    }

    /**
     * Dispatches a [SearchEvent] to the ViewModel.
     */
    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                _uiState.update { it.copy(query = event.query) }
                _queryFlow.value = event.query
            }
            is SearchEvent.FilterSelected -> _uiState.update { it.copy(selectedFilter = event.filter) }
            is SearchEvent.ClearQuery -> {
                _uiState.update { it.copy(query = "", results = emptyList()) }
                _queryFlow.value = ""
            }
            is SearchEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            quizRepository.getPublicQuizzes().collect { quizzes ->
                _uiState.update { it.copy(trendingQuizzes = quizzes) }
            }
        }
    }
}



