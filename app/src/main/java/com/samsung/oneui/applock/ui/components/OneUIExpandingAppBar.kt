package com.samsung.oneui.applock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    // Fraction: 0.0 (fully expanded) to 1.0 (fully collapsed)
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
    AnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .background(OneUITheme.extraColors.cardBackground.copy(alpha = 0.95f))
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
    }
}
