package com.samsung.oneui.applock.ui.main

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.samsung.oneui.applock.BuildConfig
import com.samsung.oneui.applock.ui.theme.OneUIBlue
import com.samsung.oneui.applock.ui.theme.OneUICardRadius
import com.samsung.oneui.applock.ui.theme.OneUIGreen
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUIRed
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography
import com.samsung.oneui.applock.update.UpdateInfo
import com.samsung.oneui.applock.update.UpdateState
import java.util.Locale

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onStartDownload: (UpdateInfo) -> Unit,
    onGrantInstallPermission: () -> Unit,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    val canDismiss = updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing

    Dialog(onDismissRequest = { if (canDismiss) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(OneUICardRadius),
            color = OneUITheme.extraColors.cardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (iconVector, iconTint) = when (updateState) {
                    is UpdateState.UpToDate -> Icons.Rounded.CheckCircle to OneUIGreen
                    is UpdateState.Error -> Icons.Rounded.ErrorOutline to OneUIRed
                    is UpdateState.Downloading, is UpdateState.Installing -> Icons.Rounded.CloudDownload to OneUIBlue
                    else -> Icons.Rounded.SystemUpdate to OneUIBlue
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (updateState) {
                    is UpdateState.Checking -> {
                        Text(
                            text = "Checking for Updates",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current version: v${BuildConfig.VERSION_NAME}",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OneUIBlue,
                            trackColor = OneUITheme.extraColors.pillBackground
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = OneUITheme.extraColors.textSecondary)
                        }
                    }

                    is UpdateState.UpToDate -> {
                        Text(
                            text = "You're Up to Date",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "App Lock v${updateState.currentVersion} is the latest release.",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(OneUIPillRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is UpdateState.Available -> {
                        val info = updateState.info
                        Text(
                            text = "Update Available",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VersionChip(text = "v${BuildConfig.VERSION_NAME} -> v${info.versionName}")
                            VersionChip(text = info.abi)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = info.releaseNotes,
                            style = OneUITypography.bodyMedium,
                            color = OneUITheme.extraColors.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Later", color = OneUITheme.extraColors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onStartDownload(info) },
                                shape = RoundedCornerShape(OneUIPillRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                            ) {
                                Text("Download & Install", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateState.PermissionRequired -> {
                        Text(
                            text = "Install Permission Needed",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Allow App Lock to install updates directly without leaving the app.",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel", color = OneUITheme.extraColors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onGrantInstallPermission,
                                shape = RoundedCornerShape(OneUIPillRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                            ) {
                                Text("Allow", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = (updateState.progressPercent / 100f).coerceIn(0f, 1f),
                            label = "download_progress"
                        )
                        Text(
                            text = "Downloading v${updateState.info.versionName}",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Architecture: ${updateState.info.abi}",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = OneUIBlue,
                            trackColor = OneUITheme.extraColors.pillBackground,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatBytesProgress(updateState.downloadedBytes, updateState.totalBytes),
                                style = OneUITypography.bodySmall,
                                color = OneUITheme.extraColors.textSecondary
                            )
                            Text(
                                text = "${updateState.progressPercent}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUIBlue
                            )
                        }
                    }

                    is UpdateState.Installing -> {
                        Text(
                            text = "Installing Update...",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Preparing v${updateState.info.versionName} (${updateState.info.abi})",
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = OneUIBlue,
                            trackColor = OneUITheme.extraColors.pillBackground
                        )
                    }

                    is UpdateState.Error -> {
                        Text(
                            text = "Update Failed",
                            style = OneUITypography.titleMedium,
                            color = OneUITheme.extraColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateState.message,
                            style = OneUITypography.bodySmall,
                            color = OneUITheme.extraColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Close", color = OneUITheme.extraColors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRetryCheck,
                                shape = RoundedCornerShape(OneUIPillRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                            ) {
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun VersionChip(text: String) {
    Box(
        modifier = Modifier
            .background(OneUIBlue.copy(alpha = 0.12f), RoundedCornerShape(OneUIPillRadius))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = OneUIBlue
        )
    }
}

private fun formatBytesProgress(downloaded: Long, total: Long): String {
    val dlMb = downloaded / (1024f * 1024f)
    return if (total > 0) {
        val totalMb = total / (1024f * 1024f)
        String.format(Locale.US, "%.1f MB / %.1f MB", dlMb, totalMb)
    } else {
        String.format(Locale.US, "%.1f MB", dlMb)
    }
}
