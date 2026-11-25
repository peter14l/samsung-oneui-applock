package com.samsung.oneui.applock.ui.main

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.LockDesignStyle
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.ui.lock.OneUIPinKeypad
import com.samsung.oneui.applock.ui.lock.SecurityStatusPill
import com.samsung.oneui.applock.ui.lock.rememberLockStylePalette
import com.samsung.oneui.applock.ui.lock.resolveLockThemeFlags
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.SamsungOneUIAppLockTheme
import com.samsung.oneui.applock.util.PermissionHelper
import kotlinx.coroutines.delay

@Composable
fun AppLockSelfAuthScreen(
    settings: LockSettings,
    getLockoutSeconds: () -> Long = { 0L },
    onPinVerify: (String) -> Boolean,
    onBiometricTrigger: () -> Unit
) {
    val designStyle = settings.lockDesignStyle
    val isSystemDark = isSystemInDarkTheme()
    val (isDark, useDynamic) = resolveLockThemeFlags(designStyle, isSystemDark)

    SamsungOneUIAppLockTheme(darkTheme = isDark, dynamicColor = useDynamic) {
        val palette = rememberLockStylePalette(designStyle, isDark)
        val context = LocalContext.current
        val biometricStatus = remember(context) { PermissionHelper.getBiometricStatusLabel(context) }

        var pinInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        val pinLength = settings.customPinLength
        var remainingLockout by remember { mutableStateOf(getLockoutSeconds()) }

        val isCustomPinMode = settings.authType == AuthType.CUSTOM_PIN && settings.isCustomPinSet

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
                val success = onPinVerify(pinInput)
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
            isCustomPinMode -> "Enter your App PIN to access settings"
            else -> "Protected • $biometricStatus"
        }

        val badgeModifier = when (designStyle) {
            LockDesignStyle.VAULT_DARK -> Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.accent.copy(alpha = 0.12f))
                .border(1.5.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            LockDesignStyle.SWISS_MONO -> Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, palette.sheetBorder ?: palette.accent, RoundedCornerShape(20.dp))
            LockDesignStyle.DYNAMIC_THUMB -> Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.accent.copy(alpha = 0.12f))
        }

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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = badgeModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "App Lock",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                SecurityStatusPill(
                    text = statusText,
                    isError = isError || remainingLockout > 0,
                    palette = palette
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = palette.sheetBackground,
                border = palette.sheetBorder?.let { BorderStroke(1.dp, it) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCustomPinMode) {
                        OneUIPinKeypad(
                            pinLength = pinLength,
                            currentPinLength = pinInput.length,
                            isError = isError,
                            showBiometricKey = settings.useBiometricWithCustomPin,
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
                            onBiometricPress = onBiometricTrigger
                        )
                    } else {
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
                                onClick = onBiometricTrigger,
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
                                onClick = onBiometricTrigger,
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
    }
}
