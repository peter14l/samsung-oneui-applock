package com.samsung.oneui.applock.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUICardRadius
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUIRed
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun CustomPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(OneUICardRadius),
            color = OneUITheme.extraColors.cardBackground,
            border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Custom App PIN",
                    style = OneUITypography.titleLarge,
                    color = OneUITheme.extraColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This PIN will be used exclusively for locked apps instead of your device lock code.",
                    style = OneUITypography.bodySmall,
                    color = OneUITheme.extraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter 4-6 digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUITheme.extraColors.cardBorder,
                        focusedTextColor = OneUITheme.extraColors.textPrimary,
                        unfocusedTextColor = OneUITheme.extraColors.textPrimary,
                        focusedLabelColor = OneUIBlue,
                        unfocusedLabelColor = OneUITheme.extraColors.textSecondary,
                        cursorColor = OneUIBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUITheme.extraColors.cardBorder,
                        focusedTextColor = OneUITheme.extraColors.textPrimary,
                        unfocusedTextColor = OneUITheme.extraColors.textPrimary,
                        focusedLabelColor = OneUIBlue,
                        unfocusedLabelColor = OneUITheme.extraColors.textSecondary,
                        cursorColor = OneUIBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = OneUIRed,
                        style = OneUITypography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(OneUIPillRadius)
                    ) {
                        Text("Cancel", color = OneUITheme.extraColors.textPrimary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            when {
                                pin.length < 4 -> errorMessage = "PIN must be at least 4 digits"
                                pin != confirmPin -> errorMessage = "PINs do not match"
                                else -> onPinSet(pin)
                            }
                        },
                        shape = RoundedCornerShape(OneUIPillRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                    ) {
                        Text("Save PIN", color = Color.White)
                    }
                }
            }
        }
    }
}
