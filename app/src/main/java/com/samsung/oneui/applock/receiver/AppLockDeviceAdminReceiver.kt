package com.samsung.oneui.applock.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.samsung.oneui.applock.R

class AppLockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin enabled: Uninstall protection active")
        Toast.makeText(context, "App Lock uninstall protection enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "Device Admin disable requested")
        return context.getString(R.string.device_admin_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device Admin disabled")
        Toast.makeText(context, "App Lock uninstall protection disabled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
}
