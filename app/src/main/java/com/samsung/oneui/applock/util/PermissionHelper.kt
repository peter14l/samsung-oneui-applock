package com.samsung.oneui.applock.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.samsung.oneui.applock.R
import com.samsung.oneui.applock.receiver.AppLockDeviceAdminReceiver
import com.samsung.oneui.applock.service.AppLockAccessibilityService
import com.samsung.oneui.applock.service.NotificationGuardService

object PermissionHelper {

    fun getDeviceAdminComponent(context: Context): ComponentName {
        return ComponentName(context, AppLockDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.isAdminActive(getDeviceAdminComponent(context))
    }

    fun getDeviceAdminActivationIntent(context: Context): Intent {
        val component = getDeviceAdminComponent(context)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_description)
            )
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun getSamsungHomeSettingsIntent(context: Context): Intent {
        val candidateIntents = listOf(
            Intent().apply {
                setClassName("com.sec.android.app.launcher", "com.sec.android.app.launcher.activities.HideAppsActivity")
            },
            Intent().apply {
                setClassName("com.sec.android.app.launcher", "com.sec.android.app.launcher.activities.HomeSettingsActivity")
            },
            Intent("com.sec.android.app.launcher.action.SETTINGS"),
            Intent(Settings.ACTION_HOME_SETTINGS)
        )

        val pm = context.packageManager
        for (intent in candidateIntents) {
            if (intent.resolveActivity(pm) != null) {
                return intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }
        }
        return Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }

    fun setApplicationHidden(context: Context, packageName: String, hidden: Boolean): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return try {
            if (dpm.isDeviceOwnerApp(context.packageName) || dpm.isProfileOwnerApp(context.packageName)) {
                dpm.setApplicationHidden(getDeviceAdminComponent(context), packageName, hidden)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceAdminSettingsIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun deactivateDeviceAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.removeActiveAdmin(getDeviceAdminComponent(context))
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedComponentName = ComponentName(context, AppLockAccessibilityService::class.java)

        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == expectedComponentName.packageName &&
                serviceInfo.name == expectedComponentName.className
            ) {
                return true
            }
        }
        return false
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    fun isPostNotificationsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getBatteryOptimizationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
