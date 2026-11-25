package com.samsung.oneui.applock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class OneUIExtraColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val dividerColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val subtitleColor: Color,
    val pillBackground: Color,
    val accentBlue: Color
)

val LocalOneUIColors = staticCompositionLocalOf {
    OneUIExtraColors(
        cardBackground = OneUILightSurface,
        cardBorder = OneUILightCardBorder,
        dividerColor = OneUILightDivider,
        textPrimary = OneUILightTextPrimary,
        textSecondary = OneUILightTextSecondary,
        subtitleColor = OneUILightSubtitle,
        pillBackground = OneUIGrayPill,
        accentBlue = OneUIBlue
    )
}

private val LightColorScheme = lightColorScheme(
    primary = OneUIBlue,
    onPrimary = Color.White,
    primaryContainer = OneUIBlue.copy(alpha = 0.12f),
    onPrimaryContainer = OneUIBlue,
    background = OneUILightBackground,
    onBackground = OneUILightTextPrimary,
    surface = OneUILightSurface,
    onSurface = OneUILightTextPrimary,
    surfaceVariant = OneUILightBackground,
    onSurfaceVariant = OneUILightTextSecondary,
    outline = OneUILightDivider,
    outlineVariant = OneUILightCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = OneUIBlue,
    onPrimary = Color.White,
    primaryContainer = OneUIBlue.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    background = OneUIDarkBackground, // True AMOLED black #010101
    onBackground = OneUIDarkTextPrimary,
    surface = OneUIDarkSurface, // #1C1C1E
    onSurface = OneUIDarkTextPrimary,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = OneUIDarkTextSecondary,
    outline = OneUIDarkDivider,
    outlineVariant = OneUIDarkCardBorder
)

@Composable
fun SamsungOneUIAppLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val accentColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorScheme.primary
    } else {
        OneUIBlue
    }

    val extraColors = if (darkTheme) {
        OneUIExtraColors(
            cardBackground = OneUIDarkSurface,
            cardBorder = OneUIDarkCardBorder,
            dividerColor = OneUIDarkDivider,
            textPrimary = OneUIDarkTextPrimary,
            textSecondary = OneUIDarkTextSecondary,
            subtitleColor = OneUIDarkSubtitle,
            pillBackground = OneUIGrayPillDark,
            accentBlue = accentColor
        )
    } else {
        OneUIExtraColors(
            cardBackground = OneUILightSurface,
            cardBorder = OneUILightCardBorder,
            dividerColor = OneUILightDivider,
            textPrimary = OneUILightTextPrimary,
            textSecondary = OneUILightTextSecondary,
            subtitleColor = OneUILightSubtitle,
            pillBackground = OneUIGrayPill,
            accentBlue = accentColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = colorScheme.background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOneUIColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = OneUIShapes,
            typography = OneUITypography,
            content = content
        )
    }
}

object OneUITheme {
    val extraColors: OneUIExtraColors
        @Composable
        get() = LocalOneUIColors.current
}
