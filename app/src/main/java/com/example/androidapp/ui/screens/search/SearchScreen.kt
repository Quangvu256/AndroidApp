package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidapp.R
import com.example.androidapp.di.LocalAppContainer
import com.example.androidapp.ui.components.feedback.EmptyState
import com.example.androidapp.ui.components.forms.QuizSearchBar

/**
 * Man hinh Tim kiem / Kham pha de tim cac bai kiem tra cong khai.
 *
 * Day la composable phi trang thai (stateless); toan bo trang thai duoc
 * quan ly boi [SearchViewModel]. Man hinh tich hop:
 * - [QuizSearchBar] — thanh tim kiem voi lich su tim kiem gan day.
 * - [TagFilterRow] — hang loc tu khoa (tag) kieu multi-select.
 * - [SearchControlsRow] — dieu khien sap xep va chuyen doi che do xem.
 * - [SearchResultsGrid] / [SearchResultsList] — hien thi ket qua dang luoi hoac danh sach.
 * - [EmptyState] — thong bao khi khong co ket qua.
 *
 * @param onNavigateToQuiz Callback khi nguoi dung chon mot bai kiem tra, truyen quiz ID.
 * @param modifier Modifier tuy chinh giao dien.
 */
@Composable
fun SearchScreen(
    onNavigateToQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer
    val viewModel: SearchViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SearchViewModel(container.quizRepository, container.searchRepository) as T
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // 1. Thanh tim kiem voi lich su tim kiem gan day
        QuizSearchBar(
            query = uiState.query,
            recentSearches = uiState.recentSearches,
            onEvent = viewModel::onEvent,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Hang loc tu khoa (an khi khong co tag nao)
        TagFilterRow(
            tags = uiState.availableTags,
            selectedTags = uiState.selectedTags,
            onTagClick = { tag -> viewModel.onEvent(SearchEvent.OnTagToggle(tag)) },
            modifier = Modifier.fillMaxWidth()
        )

        // 3. Dieu khien sap xep va chuyen doi che do xem (chi hien khi co ket qua)
        if (uiState.searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            SearchControlsRow(
                currentSort = uiState.sortOption,
                isGridView = uiState.isGridView,
                onSortSelected = { option ->
                    viewModel.onEvent(SearchEvent.OnSortOptionSelected(option))
                },
                onToggleView = { viewModel.onEvent(SearchEvent.OnToggleViewMode) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Vung hien thi ket qua
        when {
            // Dang tai ket qua
            uiState.isSearching -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Da tim kiem nhung khong co ket qua
            uiState.hasSearched && uiState.searchResults.isEmpty() -> {
                EmptyState(
                    message = stringResource(R.string.search_no_results_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Co ket qua — hien thi theo che do xem hien tai
            uiState.searchResults.isNotEmpty() -> {
                if (uiState.isGridView) {
                    SearchResultsGrid(
                        results = uiState.searchResults,
                        onQuizClick = onNavigateToQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    SearchResultsList(
                        results = uiState.searchResults,
                        onQuizClick = onNavigateToQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}
