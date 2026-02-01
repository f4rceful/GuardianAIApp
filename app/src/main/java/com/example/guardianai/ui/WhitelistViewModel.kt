package com.example.guardianai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardianai.data.GuardianRepository
import com.example.guardianai.util.AppInfo
import com.example.guardianai.util.AppManager
import com.example.guardianai.util.ContactInfo
import com.example.guardianai.util.ContactManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhitelistViewModel(
    application: Application,
    private val repository: GuardianRepository
) : AndroidViewModel(application) {

    // 0 = Apps, 1 = Contacts
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Data lists need to be observed from Repository (which observes SettingsManager)
    // SettingsManager uses mutableStateOf, we can bridge it to Flow or expose as is if we were using Compose state in VM, 
    // but for MVVM purity let's expose Flows.
    // However, since SettingsManager is a singleton with MutableState, we can just expose computed flows or update them manually.
    // Ideally, SettingsManager should expose Flows. For now, we'll pull data when needed or rely on Compose re-composition if we used the same singleton state.
    // But to make it cleaner, let's mirror state.
    
    private val _trustedApps = MutableStateFlow<Set<String>>(emptySet())
    val trustedApps: StateFlow<Set<String>> = _trustedApps.asStateFlow()

    private val _trustedContacts = MutableStateFlow<Set<String>>(emptySet())
    val trustedContacts: StateFlow<Set<String>> = _trustedContacts.asStateFlow()

    // Picker States
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _contactsList = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contactsList: StateFlow<List<ContactInfo>> = _contactsList.asStateFlow()

    private val _isLoadingPicker = MutableStateFlow(false)
    val isLoadingPicker: StateFlow<Boolean> = _isLoadingPicker.asStateFlow()

    // Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    init {
        refreshData()
    }

    private fun refreshData() {
        _trustedApps.value = repository.getTrustedApps()
        _trustedContacts.value = repository.getTrustedContacts()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
        exitSelectionMode()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingPicker.value = true
            val apps = withContext(Dispatchers.IO) {
                AppManager.getInstalledApps(getApplication())
            }
            _installedApps.value = apps
            _isLoadingPicker.value = false
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoadingPicker.value = true
            val contacts = withContext(Dispatchers.IO) {
                ContactManager.getContacts(getApplication())
            }
            _contactsList.value = contacts
            _isLoadingPicker.value = false
        }
    }

    fun addTrustedApps(packageNames: List<String>) {
        packageNames.forEach { repository.addTrustedApp(getApplication(), it) }
        refreshData()
    }

    fun addTrustedContacts(names: List<String>) {
        names.forEach { repository.addTrustedContact(getApplication(), it) }
        refreshData()
    }

    fun removeTrustedApp(packageName: String) {
        repository.removeTrustedApp(getApplication(), packageName)
        refreshData()
    }

    fun removeTrustedContact(name: String) {
        repository.removeTrustedContact(getApplication(), name)
        refreshData()
    }

    // Selection Mode Logic
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedItems.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun toggleSelection(item: String) {
        val current = _selectedItems.value
        if (current.contains(item)) {
            _selectedItems.value = current - item
        } else {
            _selectedItems.value = current + item
        }
    }
    
    fun deleteSelected() {
        val items = _selectedItems.value.toList()
        if (_selectedTab.value == 0) {
            items.forEach { removeTrustedApp(it) }
        } else {
            items.forEach { removeTrustedContact(it) }
        }
        exitSelectionMode()
    }
}
