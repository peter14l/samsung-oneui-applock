package com.samsung.oneui.applock.service

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.samsung.oneui.applock.R
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.lock.LockOverlayActivity
import com.samsung.oneui.applock.util.PermissionHelper
import com.samsung.oneui.applock.util.SystemPackageFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Banking & Payment Friendly Interception Engine.
 * Runs as a lightweight Foreground Service using Android's UsageStatsManager (App Usage Access)
 * and SYSTEM_ALERT_WINDOW ("Appear on top") instead of AccessibilityService.
 * Completely invisible to Google Pay, PhonePe, Paytm, and banking security detectors.
 */
class AppLockUsageService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    private lateinit var prefsManager: EncryptedPreferencesManager
    private lateinit var systemPackageFilter: SystemPackageFilter
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var powerManager: PowerManager

    @Volatile
    private var lockedPackagesSnapshot: Set<String> = emptySet()

    @Volatile
    private var masterLockEnabled: Boolean = true

    @Volatile
    private var currentEngine: InterceptionEngine = InterceptionEngine.ACCESSIBILITY

    @Volatile
    private var relockPolicy: RelockPolicy = RelockPolicy.SCREEN_OFF

    @Volatile
    private var isScreenOn: Boolean = true

    private var lastForegroundPackage: String? = null
    private var lastEventTimestamp: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    Log.d(TAG, "Screen OFF detected: clearing unlocked sessions & pausing usage polling")
                    prefsManager.clearAllUnlockedSessions()
                    pausePolling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    lastForegroundPackage = null
                    Log.d(TAG, "Screen ON detected: resuming usage polling")
                    checkAndResumePolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AppLockUsageService onCreate")
        startForegroundProtection()

        prefsManager = EncryptedPreferencesManager.getInstance(this)
        systemPackageFilter = SystemPackageFilter(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenOn = powerManager.isInteractive
        lastEventTimestamp = System.currentTimeMillis() - 2000L

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Observe locked packages
        serviceScope.launch {
            prefsManager.lockedPackagesFlow.collectLatest { set ->
                lockedPackagesSnapshot = set
            }
        }

        // Observe settings
        serviceScope.launch {
            prefsManager.settingsFlow.collectLatest { settings ->
                masterLockEnabled = settings.masterLockEnabled
                currentEngine = settings.interceptionEngine
                relockPolicy = settings.relockPolicy

                if (currentEngine == InterceptionEngine.USAGE_ACCESS && masterLockEnabled) {
                    checkAndResumePolling()
                } else {
                    pausePolling()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundProtection()
        checkAndResumePolling()
        return START_STICKY
    }

    private fun startForegroundProtection() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.usage_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.usage_service_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.usage_service_notification_title))
            .setContentText(getString(R.string.usage_service_notification_text))
            .setSmallIcon(R.drawable.ic_lock_badge)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "AppLockUsageService running in foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground for AppLockUsageService", e)
        }
    }

    private fun checkAndResumePolling() {
        if (pollingJob?.isActive == true) return
        if (!isScreenOn || !masterLockEnabled || currentEngine != InterceptionEngine.USAGE_ACCESS) {
            return
        }

        pollingJob = serviceScope.launch {
            Log.i(TAG, "Starting UsageStatsManager detection loop (Banking Friendly Mode)")
            while (isActive) {
                try {
                    detectForegroundApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in usage stats detection loop", e)
                }
                delay(200)
            }
        }
    }

    private fun pausePolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.i(TAG, "Paused UsageStatsManager detection loop")
    }

    private fun detectForegroundApp() {
        if (!PermissionHelper.isUsageAccessGranted(this)) {
            return
        }

        val now = System.currentTimeMillis()
        val queryStart = if (lastEventTimestamp <= 0L || (now - lastEventTimestamp > 5000L)) {
            now - 2000L
        } else {
            lastEventTimestamp
        }

        val events = usageStatsManager.queryEvents(queryStart, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null

        if (events != null) {
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.timeStamp > lastEventTimestamp) {
                    lastEventTimestamp = event.timeStamp
                }
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    latestPackage = event.packageName
                }
            }
        }

        // Fallback for screen wake-up directly into app without new resume event
        if (latestPackage == null && lastForegroundPackage == null) {
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 4000L, now)
            latestPackage = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        }

        val eventPackage = latestPackage ?: return

        // Ignore our own package
        if (eventPackage == packageName) {
            return
        }

        if (eventPackage == lastForegroundPackage) {
            return
        }

        // Ignore system packages & launchers
        if (systemPackageFilter.isSystemOrIgnored(eventPackage)) {
            if (relockPolicy == RelockPolicy.IMMEDIATE && lastForegroundPackage != null && lastForegroundPackage != eventPackage) {
                prefsManager.lockPackageSession(lastForegroundPackage!!)
            }
            lastForegroundPackage = eventPackage
            return
        }

        if (relockPolicy == RelockPolicy.IMMEDIATE && lastForegroundPackage != null && lastForegroundPackage != eventPackage) {
            prefsManager.lockPackageSession(lastForegroundPackage!!)
        }

        if (!masterLockEnabled || currentEngine != InterceptionEngine.USAGE_ACCESS) {
            lastForegroundPackage = eventPackage
            return
        }

        if (lockedPackagesSnapshot.contains(eventPackage)) {
            if (prefsManager.isSessionUnlocked(eventPackage)) {
                lastForegroundPackage = eventPackage
                return
            }
            if (!PermissionHelper.isAppearOnTopGranted(this)) {
                Log.w(TAG, "Cannot launch lock overlay for $eventPackage: Appear on Top permission not granted")
                return
            }
            lastForegroundPackage = eventPackage
            launchLockOverlay(eventPackage)
        } else {
            lastForegroundPackage = eventPackage
        }
    }

    private fun launchLockOverlay(targetPackage: String) {
        try {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, targetPackage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                options.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    targetPackage.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent.send(options.toBundle())
            } else {
                startActivity(intent, options.toBundle())
            }
            Log.i(TAG, "Launched LockOverlayActivity for $targetPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch LockOverlayActivity for $targetPackage", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        pausePolling()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        serviceScope.cancel()
        Log.i(TAG, "AppLockUsageService onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AppLockUsageService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "app_lock_usage_channel"

        fun start(context: Context) {
            try {
                val intent = Intent(context, AppLockUsageService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppLockUsageService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AppLockUsageService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AppLockUsageService", e)
            }
        }
    }
}
