package com.samsung.oneui.applock.ui.main

data class PermissionState(
    val isAccessibilityEnabled: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isPostNotificationsGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false
) {
    val allRequiredGranted: Boolean
        get() = isAccessibilityEnabled && isNotificationListenerEnabled && isPostNotificationsGranted
}
