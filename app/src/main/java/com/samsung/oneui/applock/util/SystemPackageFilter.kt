package com.samsung.oneui.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class SystemPackageFilter(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val cachedLauncherPackages = mutableSetOf<String>()
    private var lastQueryTime = 0L

    fun getLauncherPackages(): Set<String> {
        val now = System.currentTimeMillis()
        if (cachedLauncherPackages.isNotEmpty() && (now - lastQueryTime < 60_000)) {
            return cachedLauncherPackages
        }

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        cachedLauncherPackages.clear()
        for (info in resolveInfos) {
            info.activityInfo?.packageName?.let { cachedLauncherPackages.add(it) }
        }

        // Add common OEM launchers (Samsung, Motorola, AOSP, Pixel)
        cachedLauncherPackages.add("com.sec.android.app.launcher")
        cachedLauncherPackages.add("com.motorola.launcher3")
        cachedLauncherPackages.add("com.motorola.launcherconfig")
        cachedLauncherPackages.add("com.android.launcher3")
        cachedLauncherPackages.add("com.google.android.apps.nexuslauncher")

        lastQueryTime = now
        return cachedLauncherPackages
    }

    fun isSystemOrIgnored(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName == "com.android.systemui") return true
        if (packageName == "android") return true
        if (packageName.startsWith("com.samsung.android.biometrics")) return true
        if (packageName.startsWith("com.samsung.android.app.cocktailbarservice")) return true // Edge panel

        // Ignore system permission controller dialogs
        if (packageName.startsWith("com.android.permissioncontroller") ||
            packageName.startsWith("com.google.android.permissioncontroller")) return true

        // Ignore transient system file & photo pickers to avoid locking parent apps mid-task
        if (packageName == "com.android.documentsui") return true
        if (packageName == "com.google.android.providers.media.module") return true
        if (packageName.startsWith("com.sec.android.app.myfiles")) return true

        // Ignore transient share sheets and in-call UIs to prevent premature immediate relock
        if (packageName == "com.android.intentresolver") return true
        if (packageName == "com.android.incallui" ||
            packageName == "com.samsung.android.incallui" ||
            packageName == "com.google.android.dialer") return true

        val launchers = getLauncherPackages()
        return launchers.contains(packageName)
    }
}
