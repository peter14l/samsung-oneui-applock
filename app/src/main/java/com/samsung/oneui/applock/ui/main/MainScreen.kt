package com.samsung.oneui.applock.ui.main

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.samsung.oneui.applock.data.model.AppInfo
import com.samsung.oneui.applock.data.model.AuthType
import com.samsung.oneui.applock.data.model.InterceptionEngine
import com.samsung.oneui.applock.data.model.LockSettings
import com.samsung.oneui.applock.data.model.NotificationMaskMode
import com.samsung.oneui.applock.data.model.RelockPolicy
import com.samsung.oneui.applock.ui.components.OneUICard
import com.samsung.oneui.applock.ui.components.OneUICollapsedTopBar
import com.samsung.oneui.applock.ui.components.OneUIDivider
import com.samsung.oneui.applock.ui.components.OneUIExpandingHeader
import com.samsung.oneui.applock.ui.components.OneUISearchBar
import com.samsung.oneui.applock.ui.components.OneUISwitch
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUICardRadius
import com.samsung.oneui.applock.ui.theme.OneUIGreen
import com.samsung.oneui.applock.ui.theme.OneUIIconRadius
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onEnableAccessibility: () -> Unit,
    onEnableUsageAccess: () -> Unit,
    onEnableNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onEnableBatteryOptimization: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    onOpenRestrictedSettingsHelp: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val apps by viewModel.displayedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val lockedCount by viewModel.lockedCount.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showHideAppsHelpDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val isTopBarCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }

    if (showPinDialog) {
        CustomPinDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { pin ->
                viewModel.setCustomPin(pin)
                showPinDialog = false
            }
        )
    }

    if (showDonationDialog) {
        DonationDialog(
            onDismiss = { showDonationDialog = false }
        )
    }

    if (showHideAppsHelpDialog) {
        HideAppsHelpDialog(
            onDismiss = { showHideAppsHelpDialog = false }
        )
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            OneUICollapsedTopBar(
                title = "App Lock",
                isCollapsed = isTopBarCollapsed
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // One UI Large Header
            item(key = "header") {
                val subtitle = if (settings.masterLockEnabled) {
                    if (lockedCount == 1) "1 app protected" else "$lockedCount apps protected"
                } else {
                    "Protection paused"
                }
                OneUIExpandingHeader(
                    title = "App Lock",
                    subtitle = subtitle,
                    scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                )
            }

            // Permissions & Security Onboarding
            item(key = "permissions_card") {
                PermissionSetupCard(
                    state = permissionState,
                    onEnableAccessibility = onEnableAccessibility,
                    onEnableUsageAccess = onEnableUsageAccess,
                    onEnableNotificationListener = onEnableNotificationListener,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onEnableBatteryOptimization = onEnableBatteryOptimization,
                    onEnableDeviceAdmin = onEnableDeviceAdmin,
                    onOpenRestrictedSettingsHelp = onOpenRestrictedSettingsHelp
                )
            }

            // Section 1: Security & Credentials
            item(key = "security_credentials_section") {
                Text(
                    text = "SECURITY & CREDENTIALS",
                    style = OneUITypography.titleSmall,
                    color = OneUITheme.extraColors.subtitleColor,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp)
                )

                OneUICard {
                    // Master Lock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setMasterLock(!settings.masterLockEnabled) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OneUIBlue.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = OneUIBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Master App Lock",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = if (settings.masterLockEnabled) "App protection active" else "App protection paused",
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = settings.masterLockEnabled,
                            onCheckedChange = { viewModel.setMasterLock(it) }
                        )
                    }

                    OneUIDivider(insetStart = false)

                    // Interception Engine Selector (Accessibility vs Banking-Friendly Usage Access)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(OneUITheme.extraColors.pillBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = OneUITheme.extraColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Interception Engine",
                                    style = OneUITypography.bodyLarge,
                                    color = OneUITheme.extraColors.textPrimary
                                )
                                Text(
                                    text = when (settings.interceptionEngine) {
                                        InterceptionEngine.ACCESSIBILITY -> "Accessibility (Instant overlay)"
                                        InterceptionEngine.USAGE_ACCESS -> "Banking-Friendly (Zero bank blocks)"
                                    },
                                    style = OneUITypography.bodySmall,
                                    color = OneUITheme.extraColors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OneUIFilterChip(
                                selected = settings.interceptionEngine == InterceptionEngine.ACCESSIBILITY,
                                label = "Accessibility",
                                onClick = { viewModel.setInterceptionEngine(InterceptionEngine.ACCESSIBILITY) }
                            )
                            OneUIFilterChip(
                                selected = settings.interceptionEngine == InterceptionEngine.USAGE_ACCESS,
                                label = "Banking-Friendly (Usage Access)",
                                onClick = { viewModel.setInterceptionEngine(InterceptionEngine.USAGE_ACCESS) }
                            )
                        }
                    }

                    OneUIDivider(insetStart = false)

                    // Authentication Method Selector
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(OneUITheme.extraColors.pillBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = OneUITheme.extraColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unlock Method",
                                    style = OneUITypography.bodyLarge,
                                    color = OneUITheme.extraColors.textPrimary
                                )
                                Text(
                                    text = when (settings.authType) {
                                        AuthType.SYSTEM_CREDENTIAL -> "Samsung Device Lock (Fingerprint/PIN)"
                                        AuthType.CUSTOM_PIN -> "Dedicated App PIN (Custom)"
                                    },
                                    style = OneUITypography.bodySmall,
                                    color = OneUITheme.extraColors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OneUIFilterChip(
                                selected = settings.authType == AuthType.SYSTEM_CREDENTIAL,
                                label = "Phone Screen Lock",
                                onClick = { viewModel.setAuthType(AuthType.SYSTEM_CREDENTIAL) }
                            )
                            OneUIFilterChip(
                                selected = settings.authType == AuthType.CUSTOM_PIN,
                                label = if (settings.isCustomPinSet) "Custom App PIN" else "Set Custom PIN",
                                onClick = {
                                    if (!settings.isCustomPinSet) {
                                        showPinDialog = true
                                    } else {
                                        viewModel.setAuthType(AuthType.CUSTOM_PIN)
                                    }
                                }
                            )
                        }

                        // Sub-options for Custom PIN
                        if (settings.authType == AuthType.CUSTOM_PIN && settings.isCustomPinSet) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Use Biometrics with App PIN",
                                    style = OneUITypography.bodyMedium,
                                    color = OneUITheme.extraColors.textPrimary
                                )
                                OneUISwitch(
                                    checked = settings.useBiometricWithCustomPin,
                                    onCheckedChange = { viewModel.setUseBiometricWithCustomPin(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { showPinDialog = true },
                                shape = RoundedCornerShape(OneUIPillRadius),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Change Custom PIN", fontSize = 13.sp)
                            }
                        }
                    }

                    OneUIDivider(insetStart = false)

                    // Anti-Uninstall Device Admin Protection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEnableDeviceAdmin() }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (permissionState.isDeviceAdminActive) OneUIGreen.copy(alpha = 0.12f)
                                    else OneUITheme.extraColors.pillBackground,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (permissionState.isDeviceAdminActive) OneUIGreen else OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anti-Uninstall Protection",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = if (permissionState.isDeviceAdminActive) "Active (Device Admin enabled)" else "Prevents unauthorized app uninstallation",
                                style = OneUITypography.bodySmall,
                                color = if (permissionState.isDeviceAdminActive) OneUIGreen else OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = permissionState.isDeviceAdminActive,
                            onCheckedChange = { onEnableDeviceAdmin() }
                        )
                    }

                    OneUIDivider(insetStart = false)

                    // Hide Locked Apps from Launcher & Searches
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val isDeviceOwner = com.samsung.oneui.applock.util.PermissionHelper.isDeviceOwner(context)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isDeviceOwner) {
                                    viewModel.setHideLockedApps(!settings.isHideLockedAppsEnabled)
                                } else {
                                    showHideAppsHelpDialog = true
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isDeviceOwner && settings.isHideLockedAppsEnabled) OneUIGreen.copy(alpha = 0.12f)
                                    else OneUITheme.extraColors.pillBackground,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = if (isDeviceOwner && settings.isHideLockedAppsEnabled) OneUIGreen else OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Locked Apps from Launcher & Search",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = if (isDeviceOwner && settings.isHideLockedAppsEnabled) "Locked apps hidden from home screen & search"
                                       else if (isDeviceOwner) "Auto-hide locked apps at system level"
                                       else "Tap to configure One UI Home or ADB auto-hide",
                                style = OneUITypography.bodySmall,
                                color = if (isDeviceOwner && settings.isHideLockedAppsEnabled) OneUIGreen else OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = isDeviceOwner && settings.isHideLockedAppsEnabled,
                            onCheckedChange = {
                                if (isDeviceOwner) {
                                    viewModel.setHideLockedApps(it)
                                } else {
                                    showHideAppsHelpDialog = true
                                }
                            }
                        )
                    }

                    OneUIDivider(insetStart = false)

                    // Guard Google Play Store & Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setGuardPlayStoreAndSettings(!settings.isGuardPlayStoreAndSettings) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OneUITheme.extraColors.pillBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guard Play Store & Settings",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = "Blocks opening locked apps via Play Store search or settings tampering",
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = settings.isGuardPlayStoreAndSettings,
                            onCheckedChange = { viewModel.setGuardPlayStoreAndSettings(it) }
                        )
                    }

                    OneUIDivider(insetStart = false)

                    // Self-Lock Protection (Play Store / Settings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSelfLock(!settings.isSelfLockEnabled) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OneUITheme.extraColors.pillBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Password,
                                contentDescription = null,
                                tint = OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock App Lock Itself",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = "Requires authentication if opened from Play Store or Settings",
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = settings.isSelfLockEnabled,
                            onCheckedChange = { viewModel.setSelfLock(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 2: Protection Rules
            item(key = "protection_rules_section") {
                Text(
                    text = "PROTECTION RULES",
                    style = OneUITypography.titleSmall,
                    color = OneUITheme.extraColors.subtitleColor,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                OneUICard {
                    // Hide Notification Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = settings.masterLockEnabled) {
                                viewModel.setMasterNotificationMasking(!settings.masterNotificationMasking)
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OneUITheme.extraColors.pillBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Notification Content",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = "Mask sensitive alert text on lock screen & shade",
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                        OneUISwitch(
                            checked = settings.masterNotificationMasking,
                            onCheckedChange = { viewModel.setMasterNotificationMasking(it) },
                            enabled = settings.masterLockEnabled
                        )
                    }

                    AnimatedVisibility(visible = settings.masterNotificationMasking && settings.masterLockEnabled) {
                        Column {
                            OneUIDivider(insetStart = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Masking Scope",
                                    style = OneUITypography.bodyMedium,
                                    color = OneUITheme.extraColors.textPrimary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OneUIFilterChip(
                                        selected = settings.notificationMaskMode == NotificationMaskMode.GLOBAL,
                                        label = "All Locked Apps",
                                        onClick = { viewModel.setNotificationMaskMode(NotificationMaskMode.GLOBAL) }
                                    )
                                    OneUIFilterChip(
                                        selected = settings.notificationMaskMode == NotificationMaskMode.PER_APP,
                                        label = "Per-App Selected",
                                        onClick = { viewModel.setNotificationMaskMode(NotificationMaskMode.PER_APP) }
                                    )
                                }
                            }
                        }
                    }

                    OneUIDivider(insetStart = false)

                    // Re-lock Timing Policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OneUITheme.extraColors.pillBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = OneUITheme.extraColors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Re-lock Timing",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = when (settings.relockPolicy) {
                                    RelockPolicy.SCREEN_OFF -> "Re-lock when phone screen turns off"
                                    RelockPolicy.IMMEDIATE -> "Re-lock immediately when exiting app"
                                },
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OneUIFilterChip(
                            selected = settings.relockPolicy == RelockPolicy.SCREEN_OFF,
                            label = "When screen turns off",
                            onClick = { viewModel.setRelockPolicy(RelockPolicy.SCREEN_OFF) }
                        )
                        OneUIFilterChip(
                            selected = settings.relockPolicy == RelockPolicy.IMMEDIATE,
                            label = "Immediately on exit",
                            onClick = { viewModel.setRelockPolicy(RelockPolicy.IMMEDIATE) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section: Support & Donations
            item(key = "donation_card") {
                Text(
                    text = "SUPPORT DEVELOPMENT",
                    style = OneUITypography.titleSmall,
                    color = OneUITheme.extraColors.subtitleColor,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                OneUICard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDonationDialog = true }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Donate via UPI / Samsung Pay",
                                style = OneUITypography.bodyLarge,
                                color = OneUITheme.extraColors.textPrimary
                            )
                            Text(
                                text = "peter.0419@pingpay • Tap to view QR & pay",
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(OneUIBlue.copy(alpha = 0.12f), RoundedCornerShape(OneUIPillRadius))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Donate",
                                color = OneUIBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 3: Applications
            item(key = "search_bar") {
                OneUISearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            item(key = "apps_list_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTALLED APPS (${apps.size})",
                        style = OneUITypography.titleSmall,
                        color = OneUITheme.extraColors.subtitleColor
                    )
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OneUIBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (apps.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No apps found matching '$searchQuery'" else "No launcher apps found",
                            style = OneUITypography.bodyMedium,
                            color = OneUITheme.extraColors.textSecondary
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.packageName }
                ) { index, app ->
                    val cardShape = when {
                        apps.size == 1 -> RoundedCornerShape(OneUICardRadius)
                        index == 0 -> RoundedCornerShape(
                            topStart = OneUICardRadius,
                            topEnd = OneUICardRadius,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                        index == apps.size - 1 -> RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = OneUICardRadius,
                            bottomEnd = OneUICardRadius
                        )
                        else -> RoundedCornerShape(0.dp)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = OneUITheme.extraColors.cardBackground,
                        border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AppListItem(
                                app = app,
                                settings = settings,
                                onLockToggled = { viewModel.toggleAppLock(app.packageName, it) },
                                onMaskToggled = { viewModel.toggleNotificationMask(app.packageName, it) }
                            )
                            if (index < apps.size - 1) {
                                OneUIDivider(insetStart = true)
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    settings: LockSettings,
    onLockToggled: (Boolean) -> Unit,
    onMaskToggled: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLockToggled(!app.isLocked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppSquircleIcon(
            icon = app.icon,
            iconBitmap = app.iconBitmap,
            appName = app.appName
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = OneUITypography.bodyLarge,
                color = OneUITheme.extraColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (app.isLocked) "Locked" else "Unlocked",
                style = OneUITypography.bodySmall,
                color = if (app.isLocked) OneUIBlue else OneUITheme.extraColors.subtitleColor
            )
        }

        if (app.isLocked && settings.masterNotificationMasking && settings.notificationMaskMode == NotificationMaskMode.PER_APP) {
            IconButton(
                onClick = { onMaskToggled(!app.isNotificationMasked) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (app.isNotificationMasked) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                    contentDescription = "Toggle Notification Masking",
                    tint = if (app.isNotificationMasked) OneUIBlue else OneUITheme.extraColors.subtitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        OneUISwitch(
            checked = app.isLocked,
            onCheckedChange = onLockToggled,
            enabled = settings.masterLockEnabled
        )
    }
}

@Composable
private fun AppSquircleIcon(
    icon: Drawable?,
    iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    appName: String
) {
    val bitmap = iconBitmap ?: remember(icon) {
        try {
            icon?.toBitmap(width = 132, height = 132)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(OneUIIconRadius))
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(OneUIIconRadius))
                .background(OneUITheme.extraColors.pillBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = OneUITheme.extraColors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun OneUIFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(OneUIPillRadius),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = OneUIBlue.copy(alpha = 0.15f),
            selectedLabelColor = OneUIBlue,
            containerColor = OneUITheme.extraColors.pillBackground,
            labelColor = OneUITheme.extraColors.textPrimary
        ),
        border = null
    )
}
