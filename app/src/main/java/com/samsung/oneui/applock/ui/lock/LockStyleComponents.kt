package com.samsung.oneui.applock.ui.lock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.oneui.applock.data.model.LockDesignStyle

internal data class LockStylePalette(
    val background: Color,
    val sheetBackground: Color,
    val sheetBorder: Color?,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val keyBg: Color,
    val keyBorder: Color?,
    val dotInactive: Color
)

internal fun resolveLockThemeFlags(style: LockDesignStyle, isSystemDark: Boolean): Pair<Boolean, Boolean> {
    val isDark = when (style) {
        LockDesignStyle.VAULT_DARK -> true
        LockDesignStyle.SWISS_MONO -> isSystemDark
        LockDesignStyle.DYNAMIC_THUMB -> isSystemDark
    }
    val useDynamic = style == LockDesignStyle.DYNAMIC_THUMB
    return isDark to useDynamic
}

@Composable
internal fun rememberLockStylePalette(style: LockDesignStyle, isDark: Boolean): LockStylePalette {
    val m3 = MaterialTheme.colorScheme
    return when (style) {
        LockDesignStyle.DYNAMIC_THUMB -> LockStylePalette(
            background = m3.background,
            sheetBackground = m3.surfaceContainerLow,
            sheetBorder = null,
            textPrimary = m3.onBackground,
            textSecondary = m3.onSurfaceVariant,
            accent = m3.primary,
            keyBg = m3.surfaceContainerHigh,
            keyBorder = null,
            dotInactive = m3.outlineVariant
        )
        LockDesignStyle.VAULT_DARK -> LockStylePalette(
            background = Color(0xFF0B0D12),
            sheetBackground = Color(0xFF131722),
            sheetBorder = Color(0xFF232A3B),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFF94A3B8),
            accent = Color(0xFF5E6AD2),
            keyBg = Color(0xFF1B2030),
            keyBorder = Color(0xFF2A3247),
            dotInactive = Color(0xFF2B3245)
        )
        LockDesignStyle.SWISS_MONO -> {
            val bg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
            val fg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
            val muted = if (isDark) Color(0xFFA0A0A0) else Color(0xFF5C5C5C)
            val border = if (isDark) Color(0xFF2E2E2E) else Color(0xFFD4D4D4)
            LockStylePalette(
                background = bg,
                sheetBackground = bg,
                sheetBorder = border,
                textPrimary = fg,
                textSecondary = muted,
                accent = fg,
                keyBg = Color.Transparent,
                keyBorder = border,
                dotInactive = border
            )
        }
    }
}

@Composable
internal fun SecurityStatusPill(
    text: String,
    isError: Boolean,
    palette: LockStylePalette
) {
    val errorRed = Color(0xFFFF3B30)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isError) errorRed.copy(alpha = 0.14f) else palette.accent.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) errorRed.copy(alpha = 0.4f) else palette.accent.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (isError) errorRed else palette.accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isError) errorRed else palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
