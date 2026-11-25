package com.samsung.oneui.applock.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.util.PermissionHelper

/**
 * Draws an immediate, hardware-accelerated opaque FLAG_SECURE shield via WindowManager
 * (TYPE_APPLICATION_OVERLAY) before LockOverlayActivity finishes launching, eliminating
 * any brief visual flash of a locked app's cached Recents snapshot or initial frame.
 */
object InstantShieldOverlay {

    private const val TAG = "InstantShieldOverlay"
    private const val AUTO_DISMISS_TIMEOUT_MS = 2500L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var shieldView: View? = null

    private val safetyDismissRunnable = Runnable {
        removeShieldInternal()
    }

    fun show(context: Context, style: LockDesignStyle) {
        if (!PermissionHelper.isAppearOnTopGranted(context)) {
            return
        }
        val appContext = context.applicationContext
        mainHandler.post {
            showInternal(appContext, style)
        }
    }

    fun dismiss() {
        mainHandler.post {
            removeShieldInternal()
        }
    }

    private fun showInternal(appContext: Context, style: LockDesignStyle) {
        try {
            mainHandler.removeCallbacks(safetyDismissRunnable)

            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val bgColor = resolveBackgroundColor(appContext, style)

            val existing = shieldView
            if (existing != null) {
                existing.setBackgroundColor(bgColor)
                mainHandler.postDelayed(safetyDismissRunnable, AUTO_DISMISS_TIMEOUT_MS)
                return
            }

            val view = View(appContext).apply {
                setBackgroundColor(bgColor)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.OPAQUE
            )

            wm.addView(view, params)
            shieldView = view
            mainHandler.postDelayed(safetyDismissRunnable, AUTO_DISMISS_TIMEOUT_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show instant shield overlay", e)
        }
    }

    private fun removeShieldInternal() {
        mainHandler.removeCallbacks(safetyDismissRunnable)
        val view = shieldView ?: return
        shieldView = null
        try {
            val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (view.isAttachedToWindow) {
                wm?.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove instant shield overlay", e)
        }
    }

    private fun resolveBackgroundColor(context: Context, style: LockDesignStyle): Int {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        return when (style) {
            LockDesignStyle.VAULT_DARK -> 0xFF0B0D12.toInt()
            LockDesignStyle.SWISS_MONO -> if (isSystemDark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            LockDesignStyle.DYNAMIC_THUMB -> if (isSystemDark) 0xFF010101.toInt() else 0xFFF6F6F8.toInt()
        }
    }
}
