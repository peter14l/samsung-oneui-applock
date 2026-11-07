package com.samsung.oneui.applock.data.model

import android.graphics.drawable.Drawable
import androidx.annotation.Keep

@Keep
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isLocked: Boolean = false,
    val isNotificationMasked: Boolean = false,
    val isSystemApp: Boolean = false
)
