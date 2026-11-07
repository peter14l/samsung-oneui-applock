package com.samsung.oneui.applock.ui.main

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.samsung.oneui.applock.ui.theme.OneUIIconRadius
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onEnableAccessibility: () -> Unit,
    onEnableNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onEnableBatteryOptimization: () -> Unit,
    onOpenRestrictedSettingsHelp: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val apps by viewModel.displayedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val lockedCount by viewModel.lockedCount.collectAsState()

    val listState = rememberLazyListState()

    // Determine collapsing state based on scroll offset
    val isTopBarCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }

    Scaffold(
        containerColor = OneUITheme.extraColors.cardBackground.copy(alpha = 0.05f),
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
            // Samsung One UI Large Expanding Header
            item(key = "header") {
                val subtitle = if (settings.masterLockEnabled) {
                    if (lockedCount == 1) "1 application locked" else "$lockedCount applications locked"
                } else {
                    "Protection paused"
                }
                OneUIExpandingHeader(
                    title = "App Lock",
                    subtitle = subtitle,
                    scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                )
            }

            // Permissions Card (if missing permissions)
            item(key = "permissions_card") {
                PermissionSetupCard(
                    state = permissionState,
                    onEnableAccessibility = onEnableAccessibility,
                    onEnableNotificationListener = onEnableNotificationListener,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onEnableBatteryOptimization = onEnableBatteryOptimization,
                    onOpenRestrictedSettingsHelp = onOpenRestrictedSettingsHelp
                )
            }

            // Global Controls Grouped Card
            item(key = "global_controls") {
                Text(
                    text = "GLOBAL PROTECTION",
                    style = OneUITypography.titleSmall,
                    color = OneUITheme.extraColors.subtitleColor,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 6.dp)
                )

                OneUICard {
                    // Master Lock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                text = if (settings.masterLockEnabled) "App locking is active" else "App locking is paused",
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

                    // Master Notification Masking Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            // Notification Masking Scope Selector
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

                    // Re-lock Timing Policy Selector
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

            // Search Bar
            item(key = "search_bar") {
                OneUISearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Apps List Header
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

            // Loading state
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
                // Grouped Apps Container inside OneUICard
                item(key = "apps_container") {
                    OneUICard {
                        apps.forEachIndexed { index, app ->
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
        // 44.dp Squircle App Icon
        AppSquircleIcon(icon = app.icon, appName = app.appName)

        Spacer(modifier = Modifier.width(14.dp))

        // App Details
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

        // Per-app Notification Masking Toggle Icon (visible if locked and per-app mode is enabled)
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

        // One UI Pill Switch
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
    appName: String
) {
    val bitmap = remember(icon) {
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
