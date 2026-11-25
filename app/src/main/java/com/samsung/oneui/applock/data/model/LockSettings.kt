package com.samsung.oneui.applock.data.model

import androidx.annotation.Keep

@Keep
enum class RelockPolicy {
    IMMEDIATE,
    SCREEN_OFF
}

@Keep
enum class NotificationMaskMode {
    GLOBAL,
    PER_APP
}

@Keep
enum class AuthType {
    SYSTEM_CREDENTIAL,
    CUSTOM_PIN
}

@Keep
enum class InterceptionEngine {
    ACCESSIBILITY,
    USAGE_ACCESS
}

@Keep
enum class LockDesignStyle(val displayName: String, val description: String) {
    DYNAMIC_THUMB("Modern Dynamic", "Material You colors with thumb-friendly bottom controls"),
    VAULT_DARK("Vault Shield", "Dark-first slate surface with glowing security ring"),
    SWISS_MONO("Swiss Monochrome", "High-contrast OLED black & white with crisp borders")
}

@Keep
data class LockSettings(
    val masterLockEnabled: Boolean = true,
    val interceptionEngine: InterceptionEngine = InterceptionEngine.ACCESSIBILITY,
    val masterNotificationMasking: Boolean = true,
    val notificationMaskMode: NotificationMaskMode = NotificationMaskMode.GLOBAL,
    val relockPolicy: RelockPolicy = RelockPolicy.SCREEN_OFF,
    val authType: AuthType = AuthType.SYSTEM_CREDENTIAL,
    val isCustomPinSet: Boolean = false,
    val customPinLength: Int = 4,
    val useBiometricWithCustomPin: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val isHideLockedAppsEnabled: Boolean = false,
    val isGuardPlayStoreAndSettings: Boolean = true,
    val isSelfLockEnabled: Boolean = true,
    val lockDesignStyle: LockDesignStyle = LockDesignStyle.DYNAMIC_THUMB
)
