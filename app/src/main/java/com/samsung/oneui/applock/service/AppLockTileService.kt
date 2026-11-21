package com.samsung.oneui.applock.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.samsung.oneui.applock.R
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager

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
        val currentEnabled = prefsManager.settingsFlow.value.masterLockEnabled
        val newEnabled = !currentEnabled
        prefsManager.setMasterLockEnabled(newEnabled)
        updateTileState(newEnabled)
        Log.i(TAG, "Quick Settings Tile clicked: Master lock set to $newEnabled")
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
