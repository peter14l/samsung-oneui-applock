package com.samsung.oneui.applock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Boot or Package Replaced received: Warming up EncryptedPreferences")
            EncryptedPreferencesManager.getInstance(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
