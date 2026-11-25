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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.data.pref.EncryptedPreferencesManager
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUIDarkBackground
import com.samsung.oneui.applock.ui.theme.OneUILightBackground
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.SamsungOneUIAppLockTheme
import com.samsung.oneui.applock.util.PermissionHelper
import kotlinx.coroutines.delay
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

        // Intercept back button to navigate cleanly to device home screen (cannot bypass lock)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHomeScreen()
            }
        })

        setContent {
            val settings = prefsManager.settingsFlow.value
            val designStyle = settings.lockDesignStyle
            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (designStyle) {
                LockDesignStyle.VAULT_DARK -> true
                LockDesignStyle.SWISS_MONO -> isSystemDark
                LockDesignStyle.DYNAMIC_THUMB -> isSystemDark
            }
            val useDynamic = designStyle == LockDesignStyle.DYNAMIC_THUMB

            SamsungOneUIAppLockTheme(darkTheme = isDark, dynamicColor = useDynamic) {
                val palette = rememberLockStylePalette(designStyle, isDark)
                val biometricStatus = remember { PermissionHelper.getBiometricStatusLabel(this) }

                // Ensure status and nav bars are fully opaque solid
                val window = this.window
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = palette.background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = palette.sheetBackground.toArgb()
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }

                if (settings.authType == AuthType.CUSTOM_PIN && settings.isCustomPinSet) {
                    CustomPinOverlayContent(
                        appName = targetAppName,
                        appIcon = targetAppIcon,
                        designStyle = designStyle,
                        palette = palette,
                        isDark = isDark,
                        pinLength = settings.customPinLength,
                        showBiometricKey = settings.useBiometricWithCustomPin,
                        biometricStatus = biometricStatus,
                        getLockoutSeconds = { prefsManager.getRemainingLockoutSeconds() },
                        onPinEntered = { pin ->
                            if (prefsManager.verifyCustomPin(pin)) {
                                targetPackage?.let { prefsManager.unlockPackageSession(it) }
                                finishWithZeroAnimation()
                                true
                            } else {
                                false
                            }
                        },
                        onBiometricPress = {
                            if (settings.useBiometricWithCustomPin) {
                                triggerBiometricPrompt()
                            }
                        }
                    )
                } else {
                    SystemCredentialOverlayContent(
                        appName = targetAppName,
                        appIcon = targetAppIcon,
                        designStyle = designStyle,
                        palette = palette,
                        biometricStatus = biometricStatus,
                        onUnlockClick = { triggerBiometricPrompt() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val settings = prefsManager.settingsFlow.value
        if (!isAuthenticating) {
            // Trigger biometrics in SYSTEM_CREDENTIAL mode, or in CUSTOM_PIN mode if biometrics enabled
            if (settings.authType == AuthType.SYSTEM_CREDENTIAL ||
                (settings.authType == AuthType.CUSTOM_PIN && settings.useBiometricWithCustomPin)) {
                triggerBiometricPrompt()
            }
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

                    val settings = prefsManager.settingsFlow.value
                    if (settings.authType == AuthType.SYSTEM_CREDENTIAL) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                navigateToHomeScreen()
                            }
                        }
                    }
                    // For CUSTOM_PIN, if biometric is canceled or fails, remain on PIN keypad
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Biometric authentication failed")
                }
            }
        )
    }

    private fun triggerBiometricPrompt() {
        val settings = prefsManager.settingsFlow.value
        val isCustomPinMode = settings.authType == AuthType.CUSTOM_PIN

        val authenticators = if (isCustomPinMode) {
            // In custom PIN mode, only authenticate with biometric sensor, not device PIN
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        } else {
            // In system credential mode, allow device PIN / Pattern / Password fallback
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(targetAppName)
            .setSubtitle("App Locked")
            .setDescription("Verify identity to continue")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)

        if (isCustomPinMode) {
            promptInfoBuilder.setNegativeButtonText("Use App PIN")
        }

        try {
            isAuthenticating = true
            biometricPrompt?.authenticate(promptInfoBuilder.build())
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

private data class LockStylePalette(
    val background: Color,
    val sheetBackground: Color,
    val sheetBorder: Color?,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val keyBg: Color,
    val keyBorder: Color?,
    val dotInactive: Color
)

@Composable
private fun rememberLockStylePalette(style: LockDesignStyle, isDark: Boolean): LockStylePalette {
    val m3 = MaterialTheme.colorScheme
    return when (style) {
        LockDesignStyle.DYNAMIC_THUMB -> LockStylePalette(
            background = m3.background,
            sheetBackground = m3.surfaceContainerLow,
            sheetBorder = null,
            textPrimary = m3.onBackground,
            textSecondary = m3.onSurfaceVariant,
            accent = m3.primary,
            keyBg = m3.surfaceContainerHigh,
            keyBorder = null,
            dotInactive = m3.outlineVariant
        )
        LockDesignStyle.VAULT_DARK -> LockStylePalette(
            background = Color(0xFF0B0D12),
            sheetBackground = Color(0xFF131722),
            sheetBorder = Color(0xFF232A3B),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF5E6AD2),
            keyBg = Color(0xFF1B2030),
            keyBorder = Color(0xFF2A3247),
            dotInactive = Color(0xFF2B3245)
        )
        LockDesignStyle.SWISS_MONO -> {
            val bg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
            val fg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
            val muted = if (isDark) Color(0xFFA0A0A0) else Color(0xFF5C5C5C)
            val border = if (isDark) Color(0xFF2E2E2E) else Color(0xFFD4D4D4)
            LockStylePalette(
                background = bg,
                sheetBackground = bg,
                sheetBorder = border,
                textPrimary = fg,
                textSecondary = muted,
                accent = fg,
                keyBg = Color.Transparent,
                keyBorder = border,
                dotInactive = border
            )
        }
    }
}

@Composable
private fun CustomPinOverlayContent(
    appName: String,
    appIcon: Drawable?,
    designStyle: LockDesignStyle,
    palette: LockStylePalette,
    isDark: Boolean,
    pinLength: Int,
    showBiometricKey: Boolean,
    biometricStatus: String,
    getLockoutSeconds: () -> Long,
    onPinEntered: (String) -> Boolean,
    onBiometricPress: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var remainingLockout by remember { mutableStateOf(getLockoutSeconds()) }

    LaunchedEffect(Unit) {
        while (true) {
            val seconds = getLockoutSeconds()
            remainingLockout = seconds
            if (seconds > 0) {
                delay(1000)
            } else {
                delay(500)
            }
        }
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == pinLength && remainingLockout <= 0) {
            val success = onPinEntered(pinInput)
            if (!success) {
                isError = true
                delay(300)
                pinInput = ""
                delay(300)
                isError = false
                remainingLockout = getLockoutSeconds()
            }
        }
    }

    val statusText = when {
        remainingLockout > 0 -> "Too many attempts. Try again in ${remainingLockout}s"
        isError -> "Incorrect PIN. Try again."
        showBiometricKey -> "Enter PIN or use Face / Fingerprint"
        else -> "Enter App PIN"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Shield Header
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppSquircleIconBadge(
                icon = appIcon,
                name = appName,
                designStyle = designStyle,
                accentColor = palette.accent,
                ringColor = palette.sheetBorder ?: palette.accent.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = appName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            SecurityStatusPill(
                text = statusText,
                isError = isError || remainingLockout > 0,
                palette = palette
            )
        }

        // Bottom-Anchored Thumb-Friendly Keypad Sheet
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.sheetBackground,
            border = palette.sheetBorder?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OneUIPinKeypad(
                    pinLength = pinLength,
                    currentPinLength = pinInput.length,
                    isError = isError,
                    showBiometricKey = showBiometricKey,
                    isDark = isDark,
                    accentColor = palette.accent,
                    keyBgColor = palette.keyBg,
                    keyBorderColor = palette.keyBorder,
                    keyTextColor = palette.textPrimary,
                    keySubTextColor = palette.textSecondary,
                    dotInactiveColor = palette.dotInactive,
                    onDigitPress = { digit ->
                        if (pinInput.length < pinLength && !isError && remainingLockout <= 0) {
                            pinInput += digit
                        }
                    },
                    onDeletePress = {
                        if (pinInput.isNotEmpty() && !isError && remainingLockout <= 0) {
                            pinInput = pinInput.dropLast(1)
                        }
                    },
                    onBiometricPress = onBiometricPress
                )

                if (showBiometricKey) {
                    Text(
                        text = biometricStatus,
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemCredentialOverlayContent(
    appName: String,
    appIcon: Drawable?,
    designStyle: LockDesignStyle,
    palette: LockStylePalette,
    biometricStatus: String,
    onUnlockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppSquircleIconBadge(
                icon = appIcon,
                name = appName,
                designStyle = designStyle,
                accentColor = palette.accent,
                ringColor = palette.sheetBorder ?: palette.accent.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = appName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            SecurityStatusPill(
                text = "App Protected • $biometricStatus",
                isError = false,
                palette = palette
            )
        }

        // Bottom-Anchored Action Sheet
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = palette.sheetBackground,
            border = palette.sheetBorder?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = "Face Unlock",
                        tint = palette.accent,
                        modifier = Modifier.size(26.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = "Fingerprint Unlock",
                        tint = palette.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (designStyle == LockDesignStyle.SWISS_MONO) {
                    OutlinedButton(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(OneUIPillRadius),
                        border = BorderStroke(1.dp, palette.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with Face / Fingerprint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                } else {
                    Button(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(OneUIPillRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with Face / Fingerprint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusPill(
    text: String,
    isError: Boolean,
    palette: LockStylePalette
) {
    val errorRed = Color(0xFFFF3B30)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isError) errorRed.copy(alpha = 0.14f) else palette.accent.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) errorRed.copy(alpha = 0.4f) else palette.accent.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (isError) errorRed else palette.accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isError) errorRed else palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppSquircleIconBadge(
    icon: Drawable?,
    name: String,
    designStyle: LockDesignStyle,
    accentColor: Color,
    ringColor: Color
) {
    val ringModifier = when (designStyle) {
        LockDesignStyle.VAULT_DARK -> Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.5.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
        LockDesignStyle.SWISS_MONO -> Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, ringColor, RoundedCornerShape(22.dp))
        LockDesignStyle.DYNAMIC_THUMB -> Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(accentColor.copy(alpha = 0.10f))
    }

    Box(
        modifier = ringModifier,
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            val bitmap = remember(icon) {
                try {
                    icon.toBitmap(width = 160, height = 160).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                return
            }
        }

        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
