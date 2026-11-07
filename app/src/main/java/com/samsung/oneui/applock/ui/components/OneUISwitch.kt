package com.samsung.oneui.applock.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.samsung.oneui.applock.ui.theme.OneUIBlue

@Composable
fun OneUISwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = OneUIBlue,
            uncheckedThumbColor = if (isDark) Color(0xFFC7C7CC) else Color.White,
            uncheckedTrackColor = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = OneUIBlue.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = if (isDark) Color(0xFF28282A) else Color(0xFFEFEFF4)
        )
    )
}
