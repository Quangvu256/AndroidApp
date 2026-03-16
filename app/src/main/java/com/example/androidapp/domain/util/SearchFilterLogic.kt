package com.example.androidapp.domain.util

/**
 * Utility object for filtering lists of quizzes based on search criteria.
 */
object SearchFilterLogic {

    /**
     * Filters a list of items (quizzes) by tags, visibility, and date range.
     * If a filter parameter is null, that specific filter is skipped.
     *
     * @param T The type representing a Quiz model.
     * @param items The list of quizzes to filter.
     * @param queryTags A set of tags to filter by. If null or empty, tag filtering is skipped.
     * @param isPublic Only include public quizzes if true, private if false. If null, skipped.
     * @param startDateMillis The start of the date range in milliseconds. If null, skipped.
     * @param endDateMillis The end of the date range in milliseconds. If null, skipped.
     * @param getTags A selector function to extract tags from a quiz.
     * @param getIsPublic A selector function to extract the visibility status from a quiz.
     * @param getTimestampMillis A selector function to extract the creation or modification timestamp from a quiz.
     * @return The filtered list of quizzes.
     */
    fun <T> filter(
        items: List<T>,
        queryTags: Set<String>? = null,
        isPublic: Boolean? = null,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        getTags: (T) -> List<String>,
        getIsPublic: (T) -> Boolean,
        getTimestampMillis: (T) -> Long
    ): List<T> {
        return items.filter { item ->
            // 1. Filter by tags: item must contain at least one of the query tags (OR logic).
            val matchTags = if (queryTags.isNullOrEmpty()) {
                true
            } else {
                val itemTags = getTags(item)
                itemTags.any { it in queryTags }
            }

            // 2. Filter by visibility (public/private)
            val matchVisibility = if (isPublic == null) {
                true
            } else {
                getIsPublic(item) == isPublic
            }

            // 3. Filter by date range
            val itemTime = getTimestampMillis(item)
            val matchStartDate = if (startDateMillis == null) {
                true
            } else {
                itemTime >= startDateMillis
            }

            val matchEndDate = if (endDateMillis == null) {
                true
            } else {
                itemTime <= endDateMillis
            }

            // Item must pass all provided criteria
            matchTags && matchVisibility && matchStartDate && matchEndDate
        }
    }
}