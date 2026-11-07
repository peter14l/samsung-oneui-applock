package com.samsung.oneui.applock.ui.lock

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUIRed
import kotlin.math.roundToInt

@Composable
fun OneUIPinKeypad(
    pinLength: Int,
    currentPinLength: Int,
    isError: Boolean,
    showBiometricKey: Boolean,
    isDark: Boolean,
    onDigitPress: (String) -> Unit,
    onDeletePress: () -> Unit,
    onBiometricPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            for (i in 0..2) {
                shakeOffset.animateTo(24f, tween(40))
                shakeOffset.animateTo(-24f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentPinLength
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isError -> OneUIRed
                        isFilled -> OneUIBlue
                        isDark -> Color(0xFF2C2C2E)
                        else -> Color(0xFFD1D1D6)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(dotColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 3x4 Keypad Layout
        val keyBgColor = if (isDark) Color(0xFF1E1E20) else Color(0xFFEAEAEE)
        val keyTextColor = if (isDark) Color(0xFFF5F5F7) else Color(0xFF111111)
        val keySubTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF767676)

        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("BIO" to "", "0" to "+", "DEL" to "")
        )

        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (key in row) {
                    when (key.first) {
                        "BIO" -> {
                            if (showBiometricKey) {
                                KeypadIconButton(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = "Biometric",
                                            tint = OneUIBlue,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    bgColor = OneUIBlue.copy(alpha = 0.12f),
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onBiometricPress()
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(76.dp))
                            }
                        }
                        "DEL" -> {
                            KeypadIconButton(
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "Backspace",
                                        tint = keyTextColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                bgColor = Color.Transparent,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onDeletePress()
                                }
                            )
                        }
                        else -> {
                            KeypadDigitButton(
                                digit = key.first,
                                letters = key.second,
                                bgColor = keyBgColor,
                                textColor = keyTextColor,
                                subTextColor = keySubTextColor,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onDigitPress(key.first)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    letters: String,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                lineHeight = 28.sp
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = subTextColor
                )
            }
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: @Composable () -> Unit,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
