package com.example.androidapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.example.androidapp.domain.util.SearchFilterLogic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel cho man hinh Tim kiem.
 *
 * Xu ly debounce tren o nhap tim kiem (400ms), truy van [QuizRepository] de lay
 * ket qua, luu lich su tim kiem qua [SearchRepository], va cung cap loc tag,
 * sap xep, chuyen doi che do xem (grid/list).
 *
 * Ket qua tho ([Quiz]) duoc luu noi bo de ho tro loc tag; chi nhung ket qua
 * da loc va map thanh [QuizCardDraft] moi duoc phat ra trong [SearchUiState].
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val quizRepository: QuizRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    /** Trang thai UI hien tai cho man hinh Tim kiem. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Danh sach ket qua tho tu repository, giu lai de loc tag va sap xep
     * ma khong can truy van lai.
     */
    private var allResults: List<Quiz> = emptyList()

    /** Flow noi bo de debounce thay doi truy van. */
    private val _queryFlow = MutableStateFlow("")

    init {
        collectRecentSearches()
        observeQueryDebounce()
    }

    /**
     * Dieu phoi mot [SearchEvent] den ViewModel.
     */
    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> handleQueryChange(event.query)
            is SearchEvent.OnClearSearch -> handleClearSearch()
            is SearchEvent.OnSearchClicked -> handleSearchClicked(event.query)
            is SearchEvent.OnRecentSearchClicked -> handleRecentSearchClicked(event.query)
            is SearchEvent.OnClearRecentSearches -> handleClearRecentSearches()
            is SearchEvent.OnTagToggle -> handleTagToggle(event.tag)
            is SearchEvent.OnToggleViewMode -> handleToggleViewMode()
            is SearchEvent.OnSortOptionSelected -> handleSortOptionSelected(event.option)
        }
    }

    // ---------------------------------------------------------------------------
    // Init helpers
    // ---------------------------------------------------------------------------

    /**
     * Thu thap lich su tim kiem gan day tu [SearchRepository] va cap nhat
     * [SearchUiState.recentSearches].
     */
    private fun collectRecentSearches() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collectLatest { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    /**
     * Lang nghe [_queryFlow] voi debounce 400ms. Khi nguoi dung ngung go,
     * tu dong thuc hien tim kiem.
     */
    private fun observeQueryDebounce() {
        viewModelScope.launch {
            _queryFlow
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        allResults = emptyList()
                        _uiState.update {
                            it.copy(
                                searchResults = emptyList(),
                                availableTags = emptyList(),
                                selectedTags = emptyList(),
                                isSearching = false,
                                hasSearched = false
                            )
                        }
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    // ---------------------------------------------------------------------------
    // Event handlers
    // ---------------------------------------------------------------------------

    /**
     * Cap nhat query trong UI state va day gia tri moi vao [_queryFlow] de
     * kich hoat debounce.
     */
    private fun handleQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
    }

    /** Xoa query, ket qua, va dat lai trang thai tim kiem. */
    private fun handleClearSearch() {
        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                availableTags = emptyList(),
                selectedTags = emptyList(),
                isSearching = false,
                hasSearched = false
            )
        }
        allResults = emptyList()
        _queryFlow.value = ""
    }

    /**
     * Xu ly khi nguoi dung nhan nut tim kiem. Luu query vao lich su
     * va thuc hien tim kiem ngay lap tuc (bo qua debounce).
     */
    private fun handleSearchClicked(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
            performSearch(query)
        }
    }

    /**
     * Xu ly khi nguoi dung chon mot tu khoa tim kiem gan day.
     * Dat query, luu vao lich su, va thuc hien tim kiem.
     */
    private fun handleRecentSearchClicked(query: String) {
        _uiState.update { it.copy(query = query) }
        _queryFlow.value = query
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
            performSearch(query)
        }
    }

    /** Xoa toan bo lich su tim kiem. */
    private fun handleClearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    /**
     * Bat/tat mot tag trong danh sach [SearchUiState.selectedTags] va
     * cap nhat ket qua da loc.
     */
    private fun handleTagToggle(tag: String) {
        _uiState.update { state ->
            val updatedTags = if (tag in state.selectedTags) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            state.copy(
                selectedTags = updatedTags,
                searchResults = deriveSearchResults(
                    allResults,
                    updatedTags,
                    state.sortOption
                )
            )
        }
    }

    /** Chuyen doi giua che do xem Grid va List. */
    private fun handleToggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    /**
     * Thay doi tieu chi sap xep va cap nhat lai ket qua da sap xep.
     */
    private fun handleSortOptionSelected(option: SortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = option,
                searchResults = deriveSearchResults(
                    allResults,
                    state.selectedTags,
                    option
                )
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Search & transform helpers
    // ---------------------------------------------------------------------------

    /**
     * Thuc hien truy van tim kiem qua [QuizRepository], luu ket qua tho,
     * trich xuat tag, va cap nhat UI state.
     */
    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }

        quizRepository.searchQuizzes(query).collectLatest { quizzes ->
            allResults = quizzes
            val availableTags = extractAvailableTags(quizzes)

            _uiState.update { state ->
                // Giu lai chi nhung tag da chon ma van con trong ket qua moi
                val validSelectedTags = state.selectedTags.filter { it in availableTags }

                state.copy(
                    isSearching = false,
                    hasSearched = true,
                    availableTags = availableTags,
                    selectedTags = validSelectedTags,
                    searchResults = deriveSearchResults(
                        quizzes,
                        validSelectedTags,
                        state.sortOption
                    )
                )
            }
        }
    }

    /**
     * Trich xuat danh sach tag duy nhat tu ket qua tim kiem, sap xep
     * theo thu tu bang chu cai.
     */
    private fun extractAvailableTags(quizzes: List<Quiz>): List<String> {
        return quizzes
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    /**
     * Loc va sap xep danh sach [Quiz] thanh [QuizCardDraft] dua tren
     * tag da chon va tieu chi sap xep hien tai.
     *
     * @param quizzes Danh sach ket qua tho tu repository.
     * @param selectedTags Cac tag dang duoc chon de loc. Neu rong thi khong loc.
     * @param sortOption Tieu chi sap xep hien tai.
     * @return Danh sach [QuizCardDraft] da loc va sap xep.
     */
    private fun deriveSearchResults(
        quizzes: List<Quiz>,
        selectedTags: List<String>,
        sortOption: SortOption
    ): List<QuizCardDraft> {
        val filtered = SearchFilterLogic.filter(
            items = quizzes,
            queryTags = selectedTags.toSet().ifEmpty { null },
            isPublic = null,
            startDateMillis = null,
            endDateMillis = null,
            getTags = { it.tags },
            getIsPublic = { it.isPublic },
            getTimestampMillis = { it.createdAt }
        )

        val sorted = when (sortOption) {
            SortOption.RELEVANCE -> filtered
            SortOption.DATE -> filtered
            SortOption.POPULARITY -> filtered.sortedByDescending { it.attemptCount }
        }

        return sorted.map { it.toCardDraft() }
    }

    /**
     * Map [Quiz] domain model sang [QuizCardDraft] UI model.
     */
    private fun Quiz.toCardDraft() = QuizCardDraft(
        id = id,
        title = title,
        authorName = authorName,
        questionCount = questionCount,
        attemptCount = attemptCount,
        coverImageUrl = thumbnailUrl
    )
}
