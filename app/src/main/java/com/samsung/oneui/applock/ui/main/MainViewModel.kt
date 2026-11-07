package com.samsung.oneui.applock.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.oneui.applock.AppLockApplication
import com.samsung.oneui.applock.data.model.AppInfo
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.repository.AppLockRepository
import com.samsung.oneui.applock.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppLockRepository = (application as AppLockApplication).repository

    val settings: StateFlow<LockSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LockSettings())

    private val _rawApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    // Combined filtered apps list reactive to search and lock status
    val displayedApps: StateFlow<List<AppInfo>> = combine(
        _rawApps,
        _searchQuery,
        repository.lockedPackagesFlow,
        repository.maskedPackagesFlow
    ) { apps, query, lockedSet, maskedSet ->
        val updated = apps.map { app ->
            app.copy(
                isLocked = lockedSet.contains(app.packageName),
                isNotificationMasked = maskedSet.contains(app.packageName)
            )
        }

        if (query.isBlank()) {
            updated.sortedWith(
                compareByDescending<AppInfo> { it.isLocked }
                    .thenBy { it.appName.lowercase() }
            )
        } else {
            val q = query.trim().lowercase()
            updated.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }.sortedWith(
                compareByDescending<AppInfo> { it.isLocked }
                    .thenBy { it.appName.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedCount: StateFlow<Int> = repository.lockedPackagesFlow
        .combine(settings) { locked, s ->
            if (s.masterLockEnabled) locked.size else 0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadInstalledApps()
        refreshPermissionState()
    }

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        _permissionState.value = PermissionState(
            isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context),
            isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context),
            isPostNotificationsGranted = PermissionHelper.isPostNotificationsGranted(context),
            isBatteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
        )
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val apps = repository.getInstalledLauncherApps()
                _rawApps.value = apps
            } catch (e: Exception) {
                _rawApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleAppLock(packageName: String, isLocked: Boolean) {
        repository.toggleAppLock(packageName, isLocked)
    }

    fun toggleNotificationMask(packageName: String, isMasked: Boolean) {
        repository.toggleNotificationMask(packageName, isMasked)
    }

    fun setMasterLock(enabled: Boolean) {
        repository.setMasterLock(enabled)
    }

    fun setMasterNotificationMasking(enabled: Boolean) {
        repository.setMasterNotificationMasking(enabled)
    }

    fun setNotificationMaskMode(mode: NotificationMaskMode) {
        repository.setNotificationMaskMode(mode)
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        repository.setRelockPolicy(policy)
    }
}
