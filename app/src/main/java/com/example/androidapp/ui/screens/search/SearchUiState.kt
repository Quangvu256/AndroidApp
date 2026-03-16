package com.example.androidapp.ui.screens.search

/**
 * Các tùy chọn sắp xếp kết quả tìm kiếm (Task 5).
 * Định nghĩa Enum ngay tại đây để SearchUiState có thể tham chiếu tới.
 */
enum class SortOption { DATE, POPULARITY, RELEVANCE }

/**
 * Trạng thái UI cho màn hình Tìm kiếm (Gồm Task 1 đến Task 5).
 */
data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,

    // Task 2: Tag Filter
    val availableTags: List<String> = emptyList(),
    val selectedTags: List<String> = emptyList(),

    // Task 3: Danh sách kết quả tìm kiếm
    val searchResults: List<QuizCardDraft> = emptyList(),

    // Task 4: Cờ xác định chế độ xem hiện tại
    val isGridView: Boolean = true,

    // Task 5: Cờ xác định tùy chọn sắp xếp hiện tại
    val sortOption: SortOption = SortOption.RELEVANCE,
    // Task 6: Cờ xác định đã bấm tìm kiếm chưa (Dùng để hiện EmptyState)
    val hasSearched: Boolean = false
)