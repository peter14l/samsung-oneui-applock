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

        // Add common Samsung launcher & system UI components
        cachedLauncherPackages.add("com.sec.android.app.launcher") // Samsung One UI Home
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

        val launchers = getLauncherPackages()
        return launchers.contains(packageName)
    }
}
