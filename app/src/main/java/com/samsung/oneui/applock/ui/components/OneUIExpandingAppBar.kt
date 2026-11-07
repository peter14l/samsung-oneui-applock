package com.samsung.oneui.applock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samsung.oneui.applock.ui.theme.OneUIDarkBackground
import com.samsung.oneui.applock.ui.theme.OneUILightBackground
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun OneUIExpandingHeader(
    title: String,
    subtitle: String? = null,
    scrollOffset: Float = 0f,
    maxHeightDp: Int = 210,
    minHeightDp: Int = 64
) {
    val collapseFraction = (scrollOffset / (maxHeightDp - minHeightDp).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                alpha = 1f - collapseFraction * 0.9f
                translationY = -scrollOffset * 0.4f
            }
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = title,
            style = OneUITypography.displayLarge,
            color = OneUITheme.extraColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = OneUITypography.bodyMedium,
                color = OneUITheme.extraColors.subtitleColor
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun OneUICollapsedTopBar(
    title: String,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) OneUIDarkBackground else OneUILightBackground

    AnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.98f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    style = OneUITypography.titleLarge,
                    color = OneUITheme.extraColors.textPrimary,
                    maxLines = 1
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = OneUITheme.extraColors.dividerColor.copy(alpha = 0.6f)
            )
        }
    }
}
