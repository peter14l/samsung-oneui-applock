package com.samsung.oneui.applock.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.samsung.oneui.applock.R
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.util.PermissionHelper

/**
 * Samsung One UI Quick Panel Tile for App Lock.
 * Allows instant 1-tap pause/resume of App Lock directly from the status bar quick settings.
 */
class AppLockTileService : TileService() {

    private lateinit var prefsManager: EncryptedPreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefsManager = EncryptedPreferencesManager.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        val isMasterLock = prefsManager.settingsFlow.value.masterLockEnabled
        updateTileState(isMasterLock)
    }

    override fun onClick() {
        super.onClick()
        val settings = prefsManager.settingsFlow.value
        val currentEnabled = settings.masterLockEnabled
        val newEnabled = !currentEnabled
        prefsManager.setMasterLockEnabled(newEnabled)

        if (settings.interceptionEngine == InterceptionEngine.USAGE_ACCESS) {
            if (newEnabled) {
                AppLockUsageService.start(this)
            } else {
                AppLockUsageService.stop(this)
            }
        } else {
            // Accessibility Engine
            if (!newEnabled) {
                // Turning OFF: Check if ADB WRITE_SECURE_SETTINGS is present or call disableSelf
                if (PermissionHelper.hasWriteSecureSettings(this)) {
                    PermissionHelper.setAccessibilityServiceViaSecureSettings(this, false)
                } else {
                    AppLockAccessibilityService.disableService()
                }
            } else {
                // Turning ON: If ADB WRITE_SECURE_SETTINGS is present, re-enable
                if (PermissionHelper.hasWriteSecureSettings(this)) {
                    PermissionHelper.setAccessibilityServiceViaSecureSettings(this, true)
                } else if (!PermissionHelper.isAccessibilityServiceEnabled(this)) {
                    val intent = PermissionHelper.getAccessibilitySettingsIntent()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startActivityAndCollapse(
                            android.app.PendingIntent.getActivity(
                                this,
                                0,
                                intent,
                                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
            }
        }

        updateTileState(newEnabled)
        Log.i(TAG, "Quick Settings Tile clicked: Master lock set to $newEnabled (Engine: ${settings.interceptionEngine})")
    }

    private fun updateTileState(isActive: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_lock_badge)
        tile.label = getString(R.string.quick_tile_label)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) getString(R.string.quick_tile_active) else getString(R.string.quick_tile_paused)
        }
        tile.updateTile()
    }

    companion object {
        private const val TAG = "AppLockTileService"
    }
}
