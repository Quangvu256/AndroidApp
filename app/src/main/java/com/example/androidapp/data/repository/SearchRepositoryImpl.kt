package com.example.androidapp.data.repository

import android.content.Context
import com.example.androidapp.domain.repository.SearchRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of [SearchRepository] that persists recent search queries
 * using [SharedPreferences] and [Gson] for JSON serialization.
 *
 * Recent searches are capped at [MAX_RECENT_SEARCHES] items, stored in
 * most-recent-first order, and automatically deduplicated.
 *
 * @param context Application context used to obtain SharedPreferences.
 */
class SearchRepositoryImpl(context: Context) : SearchRepository {

    private val gson = Gson()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recentSearches = MutableStateFlow(loadSearches())

    /**
     * Returns a [Flow] that emits the current list of recent search queries
     * whenever the list is updated. Items are ordered most-recent-first.
     */
    override fun getRecentSearches(): Flow<List<String>> = _recentSearches.asStateFlow()

    /**
     * Adds [query] to the top of the recent searches list.
     *
     * If [query] already exists in the list it is moved to the top rather than
     * duplicated. The list is trimmed to [MAX_RECENT_SEARCHES] entries after
     * insertion. Blank queries are ignored.
     *
     * @param query The search term to record.
     */
    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val current = _recentSearches.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)

        val updated = current.take(MAX_RECENT_SEARCHES)
        saveSearches(updated)
        _recentSearches.value = updated
    }

    /**
     * Removes all recent search entries from storage and notifies collectors
     * with an empty list.
     */
    override suspend fun clearRecentSearches() {
        saveSearches(emptyList())
        _recentSearches.value = emptyList()
    }

    /**
     * Loads the persisted list of recent searches from SharedPreferences.
     *
     * @return The deserialized list, or an empty list when no data is stored.
     */
    private fun loadSearches(): List<String> {
        val json = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Persists [searches] to SharedPreferences as a JSON array.
     */
    private fun saveSearches(searches: List<String>) {
        val json = gson.toJson(searches)
        prefs.edit().putString(KEY_RECENT_SEARCHES, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "search_prefs"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 10
    }
}
