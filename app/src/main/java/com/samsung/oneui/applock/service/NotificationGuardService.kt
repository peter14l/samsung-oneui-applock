package com.samsung.oneui.applock.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.samsung.oneui.applock.AppLockApplication
import com.samsung.oneui.applock.R
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.util.PermissionHelper
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class NotificationGuardService : NotificationListenerService() {

    private lateinit var prefsManager: EncryptedPreferencesManager
    private val suppressedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NotificationGuardService onCreate")
        prefsManager = EncryptedPreferencesManager.getInstance(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        val sbnKey = sbn.key ?: return

        // 1. Prevent infinite loops: ignore our own package notifications
        if (pkgName == packageName) {
            return
        }

        // 2. Prevent infinite loops: check if this is already a replacement notification we posted
        if (sbn.tag == REPLACEMENT_NOTIFICATION_TAG || suppressedKeys.contains(sbnKey)) {
            suppressedKeys.remove(sbnKey)
            return
        }

        // 3. Check if app is locked and notification masking is enabled
        if (!prefsManager.isNotificationMasked(pkgName)) {
            return
        }

        // Do not suppress ongoing or foreground service notifications (e.g. Media players, timers, calls)
        val originalNotification = sbn.notification
        if (sbn.isOngoing || (originalNotification != null && (originalNotification.flags and Notification.FLAG_ONGOING_EVENT != 0))) {
            return
        }

        // Check if user has already unlocked this package session
        if (prefsManager.isSessionUnlocked(pkgName)) {
            return
        }

        // 4. Cancel the sensitive notification
        try {
            suppressedKeys.add(sbnKey)
            cancelNotification(sbnKey)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel sensitive notification $sbnKey", e)
        }

        // 5. Post masked replacement notification
        postMaskedReplacement(sbn, pkgName)
    }

    private fun postMaskedReplacement(sbn: StatusBarNotification, targetPackage: String) {
        // Permission check for Android 13+ (API 33 / TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.isPostNotificationsGranted(this)) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted on Android 13+, skipping replacement")
                return
            }
        }

        val originalNotification = sbn.notification ?: return
        val contentIntent = originalNotification.contentIntent

        // Resolve user-friendly app name
        val appName = try {
            val pm = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(targetPackage, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(targetPackage, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            targetPackage
        }

        val maskedNotification = NotificationCompat.Builder(this, AppLockApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock_badge)
            .setContentTitle(appName)
            .setContentText(getString(R.string.notification_masked_content))
            .setSubText(getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (contentIntent != null) {
                    setContentIntent(contentIntent)
                }
                if (originalNotification.group != null) {
                    setGroup(originalNotification.group)
                }
            }
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.notify(
                REPLACEMENT_NOTIFICATION_TAG,
                sbn.id,
                maskedNotification
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post masked replacement notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { suppressedKeys.remove(it) }
    }

    companion object {
        private const val TAG = "NotificationGuard"
        private const val REPLACEMENT_NOTIFICATION_TAG = "samsung_oneui_masked_notification"
    }
}
