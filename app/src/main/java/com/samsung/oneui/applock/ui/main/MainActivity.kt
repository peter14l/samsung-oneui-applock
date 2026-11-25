package com.samsung.oneui.applock.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.theme.SamsungOneUIAppLockTheme
import com.samsung.oneui.applock.util.PermissionHelper
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var prefsManager: EncryptedPreferencesManager
    private lateinit var executor: Executor
    private var biometricPrompt: BiometricPrompt? = null
    private var isAuthenticating: Boolean = false

    private var isSelfAuthenticated by mutableStateOf(false)

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

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        prefsManager = EncryptedPreferencesManager.getInstance(this)
        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        // If back is pressed while not authenticated, cleanly exit to Home
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isSelfAuthenticated && viewModel.settings.value.isSelfLockEnabled) {
                    finish()
                } else {
                    finish()
                }
            }
        })

        setContent {
            SamsungOneUIAppLockTheme {
                val settings by viewModel.settings.collectAsState()

                if (settings.isSelfLockEnabled && !isSelfAuthenticated) {
                    AppLockSelfAuthScreen(
                        settings = settings,
                        getLockoutSeconds = { prefsManager.getRemainingLockoutSeconds() },
                        onPinVerify = { pin ->
                            if (prefsManager.verifyCustomPin(pin)) {
                                isSelfAuthenticated = true
                                prefsManager.unlockPackageSession(packageName)
                                true
                            } else {
                                false
                            }
                        },
                        onBiometricTrigger = { triggerBiometricPrompt() }
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        onEnableAccessibility = {
                            try {
                                startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(this, "Unable to open Accessibility Settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEnableUsageAccess = {
                            try {
                                startActivity(PermissionHelper.getUsageAccessSettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(this, "Unable to open Usage Access Settings", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEnableAppearOnTop = {
                            try {
                                startActivity(PermissionHelper.getAppearOnTopSettingsIntent(this))
                            } catch (e: Exception) {
                                Toast.makeText(this, "Unable to open Appear on Top Settings", Toast.LENGTH_SHORT).show()
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
                        onEnableDeviceAdmin = {
                            if (PermissionHelper.isDeviceAdminActive(this)) {
                                try {
                                    startActivity(PermissionHelper.getDeviceAdminSettingsIntent())
                                    Toast.makeText(this, "Anti-uninstall is active. Manage in Settings > Device admin apps", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    PermissionHelper.deactivateDeviceAdmin(this)
                                    viewModel.refreshPermissionState()
                                }
                            } else {
                                try {
                                    val intent = PermissionHelper.getDeviceAdminActivationIntent(this)
                                    deviceAdminLauncher.launch(intent)
                                } catch (e: Exception) {
                                    try {
                                        startActivity(PermissionHelper.getDeviceAdminActivationIntent(this))
                                    } catch (e2: Exception) {
                                        Toast.makeText(this, "Unable to request Device Administrator", Toast.LENGTH_SHORT).show()
                                    }
                                }
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()

        val settings = viewModel.settings.value
        if (settings.isSelfLockEnabled && !isSelfAuthenticated && !isAuthenticating) {
            if (settings.authType == AuthType.SYSTEM_CREDENTIAL ||
                (settings.authType == AuthType.CUSTOM_PIN && settings.useBiometricWithCustomPin)) {
                triggerBiometricPrompt()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val settings = viewModel.settings.value
        // If immediate relock or closing app, require authentication next time
        if (settings.relockPolicy == RelockPolicy.IMMEDIATE || settings.isSelfLockEnabled) {
            isSelfAuthenticated = false
            prefsManager.lockPackageSession(packageName)
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticating = false
                    isSelfAuthenticated = true
                    prefsManager.unlockPackageSession(packageName)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    Log.d(TAG, "Biometric error $errorCode: $errString")
                    val settings = viewModel.settings.value
                    if (settings.authType == AuthType.SYSTEM_CREDENTIAL) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                if (!isSelfAuthenticated) {
                                    finish()
                                }
                            }
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Biometric authentication failed")
                }
            }
        )
    }

    private fun triggerBiometricPrompt() {
        val settings = viewModel.settings.value
        val isCustomPin = settings.authType == AuthType.CUSTOM_PIN && settings.isCustomPinSet

        val authenticators = if (isCustomPin) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Lock")
            .setSubtitle("Verify identity to open App Lock")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)

        if (isCustomPin) {
            promptInfoBuilder.setNegativeButtonText("Use App PIN")
        }

        try {
            isAuthenticating = true
            biometricPrompt?.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            isAuthenticating = false
            Log.e(TAG, "Failed to launch BiometricPrompt", e)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
