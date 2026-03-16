package com.example.androidapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interface chịu trách nhiệm quản lý dữ liệu tìm kiếm, bao gồm lịch sử tìm kiếm.
 */
interface SearchRepository {
    /**
     * Lấy danh sách các từ khóa tìm kiếm gần đây.
     */
    fun getRecentSearches(): Flow<List<String>>

    /**
     * Lưu từ khóa tìm kiếm mới vào lịch sử.
     */
    suspend fun addRecentSearch(query: String)

    /**
     * Xóa toàn bộ lịch sử tìm kiếm.
     */
    suspend fun clearRecentSearches()
}