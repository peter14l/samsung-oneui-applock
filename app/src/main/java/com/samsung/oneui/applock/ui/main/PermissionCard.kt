package com.samsung.oneui.applock.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.ui.components.OneUICard
import com.samsung.oneui.applock.ui.components.OneUIDivider
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUIGreen
import com.samsung.oneui.applock.ui.theme.OneUIOrange
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun PermissionSetupCard(
    state: PermissionState,
    onEnableAccessibility: () -> Unit,
    onEnableUsageAccess: () -> Unit,
    onEnableAppearOnTop: () -> Unit,
    onEnableNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onEnableBatteryOptimization: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    onOpenRestrictedSettingsHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsAttention = !state.allRequiredGranted || !state.isBatteryOptimizationIgnored || !state.isDeviceAdminActive

    AnimatedVisibility(
        visible = needsAttention,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "REQUIRED PERMISSIONS & SECURITY",
                style = OneUITypography.titleSmall,
                color = OneUITheme.extraColors.subtitleColor,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            OneUICard {
                // Header banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OneUIOrange.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = OneUIOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Setup Required",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Text(
                            text = "Grant permissions below to enable app locking & anti-uninstall protection",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                    }
                }

                OneUIDivider(insetStart = false)

                // 1. Interception Engine Permission
                if (state.interceptionEngine == InterceptionEngine.ACCESSIBILITY) {
                    PermissionItem(
                        title = "App Interception Service",
                        subtitle = "Required to detect when locked apps open",
                        icon = Icons.Rounded.AccessibilityNew,
                        isGranted = state.isAccessibilityEnabled,
                        onClick = onEnableAccessibility
                    )
                } else {
                    PermissionItem(
                        title = "Usage Access (Banking-Friendly)",
                        subtitle = "Required to detect apps without triggering bank blocks",
                        icon = Icons.Rounded.Security,
                        isGranted = state.isUsageAccessGranted,
                        onClick = onEnableUsageAccess
                    )

                    OneUIDivider(insetStart = true)

                    PermissionItem(
                        title = "Appear on Top",
                        subtitle = "Required to display lock overlay over opened apps",
                        icon = Icons.Rounded.Layers,
                        isGranted = state.isAppearOnTopGranted,
                        onClick = onEnableAppearOnTop
                    )
                }

                OneUIDivider(insetStart = true)

                // 2. Notification Listener
                PermissionItem(
                    title = "Notification Guard",
                    subtitle = "Required to mask sensitive notification text",
                    icon = Icons.Rounded.Notifications,
                    isGranted = state.isNotificationListenerEnabled,
                    onClick = onEnableNotificationListener
                )

                OneUIDivider(insetStart = true)

                // 3. Post Notifications (Android 13+)
                PermissionItem(
                    title = "Alert Notifications",
                    subtitle = "Required to show masked notifications",
                    icon = Icons.Rounded.Notifications,
                    isGranted = state.isPostNotificationsGranted,
                    onClick = onRequestPostNotifications
                )

                OneUIDivider(insetStart = true)

                // 4. Device Admin (Uninstall Protection)
                PermissionItem(
                    title = "Anti-Uninstall Protection",
                    subtitle = "Device Admin prevents unauthorized app removal",
                    icon = Icons.Rounded.Security,
                    isGranted = state.isDeviceAdminActive,
                    onClick = onEnableDeviceAdmin
                )

                OneUIDivider(insetStart = true)

                // 5. Battery Optimization
                PermissionItem(
                    title = "Background Protection",
                    subtitle = "Set Battery Usage to Unrestricted for 24/7 reliability",
                    icon = Icons.Rounded.BatteryAlert,
                    isGranted = state.isBatteryOptimizationIgnored,
                    onClick = onEnableBatteryOptimization
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Restricted Settings Hint for Android 13-15+
            Text(
                text = "Tip (Android 13–15+): If Usage Access, Appear on Top, or Accessibility is greyed out (Restricted Setting), open App Info > 3-dots > 'Allow restricted settings'. Also set Battery to Unrestricted. Tap here to open App Info.",
                style = OneUITypography.bodySmall,
                color = OneUITheme.extraColors.accentBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenRestrictedSettingsHelp() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isGranted) OneUIGreen.copy(alpha = 0.12f) else OneUITheme.extraColors.pillBackground,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                contentDescription = null,
                tint = if (isGranted) OneUIGreen else OneUITheme.extraColors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OneUITypography.bodyLarge,
                color = OneUITheme.extraColors.textPrimary
            )
            Text(
                text = subtitle,
                style = OneUITypography.bodySmall,
                color = OneUITheme.extraColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!isGranted) {
            Box(
                modifier = Modifier
                    .background(OneUIBlue, RoundedCornerShape(OneUIPillRadius))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Enable",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = "Active",
                color = OneUIGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
