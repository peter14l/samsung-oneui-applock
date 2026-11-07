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
data class LockSettings(
    val masterLockEnabled: Boolean = true,
    val masterNotificationMasking: Boolean = true,
    val notificationMaskMode: NotificationMaskMode = NotificationMaskMode.GLOBAL,
    val relockPolicy: RelockPolicy = RelockPolicy.SCREEN_OFF
)
