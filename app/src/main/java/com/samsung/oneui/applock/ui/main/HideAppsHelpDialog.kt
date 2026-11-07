package com.samsung.oneui.applock.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUICardRadius
import com.samsung.oneui.applock.ui.theme.OneUIGreen
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography
import com.samsung.oneui.applock.util.PermissionHelper

@Composable
fun HideAppsHelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDeviceOwner = PermissionHelper.isDeviceOwner(context)
    val adbCommand = "adb shell dpm set-device-owner com.samsung.oneui.applock/.receiver.AppLockDeviceAdminReceiver"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(OneUICardRadius),
            color = OneUITheme.extraColors.cardBackground,
            border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(OneUIBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = OneUIBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hide Apps on Samsung",
                    style = OneUITypography.titleLarge,
                    color = OneUITheme.extraColors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Android security prevents third-party apps from modifying your home screen without permission. Choose a method below:",
                    style = OneUITypography.bodySmall,
                    color = OneUITheme.extraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Method 1: Samsung One UI Home Native Hide
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OneUITheme.extraColors.pillBackground,
                    border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = null,
                                tint = OneUIBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Method 1: One UI Home (No PC)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITheme.extraColors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Samsung One UI includes a native hide apps feature in your Home Screen settings. Tap below, select 'Hide apps on Home and Apps screens', and pick your apps.",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    context.startActivity(PermissionHelper.getSamsungHomeSettingsIntent(context))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Open Home Screen settings manually by pinching home screen", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(OneUIPillRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("Open Home Screen Settings", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Method 2: Automatic OS-Level Hiding via ADB
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = OneUITheme.extraColors.pillBackground,
                    border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = if (isDeviceOwner) OneUIGreen else OneUIBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Method 2: Auto-Hide (ADB)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUITheme.extraColors.textPrimary
                                )
                            }

                            if (isDeviceOwner) {
                                Box(
                                    modifier = Modifier
                                        .background(OneUIGreen.copy(alpha = 0.15f), RoundedCornerShape(OneUIPillRadius))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Active", color = OneUIGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "To let AppLock hide and unhide apps automatically in the background, run this one-time command from your PC with USB debugging enabled:",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OneUITheme.extraColors.cardBackground,
                            border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = adbCommand,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OneUITheme.extraColors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ADB Command", adbCommand)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(OneUIPillRadius),
                                    colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(OneUIPillRadius),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Got It", color = OneUITheme.extraColors.textPrimary)
                }
            }
        }
    }
}
