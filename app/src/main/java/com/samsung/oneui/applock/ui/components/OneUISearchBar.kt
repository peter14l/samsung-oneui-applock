package com.samsung.oneui.applock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.samsung.oneui.applock.ui.theme.OneUIDarkSurface
import com.samsung.oneui.applock.ui.theme.OneUILightSurface
import com.samsung.oneui.applock.ui.theme.OneUIPillRadius
import com.samsung.oneui.applock.ui.theme.OneUITheme
import com.samsung.oneui.applock.ui.theme.OneUITypography

@Composable
fun OneUISearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search apps"
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) OneUIDarkSurface else OneUILightSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = bgColor,
                shape = RoundedCornerShape(OneUIPillRadius)
            )
            .border(
                width = 1.dp,
                color = OneUITheme.extraColors.cardBorder,
                shape = RoundedCornerShape(OneUIPillRadius)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = OneUITheme.extraColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = OneUITypography.bodyMedium,
                        color = OneUITheme.extraColors.subtitleColor
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = OneUITypography.bodyMedium.copy(
                        color = OneUITheme.extraColors.textPrimary
                    ),
                    cursorBrush = SolidColor(OneUITheme.extraColors.accentBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear search",
                    tint = OneUITheme.extraColors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}
