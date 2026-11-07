package com.samsung.oneui.applock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsung.oneui.applock.ui.theme.OneUICardRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme

@Composable
fun OneUICard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneUICardRadius),
        color = OneUITheme.extraColors.cardBackground,
        border = BorderStroke(1.dp, OneUITheme.extraColors.cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun OneUIDivider(
    modifier: Modifier = Modifier,
    insetStart: Boolean = true
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (insetStart) 68.dp else 16.dp, end = 16.dp),
        thickness = 1.dp,
        color = OneUITheme.extraColors.dividerColor
    )
}
