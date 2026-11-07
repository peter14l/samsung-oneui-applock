package com.samsung.oneui.applock.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.samsung.oneui.applock.ui.theme.SamsungOneUIAppLockTheme
import com.samsung.oneui.applock.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val postNotificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissionState()
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission is needed to show masked notifications",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SamsungOneUIAppLockTheme {
                MainScreen(
                    viewModel = viewModel,
                    onEnableAccessibility = {
                        try {
                            startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to open Accessibility Settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEnableNotificationListener = {
                        try {
                            startActivity(PermissionHelper.getNotificationListenerSettingsIntent())
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to open Notification Access Settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onEnableBatteryOptimization = {
                        try {
                            startActivity(PermissionHelper.getBatteryOptimizationSettingsIntent(this))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to request battery optimization ignore", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenRestrictedSettingsHelp = {
                        try {
                            startActivity(PermissionHelper.getAppDetailsSettingsIntent(this))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Unable to open App Info", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()
    }
}
