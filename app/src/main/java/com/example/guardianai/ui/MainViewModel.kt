package com.example.guardianai.ui

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardianai.data.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    private val repository: GuardianRepository
) : AndroidViewModel(application) {

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(true)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()

    init {
        checkPermission()
        startServerCheck()
    }

    fun checkPermission() {
        val context = getApplication<Application>()
        val packageName = context.packageName
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        _isPermissionGranted.value = enabledListeners.contains(packageName)
    }

    private fun startServerCheck() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Initial check
                checkServer()
                
                // Done loading
                _isCheckingStatus.value = false

                // Polling
                while (true) {
                    delay(5000)
                    checkServer()
                }
            }
        }
    }

    private suspend fun checkServer() {
        val connected = repository.checkServerConnection()
        _isServerConnected.value = connected
    }
}
