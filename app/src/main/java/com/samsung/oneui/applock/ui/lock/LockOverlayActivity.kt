package com.samsung.oneui.applock.ui.lock

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUIDarkBackground
import com.samsung.oneui.applock.ui.theme.OneUILightBackground
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.SamsungOneUIAppLockTheme
import java.util.concurrent.Executor

class LockOverlayActivity : FragmentActivity() {

    private lateinit var prefsManager: EncryptedPreferencesManager
    private var targetPackage: String? = null
    private var targetAppName: String = "Application"
    private var targetAppIcon: Drawable? = null

    private lateinit var executor: Executor
    private var biometricPrompt: BiometricPrompt? = null
    private var isAuthenticating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots, recent tasks preview leakage, and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        prefsManager = EncryptedPreferencesManager.getInstance(this)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage.isNullOrEmpty()) {
            finish()
            return
        }

        loadAppMetadata(targetPackage!!)

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        // Intercept back button to navigate to device home screen (cannot bypass lock)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHomeScreen()
            }
        })

        setContent {
            SamsungOneUIAppLockTheme {
                val isDark = isSystemInDarkTheme()

                // Completely 100% opaque solid background to ensure 0% underlying app content leaks through
                val solidBackground = if (isDark) OneUIDarkBackground else OneUILightBackground

                // Ensure status and nav bars are fully opaque solid
                val window = this.window
                window.statusBarColor = if (isDark) 0xFF010101.toInt() else 0xFFF6F6F8.toInt()
                window.navigationBarColor = if (isDark) 0xFF010101.toInt() else 0xFFF6F6F8.toInt()
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }

                LockOverlayContent(
                    appName = targetAppName,
                    appIcon = targetAppIcon,
                    backgroundColor = solidBackground,
                    isDark = isDark,
                    onUnlockClick = { triggerBiometricPrompt() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticating) {
            triggerBiometricPrompt()
        }
    }

    private fun loadAppMetadata(pkg: String) {
        try {
            val pm = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
            targetAppName = pm.getApplicationLabel(appInfo).toString()
            targetAppIcon = pm.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            targetAppName = pkg
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
                    targetPackage?.let { pkg ->
                        prefsManager.unlockPackageSession(pkg)
                    }
                    finishWithZeroAnimation()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    Log.d(TAG, "Biometric authentication error $errorCode: $errString")

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            navigateToHomeScreen()
                        }
                        // Lockout or temporary errors: stay on overlay, user can press Unlock button
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Biometric authentication failed (mismatch)")
                }
            }
        )
    }

    private fun triggerBiometricPrompt() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(targetAppName)
            .setSubtitle("App Locked")
            .setDescription("Verify your identity with Samsung Biometrics or PIN to continue")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        try {
            isAuthenticating = true
            biometricPrompt?.authenticate(promptInfo)
        } catch (e: Exception) {
            isAuthenticating = false
            Log.e(TAG, "Failed to authenticate biometric prompt", e)
        }
    }

    private fun navigateToHomeScreen() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start home intent", e)
        } finally {
            finishWithZeroAnimation()
        }
    }

    private fun finishWithZeroAnimation() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        private const val TAG = "LockOverlayActivity"
    }
}

@Composable
private fun LockOverlayContent(
    appName: String,
    appIcon: Drawable?,
    backgroundColor: Color,
    isDark: Boolean,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Target App Icon
            if (appIcon != null) {
                val bitmap = remember(appIcon) {
                    try {
                        appIcon.toBitmap(width = 160, height = 160).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = appName,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                } else {
                    DefaultLockIconBadge()
                }
            } else {
                DefaultLockIconBadge()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = appName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF5F5F7) else Color(0xFF111111),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Protected by Samsung One UI App Lock",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF98989D) else Color(0xFF767676),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Subtle One UI Pill Unlock Button
            Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(OneUIPillRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUIBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Unlock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DefaultLockIconBadge() {
    Surface(
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = OneUIBlue.copy(alpha = 0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = OneUIBlue,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
