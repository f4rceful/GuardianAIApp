package com.example.guardianai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardianai.data.GuardianRepository
import com.example.guardianai.data.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application,
    private val repository: GuardianRepository
) : AndroidViewModel(application) {

    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val list = repository.getHistory(getApplication())
        _historyList.value = list
    }

    fun enterSelectionMode(initialId: Long? = null) {
        _isSelectionMode.value = true
        if (initialId != null) {
            _selectedItems.value = setOf(initialId)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        val current = _selectedItems.value
        if (current.contains(id)) {
            _selectedItems.value = current - id
        } else {
            _selectedItems.value = current + id
        }
    }

    fun selectAll() {
        if (_selectedItems.value.size == _historyList.value.size) {
            _selectedItems.value = emptySet()
        } else {
            _selectedItems.value = _historyList.value.map { it.id }.toSet()
        }
    }

    fun deleteSelected() {
        val idsToDelete = _selectedItems.value.toList()
        repository.deleteHistoryItems(getApplication(), idsToDelete)
        loadHistory()
        exitSelectionMode()
    }

    fun sendFeedback(item: HistoryItem) {
        if (!item.isScam) return // Logic for sending feedback only if it was marked as scam? 
        // Or if user says it is NOT scam.
        
        viewModelScope.launch {
            try {
                repository.sendFeedback(item.text, isScamReport = false)
            } catch (e: Exception) {
                // Ignore error for now or expose state
            }
        }
    }
}
