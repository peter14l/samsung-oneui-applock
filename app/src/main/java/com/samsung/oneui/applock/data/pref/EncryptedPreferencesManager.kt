package com.samsung.oneui.applock.data.pref

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class EncryptedPreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to standard private prefs", e)
            appContext.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    // In-memory synchronized Set for O(1) instant lookup in AccessibilityService
    private val _lockedPackagesFlow = MutableStateFlow<Set<String>>(emptySet())
    val lockedPackagesFlow: StateFlow<Set<String>> = _lockedPackagesFlow.asStateFlow()

    private val _maskedPackagesFlow = MutableStateFlow<Set<String>>(emptySet())
    val maskedPackagesFlow: StateFlow<Set<String>> = _maskedPackagesFlow.asStateFlow()

    private val _settingsFlow = MutableStateFlow(LockSettings())
    val settingsFlow: StateFlow<LockSettings> = _settingsFlow.asStateFlow()

    // Temporary session unlocked packages: PackageName -> Timestamp of unlock
    private val unlockedSessions = ConcurrentHashMap<String, Long>()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        val lockedSet = prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        _lockedPackagesFlow.value = lockedSet

        val maskedSet = prefs.getStringSet(KEY_MASKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        _maskedPackagesFlow.value = maskedSet

        val masterLock = prefs.getBoolean(KEY_MASTER_LOCK, true)
        val masterMasking = prefs.getBoolean(KEY_MASTER_NOTIFICATION_MASKING, true)
        val maskModeName = prefs.getString(KEY_NOTIFICATION_MASK_MODE, NotificationMaskMode.GLOBAL.name)
        val maskMode = try {
            NotificationMaskMode.valueOf(maskModeName ?: NotificationMaskMode.GLOBAL.name)
        } catch (e: Exception) {
            NotificationMaskMode.GLOBAL
        }

        val relockPolicyName = prefs.getString(KEY_RELOCK_POLICY, RelockPolicy.SCREEN_OFF.name)
        val relockPolicy = try {
            RelockPolicy.valueOf(relockPolicyName ?: RelockPolicy.SCREEN_OFF.name)
        } catch (e: Exception) {
            RelockPolicy.SCREEN_OFF
        }

        _settingsFlow.value = LockSettings(
            masterLockEnabled = masterLock,
            masterNotificationMasking = masterMasking,
            notificationMaskMode = maskMode,
            relockPolicy = relockPolicy
        )
    }

    fun isPackageLocked(packageName: String): Boolean {
        if (!_settingsFlow.value.masterLockEnabled) return false
        return _lockedPackagesFlow.value.contains(packageName)
    }

    fun isNotificationMasked(packageName: String): Boolean {
        val settings = _settingsFlow.value
        if (!settings.masterLockEnabled || !settings.masterNotificationMasking) return false
        return when (settings.notificationMaskMode) {
            NotificationMaskMode.GLOBAL -> _lockedPackagesFlow.value.contains(packageName)
            NotificationMaskMode.PER_APP -> _maskedPackagesFlow.value.contains(packageName)
        }
    }

    fun isSessionUnlocked(packageName: String): Boolean {
        return unlockedSessions.containsKey(packageName)
    }

    fun unlockPackageSession(packageName: String) {
        unlockedSessions[packageName] = System.currentTimeMillis()
    }

    fun lockPackageSession(packageName: String) {
        unlockedSessions.remove(packageName)
    }

    fun clearAllUnlockedSessions() {
        unlockedSessions.clear()
    }

    fun setPackageLocked(packageName: String, locked: Boolean) {
        val current = _lockedPackagesFlow.value.toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            unlockedSessions.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackagesFlow.value = current
    }

    fun setPackageNotificationMasked(packageName: String, masked: Boolean) {
        val current = _maskedPackagesFlow.value.toMutableSet()
        if (masked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_MASKED_PACKAGES, current).apply()
        _maskedPackagesFlow.value = current
    }

    fun setMasterLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_LOCK, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(masterLockEnabled = enabled)
    }

    fun setMasterNotificationMasking(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_NOTIFICATION_MASKING, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(masterNotificationMasking = enabled)
    }

    fun setNotificationMaskMode(mode: NotificationMaskMode) {
        prefs.edit().putString(KEY_NOTIFICATION_MASK_MODE, mode.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(notificationMaskMode = mode)
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        prefs.edit().putString(KEY_RELOCK_POLICY, policy.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(relockPolicy = policy)
    }

    companion object {
        private const val TAG = "EncryptedPrefs"
        private const val PREFS_FILE_NAME = "samsung_oneui_applock_secure_prefs"
        private const val FALLBACK_PREFS_FILE = "samsung_oneui_applock_fallback_prefs"

        private const val KEY_LOCKED_PACKAGES = "key_locked_packages"
        private const val KEY_MASKED_PACKAGES = "key_masked_packages"
        private const val KEY_MASTER_LOCK = "key_master_lock"
        private const val KEY_MASTER_NOTIFICATION_MASKING = "key_master_notification_masking"
        private const val KEY_NOTIFICATION_MASK_MODE = "key_notification_mask_mode"
        private const val KEY_RELOCK_POLICY = "key_relock_policy"

        @Volatile
        private var INSTANCE: EncryptedPreferencesManager? = null

        fun getInstance(context: Context): EncryptedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptedPreferencesManager(context).also { INSTANCE = it }
            }
        }
    }
}
