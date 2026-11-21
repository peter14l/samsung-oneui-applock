package com.samsung.oneui.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.lock.LockOverlayActivity
import com.samsung.oneui.applock.util.SystemPackageFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var prefsManager: EncryptedPreferencesManager
    private lateinit var systemPackageFilter: SystemPackageFilter

    // Local cached in-memory set for O(1) instant lookup
    @Volatile
    private var lockedPackagesSnapshot: Set<String> = emptySet()

    @Volatile
    private var masterLockEnabled: Boolean = true

    @Volatile
    private var relockPolicy: RelockPolicy = RelockPolicy.SCREEN_OFF

    private var lastForegroundPackage: String? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected: Clearing session unlocks")
                prefsManager.clearAllUnlockedSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AppLockAccessibilityService onCreate")
        prefsManager = EncryptedPreferencesManager.getInstance(this)
        systemPackageFilter = SystemPackageFilter(this)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        // Sync locked packages via Flow
        serviceScope.launch {
            prefsManager.lockedPackagesFlow.collectLatest { set ->
                lockedPackagesSnapshot = set
                updateDynamicPackageNames(set)
            }
        }

        // Sync settings via Flow
        serviceScope.launch {
            prefsManager.settingsFlow.collectLatest { settings ->
                masterLockEnabled = settings.masterLockEnabled
                relockPolicy = settings.relockPolicy
                updateDynamicPackageNames(lockedPackagesSnapshot)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateDynamicPackageNames(lockedPackagesSnapshot)
    }

    private fun updateDynamicPackageNames(lockedPackages: Set<String>) {
        try {
            val info = serviceInfo ?: return
            // If master lock is disabled or no apps are locked, scope strictly to our own package.
            // This ensures banking and payment apps are NEVER monitored by the accessibility service.
            val targets = if (!masterLockEnabled || lockedPackages.isEmpty()) {
                arrayOf(packageName)
            } else {
                // Include locked apps, our own package, and home launchers for immediate exit detection
                val launchers = systemPackageFilter.getLauncherPackages()
                (lockedPackages + packageName + launchers).toTypedArray()
            }
            info.packageNames = targets
            serviceInfo = info
            Log.d(TAG, "Accessibility serviceInfo.packageNames scoped to ${targets.size} targets (banking apps excluded)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update packageNames scope", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val eventPackage = event.packageName?.toString() ?: return

        // Ignore our own package and system packages
        if (systemPackageFilter.isSystemOrIgnored(eventPackage)) {
            // If user left a locked app and relock policy is IMMEDIATE, relock it
            if (relockPolicy == RelockPolicy.IMMEDIATE && lastForegroundPackage != null && lastForegroundPackage != eventPackage) {
                prefsManager.lockPackageSession(lastForegroundPackage!!)
            }
            return
        }

        // If relock policy is IMMEDIATE and package changed to another app
        if (relockPolicy == RelockPolicy.IMMEDIATE && lastForegroundPackage != null && lastForegroundPackage != eventPackage) {
            prefsManager.lockPackageSession(lastForegroundPackage!!)
        }

        lastForegroundPackage = eventPackage

        if (!masterLockEnabled) {
            return
        }

        // O(1) instant lookup
        if (lockedPackagesSnapshot.contains(eventPackage)) {
            // Check if already authenticated for current session
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
            val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            startActivity(intent, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch LockOverlayActivity for $targetPackage", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AppLockAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Ignored if receiver wasn't registered
        }
        serviceScope.cancel()
        Log.i(TAG, "AppLockAccessibilityService onDestroy")
    }

    companion object {
        private const val TAG = "AppLockA11yService"
    }
}
