package com.samsung.oneui.applock.ui.main

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.ui.lock.OneUIPinKeypad
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUIDarkBackground
import com.samsung.oneui.applock.ui.theme.OneUILightBackground
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import kotlinx.coroutines.delay

@Composable
fun AppLockSelfAuthScreen(
    settings: LockSettings,
    onPinVerify: (String) -> Boolean,
    onBiometricTrigger: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) OneUIDarkBackground else OneUILightBackground

    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val pinLength = 4

    val isCustomPinMode = settings.authType == AuthType.CUSTOM_PIN && settings.isCustomPinSet

    LaunchedEffect(pinInput) {
        if (pinInput.length == pinLength) {
            val success = onPinVerify(pinInput)
            if (!success) {
                isError = true
                delay(300)
                pinInput = ""
                delay(300)
                isError = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Samsung One UI Lock Badge
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = OneUIBlue.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = OneUIBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "App Lock",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF5F5F7) else Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isError) "Incorrect PIN. Try again."
                else if (isCustomPinMode) "Enter your App PIN to access settings"
                else "Verify your identity to access settings",
                fontSize = 14.sp,
                fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isError) Color(0xFFFF3B30) else if (isDark) Color(0xFF98989D) else Color(0xFF767676),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isCustomPinMode) {
                OneUIPinKeypad(
                    pinLength = pinLength,
                    currentPinLength = pinInput.length,
                    isError = isError,
                    showBiometricKey = false,
                    isDark = isDark,
                    onDigitPress = { digit ->
                        if (pinInput.length < pinLength && !isError) {
                            pinInput += digit
                        }
                    },
                    onDeletePress = {
                        if (pinInput.isNotEmpty() && !isError) {
                            pinInput = pinInput.dropLast(1)
                        }
                    },
                    onBiometricPress = onBiometricTrigger
                )
            } else {
                Button(
                    onClick = onBiometricTrigger,
                    shape = RoundedCornerShape(OneUIPillRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                    modifier = Modifier
                        .height(50.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Unlock Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
