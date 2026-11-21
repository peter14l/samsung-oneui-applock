package com.samsung.oneui.applock.service

import android.app.ActivityOptions
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.lock.LockOverlayActivity
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
 * Uses Android's UsageStatsManager (App Usage Access) instead of AccessibilityService.
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
                    Log.d(TAG, "Screen ON detected: resuming usage polling")
                    checkAndResumePolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AppLockUsageService onCreate")
        prefsManager = EncryptedPreferencesManager.getInstance(this)
        systemPackageFilter = SystemPackageFilter(this)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenOn = powerManager.isInteractive

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

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
        checkAndResumePolling()
        return START_STICKY
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
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 1500, now) ?: return
        val event = UsageEvents.Event()
        var latestPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestPackage = event.packageName
            }
        }

        val eventPackage = latestPackage ?: return

        if (eventPackage == lastForegroundPackage) {
            return
        }

        // Ignore system packages & our own app
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

        lastForegroundPackage = eventPackage

        if (!masterLockEnabled || currentEngine != InterceptionEngine.USAGE_ACCESS) {
            return
        }

        if (lockedPackagesSnapshot.contains(eventPackage)) {
            if (prefsManager.isSessionUnlocked(eventPackage)) {
                return
            }
            launchLockOverlay(eventPackage)
        }
    }

    private fun launchLockOverlay(targetPackage: String) {
        try {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, targetPackage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            startActivity(intent, options)
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
        serviceScope.cancel()
        Log.i(TAG, "AppLockUsageService onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AppLockUsageService"

        fun start(context: Context) {
            try {
                val intent = Intent(context, AppLockUsageService::class.java)
                context.startService(intent)
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
