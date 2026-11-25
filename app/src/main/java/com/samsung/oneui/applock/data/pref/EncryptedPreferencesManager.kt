package com.samsung.oneui.applock.data.pref

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class EncryptedPreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext

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

    fun refreshState() {
        loadInitialState()
    }

    private fun loadInitialState() {
        val lockedSet = prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        _lockedPackagesFlow.value = lockedSet

        val maskedSet = prefs.getStringSet(KEY_MASKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
        _maskedPackagesFlow.value = maskedSet

        val masterLock = prefs.getBoolean(KEY_MASTER_LOCK, true)
        val engineName = prefs.getString(KEY_INTERCEPTION_ENGINE, InterceptionEngine.ACCESSIBILITY.name)
        val interceptionEngine = try {
            InterceptionEngine.valueOf(engineName ?: InterceptionEngine.ACCESSIBILITY.name)
        } catch (e: Exception) {
            InterceptionEngine.ACCESSIBILITY
        }
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

        val authTypeName = prefs.getString(KEY_AUTH_TYPE, AuthType.SYSTEM_CREDENTIAL.name)
        val authType = try {
            AuthType.valueOf(authTypeName ?: AuthType.SYSTEM_CREDENTIAL.name)
        } catch (e: Exception) {
            AuthType.SYSTEM_CREDENTIAL
        }

        val customPinHash = prefs.getString(KEY_CUSTOM_PIN_HASH, null)
        val isCustomPinSet = !customPinHash.isNullOrBlank()
        val customPinLength = prefs.getInt(KEY_CUSTOM_PIN_LENGTH, 4)
        val useBiometricWithCustomPin = prefs.getBoolean(KEY_USE_BIOMETRIC_CUSTOM_PIN, false)
        val isDeviceAdmin = PermissionHelper.isDeviceAdminActive(appContext)

        val isHideLockedApps = prefs.getBoolean(KEY_HIDE_LOCKED_APPS, false)
        val isGuardPlayStore = prefs.getBoolean(KEY_GUARD_PLAY_STORE, true)
        val isSelfLock = prefs.getBoolean(KEY_SELF_LOCK, true)
        val designStyleName = prefs.getString(KEY_LOCK_DESIGN_STYLE, LockDesignStyle.DYNAMIC_THUMB.name)
        val lockDesignStyle = try {
            LockDesignStyle.valueOf(designStyleName ?: LockDesignStyle.DYNAMIC_THUMB.name)
        } catch (e: Exception) {
            LockDesignStyle.DYNAMIC_THUMB
        }

        _settingsFlow.value = LockSettings(
            masterLockEnabled = masterLock,
            interceptionEngine = interceptionEngine,
            masterNotificationMasking = masterMasking,
            notificationMaskMode = maskMode,
            relockPolicy = relockPolicy,
            authType = authType,
            isCustomPinSet = isCustomPinSet,
            customPinLength = customPinLength,
            useBiometricWithCustomPin = useBiometricWithCustomPin,
            isDeviceAdminEnabled = isDeviceAdmin,
            isHideLockedAppsEnabled = isHideLockedApps,
            isGuardPlayStoreAndSettings = isGuardPlayStore,
            isSelfLockEnabled = isSelfLock,
            lockDesignStyle = lockDesignStyle
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

        if (_settingsFlow.value.isHideLockedAppsEnabled && PermissionHelper.isDeviceOwner(appContext)) {
            PermissionHelper.setApplicationHidden(appContext, packageName, locked)
        }
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

    fun setInterceptionEngine(engine: InterceptionEngine) {
        prefs.edit().putString(KEY_INTERCEPTION_ENGINE, engine.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(interceptionEngine = engine)
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

    fun setAuthType(type: AuthType) {
        prefs.edit().putString(KEY_AUTH_TYPE, type.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(authType = type)
    }

    fun setUseBiometricWithCustomPin(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_BIOMETRIC_CUSTOM_PIN, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(useBiometricWithCustomPin = enabled)
    }

    fun getRemainingLockoutSeconds(): Long {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        return if (lockoutUntil > now) {
            (lockoutUntil - now + 999) / 1000
        } else {
            0L
        }
    }

    fun setCustomPin(pin: String): Boolean {
        if (pin.length !in 4..6) return false
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_CUSTOM_PIN_HASH, hash)
            .putInt(KEY_CUSTOM_PIN_LENGTH, pin.length)
            .apply()
        resetFailedPinAttempts()
        _settingsFlow.value = _settingsFlow.value.copy(
            isCustomPinSet = true,
            customPinLength = pin.length,
            authType = AuthType.CUSTOM_PIN
        )
        return true
    }

    fun verifyCustomPin(pin: String): Boolean {
        if (getRemainingLockoutSeconds() > 0) {
            return false
        }

        val storedHash = prefs.getString(KEY_CUSTOM_PIN_HASH, null) ?: return false
        val salt = getOrCreateSalt()
        val isPbkdf2 = storedHash.startsWith(PBKDF2_PREFIX)
        val computedHash = if (isPbkdf2) hashPin(pin, salt) else legacyHashPin(pin, salt)

        // Constant-time comparison to prevent timing attacks
        val matches = MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            computedHash.toByteArray(Charsets.UTF_8)
        )

        if (matches) {
            if (!isPbkdf2) {
                // Transparently upgrade legacy SHA-256 PIN hash to PBKDF2
                prefs.edit().putString(KEY_CUSTOM_PIN_HASH, hashPin(pin, salt)).apply()
            }
            resetFailedPinAttempts()
            return true
        } else {
            recordFailedPinAttempt()
            return false
        }
    }

    private fun recordFailedPinAttempt() {
        val currentAttempts = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, currentAttempts)

        val lockoutDurationMs = when {
            currentAttempts >= 10 -> 60_000L // 60 seconds
            currentAttempts >= 5 -> 30_000L  // 30 seconds
            else -> 0L
        }

        if (lockoutDurationMs > 0) {
            val lockoutUntil = System.currentTimeMillis() + lockoutDurationMs
            editor.putLong(KEY_PIN_LOCKOUT_UNTIL, lockoutUntil)
        }
        editor.apply()
    }

    fun resetFailedPinAttempts() {
        prefs.edit()
            .remove(KEY_FAILED_PIN_ATTEMPTS)
            .remove(KEY_PIN_LOCKOUT_UNTIL)
            .apply()
    }

    fun removeCustomPin() {
        prefs.edit()
            .remove(KEY_CUSTOM_PIN_HASH)
            .remove(KEY_CUSTOM_PIN_LENGTH)
            .apply()
        resetFailedPinAttempts()
        _settingsFlow.value = _settingsFlow.value.copy(
            isCustomPinSet = false,
            customPinLength = 4,
            authType = AuthType.SYSTEM_CREDENTIAL,
            useBiometricWithCustomPin = false
        )
    }

    fun setHideLockedApps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_LOCKED_APPS, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isHideLockedAppsEnabled = enabled)
        // If Device Owner is active, hide/unhide locked packages at system level
        if (PermissionHelper.isDeviceOwner(appContext)) {
            val locked = _lockedPackagesFlow.value
            for (pkg in locked) {
                PermissionHelper.setApplicationHidden(appContext, pkg, enabled)
            }
        }
    }

    fun setGuardPlayStoreAndSettings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUARD_PLAY_STORE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isGuardPlayStoreAndSettings = enabled)
        val current = _lockedPackagesFlow.value.toMutableSet()
        val playStorePkg = "com.android.vending"
        val settingsPkg = "com.android.settings"
        if (enabled) {
            current.add(playStorePkg)
            current.add(settingsPkg)
        } else {
            current.remove(playStorePkg)
            current.remove(settingsPkg)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedPackagesFlow.value = current
    }

    fun setSelfLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SELF_LOCK, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isSelfLockEnabled = enabled)
    }

    fun setLockDesignStyle(style: LockDesignStyle) {
        prefs.edit().putString(KEY_LOCK_DESIGN_STYLE, style.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(lockDesignStyle = style)
    }

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(KEY_PIN_SALT, null)
        if (salt.isNullOrEmpty()) {
            val random = SecureRandom()
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            salt = bytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt
    }

    private fun hashPin(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        return PBKDF2_PREFIX + derived.joinToString("") { "%02x".format(it) }
    }

    private fun legacyHashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "EncryptedPrefs"
        private const val PBKDF2_PREFIX = "pbkdf2:"
        private const val PBKDF2_ITERATIONS = 12_000
        private const val PBKDF2_KEY_BITS = 256
        private const val PREFS_FILE_NAME = "samsung_oneui_applock_secure_prefs"
        private const val FALLBACK_PREFS_FILE = "samsung_oneui_applock_fallback_prefs"

        private const val KEY_LOCKED_PACKAGES = "key_locked_packages"
        private const val KEY_MASKED_PACKAGES = "key_masked_packages"
        private const val KEY_MASTER_LOCK = "key_master_lock"
        private const val KEY_INTERCEPTION_ENGINE = "key_interception_engine"
        private const val KEY_MASTER_NOTIFICATION_MASKING = "key_master_notification_masking"
        private const val KEY_NOTIFICATION_MASK_MODE = "key_notification_mask_mode"
        private const val KEY_RELOCK_POLICY = "key_relock_policy"
        private const val KEY_AUTH_TYPE = "key_auth_type"
        private const val KEY_CUSTOM_PIN_HASH = "key_custom_pin_hash"
        private const val KEY_CUSTOM_PIN_LENGTH = "key_custom_pin_length"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_FAILED_PIN_ATTEMPTS = "key_failed_pin_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL = "key_pin_lockout_until"
        private const val KEY_USE_BIOMETRIC_CUSTOM_PIN = "key_use_biometric_custom_pin"
        private const val KEY_HIDE_LOCKED_APPS = "key_hide_locked_apps"
        private const val KEY_GUARD_PLAY_STORE = "key_guard_play_store"
        private const val KEY_SELF_LOCK = "key_self_lock"
        private const val KEY_LOCK_DESIGN_STYLE = "key_lock_design_style"

        @Volatile
        private var INSTANCE: EncryptedPreferencesManager? = null

        fun getInstance(context: Context): EncryptedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncryptedPreferencesManager(context).also { INSTANCE = it }
            }
        }
    }
}
