package com.samsung.oneui.applock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.samsung.oneui.applock.data.model.AppInfo
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AppLockRepository(private val context: Context) {

    private val prefsManager = EncryptedPreferencesManager.getInstance(context)
    private val packageManager: PackageManager = context.packageManager

    val lockedPackagesFlow: Flow<Set<String>> = prefsManager.lockedPackagesFlow
    val maskedPackagesFlow: Flow<Set<String>> = prefsManager.maskedPackagesFlow
    val settingsFlow: Flow<LockSettings> = prefsManager.settingsFlow

    suspend fun getInstalledLauncherApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        val myPackage = context.packageName
        val lockedSet = prefsManager.lockedPackagesFlow.value
        val maskedSet = prefsManager.maskedPackagesFlow.value

        val uniqueApps = resolveInfos
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != myPackage }

        uniqueApps.mapNotNull { pkg ->
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        pkg,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(pkg, 0)
                }

                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                AppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isLocked = lockedSet.contains(pkg),
                    isNotificationMasked = maskedSet.contains(pkg),
                    isSystemApp = isSystem
                )
            } catch (e: Exception) {
                null
            }
        }.sortedWith(
            compareByDescending<AppInfo> { it.isLocked }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun observeApps(): Flow<Pair<Set<String>, Set<String>>> {
        return combine(lockedPackagesFlow, maskedPackagesFlow) { locked, masked ->
            Pair(locked, masked)
        }
    }

    fun toggleAppLock(packageName: String, isLocked: Boolean) {
        prefsManager.setPackageLocked(packageName, isLocked)
    }

    fun toggleNotificationMask(packageName: String, isMasked: Boolean) {
        prefsManager.setPackageNotificationMasked(packageName, isMasked)
    }

    fun setMasterLock(enabled: Boolean) {
        prefsManager.setMasterLockEnabled(enabled)
    }

    fun setMasterNotificationMasking(enabled: Boolean) {
        prefsManager.setMasterNotificationMasking(enabled)
    }

    fun setNotificationMaskMode(mode: NotificationMaskMode) {
        prefsManager.setNotificationMaskMode(mode)
    }

    fun setRelockPolicy(policy: RelockPolicy) {
        prefsManager.setRelockPolicy(policy)
    }

    fun unlockSession(packageName: String) {
        prefsManager.unlockPackageSession(packageName)
    }

    fun lockSession(packageName: String) {
        prefsManager.lockPackageSession(packageName)
    }

    fun clearAllSessions() {
        prefsManager.clearAllUnlockedSessions()
    }

    fun isAppLocked(packageName: String): Boolean {
        return prefsManager.isPackageLocked(packageName)
    }

    fun isSessionUnlocked(packageName: String): Boolean {
        return prefsManager.isSessionUnlocked(packageName)
    }
}
