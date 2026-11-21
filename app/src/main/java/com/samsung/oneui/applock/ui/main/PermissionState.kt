package com.samsung.oneui.applock.ui.main

import com.samsung.oneui.applock.data.model.InterceptionEngine

data class PermissionState(
    val isAccessibilityEnabled: Boolean = false,
    val isUsageAccessGranted: Boolean = false,
    val isAppearOnTopGranted: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isPostNotificationsGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val interceptionEngine: InterceptionEngine = InterceptionEngine.ACCESSIBILITY
) {
    val allRequiredGranted: Boolean
        get() {
            val engineReady = when (interceptionEngine) {
                InterceptionEngine.ACCESSIBILITY -> isAccessibilityEnabled
                InterceptionEngine.USAGE_ACCESS -> isUsageAccessGranted && isAppearOnTopGranted
            }
            return engineReady && isNotificationListenerEnabled && isPostNotificationsGranted
        }
}
