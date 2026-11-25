package com.samsung.oneui.applock.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.oneui.applock.AppLockApplication
import com.samsung.oneui.applock.data.model.AppInfo
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.repository.AppLockRepository
import com.samsung.oneui.applock.service.AppLockUsageService
import com.samsung.oneui.applock.update.AppUpdater
import com.samsung.oneui.applock.update.UpdateInfo
import com.samsung.oneui.applock.update.UpdateState
import com.samsung.oneui.applock.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

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
        silentCheckForUpdates()

        // Sync UsageService with engine state
        viewModelScope.launch {
            settings.collectLatest { s ->
                val context = getApplication<Application>()
                if (s.interceptionEngine == InterceptionEngine.USAGE_ACCESS && s.masterLockEnabled) {
                    AppLockUsageService.start(context)
                } else {
                    AppLockUsageService.stop(context)
                }
            }
        }
    }

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        val currentEngine = settings.value.interceptionEngine
        _permissionState.value = PermissionState(
            isAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context),
            isUsageAccessGranted = PermissionHelper.isUsageAccessGranted(context),
            isAppearOnTopGranted = PermissionHelper.isAppearOnTopGranted(context),
            isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context),
            isPostNotificationsGranted = PermissionHelper.isPostNotificationsGranted(context),
            isBatteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context),
            isDeviceAdminActive = PermissionHelper.isDeviceAdminActive(context),
            interceptionEngine = currentEngine
        )
        repository.refreshState()
    }

    fun setInterceptionEngine(engine: InterceptionEngine) {
        repository.setInterceptionEngine(engine)
        val context = getApplication<Application>()
        if (engine == InterceptionEngine.USAGE_ACCESS && settings.value.masterLockEnabled) {
            AppLockUsageService.start(context)
        } else {
            AppLockUsageService.stop(context)
        }
        refreshPermissionState()
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

    fun setAuthType(type: AuthType) {
        repository.setAuthType(type)
    }

    fun setUseBiometricWithCustomPin(enabled: Boolean) {
        repository.setUseBiometricWithCustomPin(enabled)
    }

    fun setCustomPin(pin: String): Boolean {
        return repository.setCustomPin(pin)
    }

    fun getRemainingLockoutSeconds(): Long {
        return repository.getRemainingLockoutSeconds()
    }

    fun removeCustomPin() {
        repository.removeCustomPin()
    }

    fun setHideLockedApps(enabled: Boolean) {
        repository.setHideLockedApps(enabled)
    }

    fun setGuardPlayStoreAndSettings(enabled: Boolean) {
        repository.setGuardPlayStoreAndSettings(enabled)
    }

    fun setSelfLock(enabled: Boolean) {
        repository.setSelfLock(enabled)
    }

    fun setLockDesignStyle(style: LockDesignStyle) {
        repository.setLockDesignStyle(style)
    }

    private fun silentCheckForUpdates() {
        viewModelScope.launch {
            val result = AppUpdater.checkForUpdate()
            if (result is UpdateState.Available) {
                _updateState.value = result
            }
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Downloading || _updateState.value is UpdateState.Installing) {
            return
        }
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            _updateState.value = AppUpdater.checkForUpdate()
        }
    }

    fun startUpdateDownload(info: UpdateInfo) {
        viewModelScope.launch {
            AppUpdater.downloadAndInstall(getApplication(), info) { state ->
                _updateState.value = state
            }
        }
    }

    fun resetUpdateStateIfFinished() {
        val current = _updateState.value
        if (current is UpdateState.UpToDate || current is UpdateState.Error || current is UpdateState.Checking) {
            _updateState.value = UpdateState.Idle
        }
    }
}
