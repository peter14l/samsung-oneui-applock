package com.samsung.oneui.applock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.data.repository.AppLockRepository
import com.samsung.oneui.applock.service.AppLockUsageService

class AppLockApplication : Application() {

    lateinit var repository: AppLockRepository
        private set

    lateinit var prefsManager: EncryptedPreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsManager = EncryptedPreferencesManager.getInstance(this)
        repository = AppLockRepository(this)
        createNotificationChannels()

        val settings = prefsManager.settingsFlow.value
        if (settings.interceptionEngine == InterceptionEngine.USAGE_ACCESS && settings.masterLockEnabled) {
            AppLockUsageService.start(this)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "channel_samsung_oneui_applock_alerts"

        @Volatile
        lateinit var instance: AppLockApplication
            private set
    }
}
