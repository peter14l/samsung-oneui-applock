package com.samsung.oneui.applock.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.File

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val apkPath = intent.getStringExtra(EXTRA_APK_PATH)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch install confirmation intent", e)
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "In-app update installed successfully")
                apkPath?.let { File(it).delete() }
            }
            else -> {
                Log.e(TAG, "Update installation failed (status=$status): $message")
                apkPath?.let { File(it).delete() }
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                    Toast.makeText(
                        context,
                        "Update install failed: ${message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.samsung.oneui.applock.action.INSTALL_STATUS"
        const val EXTRA_APK_PATH = "extra_apk_path"
        private const val TAG = "UpdateInstallReceiver"
    }
}
