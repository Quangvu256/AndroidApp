package com.example.androidapp.ui.screens.create

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared ViewModel scoped to the create/edit quiz navigation sub-graph.
 * Holds the draft for the currently edited question so both the list screen
 * and a potential question-editor screen can share state.
 */
class SharedQuizViewModel : ViewModel() {

    private val _editingIndex = MutableStateFlow<Int?>(null)

    /** The index of the question currently being edited, or null if none. */
    val editingIndex: StateFlow<Int?> = _editingIndex.asStateFlow()

    private val _editingDraft = MutableStateFlow<QuestionDraft?>(null)

    /** The [QuestionDraft] currently being edited, or null if none. */
    val editingDraft: StateFlow<QuestionDraft?> = _editingDraft.asStateFlow()

    /**
     * Starts editing the question at [index] with the given [draft].
     */
    fun onStartEditing(index: Int, draft: QuestionDraft) {
        _editingIndex.value = index
        _editingDraft.value = draft
    }

    /**
     * Updates the draft being edited.
     */
    fun onDraftChanged(draft: QuestionDraft) {
        _editingDraft.update { draft }
    }

    /**
     * Clears the editing state.
     */
    fun onClearEditing() {
        _editingIndex.value = null
        _editingDraft.value = null
    }
}

