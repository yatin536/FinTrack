package com.example.fintrack.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.fintrack.data.local.GlassThemePreset

val LocalGlassThemePreset = compositionLocalOf { GlassThemePreset.MIDNIGHT_GLASS }
val LocalCurrencySymbol = compositionLocalOf { "₹" }

private fun getPresetColorScheme(
    preset: GlassThemePreset,
    darkTheme: Boolean
): ColorScheme {
    return if (darkTheme) {
        when (preset) {
            GlassThemePreset.MIDNIGHT_GLASS -> darkColorScheme(
                primary = MidnightAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF1E2638),
                onPrimaryContainer = MidnightGlowCyan,
                secondary = EmeraldGreen,
                onSecondary = Color.White,
                tertiary = NeonCoral,
                background = MidnightBase,
                onBackground = DarkTextPrimary,
                surface = MidnightSecondary,
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF1F2937),
                onSurfaceVariant = DarkTextSecondary,
                outline = DarkOutline
            )
            GlassThemePreset.AURO_GLASS -> darkColorScheme(
                primary = AuroAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF2E1065),
                onPrimaryContainer = AuroGlowLavender,
                secondary = AuroGlowCyan,
                onSecondary = Color.White,
                tertiary = NeonCoral,
                background = AuroBase,
                onBackground = DarkTextPrimary,
                surface = AuroSecondary,
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF281E4E),
                onSurfaceVariant = DarkTextSecondary,
                outline = Color(0xFF3B2E68)
            )
            GlassThemePreset.SUNSET_GLOW -> darkColorScheme(
                primary = SunsetAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF381428),
                onPrimaryContainer = SunsetGlowCoral,
                secondary = SunsetGlowAmber,
                onSecondary = Color.White,
                tertiary = EmeraldGreen,
                background = SunsetBase,
                onBackground = DarkTextPrimary,
                surface = SunsetSecondary,
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF2A1535),
                onSurfaceVariant = DarkTextSecondary,
                outline = Color(0xFF42224A)
            )
            GlassThemePreset.EMERALD_CALM -> darkColorScheme(
                primary = EmeraldAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF064E3B),
                onPrimaryContainer = EmeraldGlowMint,
                secondary = CyberCyan,
                onSecondary = Color.White,
                tertiary = AmberGold,
                background = EmeraldBase,
                onBackground = DarkTextPrimary,
                surface = EmeraldSecondary,
                onSurface = DarkTextPrimary,
                surfaceVariant = Color(0xFF0F382E),
                onSurfaceVariant = DarkTextSecondary,
                outline = Color(0xFF165042)
            )
        }
    } else {
        when (preset) {
            GlassThemePreset.MIDNIGHT_GLASS -> lightColorScheme(
                primary = RoyalBlue,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDBEAFE),
                onPrimaryContainer = RoyalBlue,
                secondary = EmeraldGreen,
                onSecondary = Color.White,
                tertiary = CrimsonRed,
                background = LightBackground,
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = LightSurfaceVariant,
                onSurfaceVariant = LightTextSecondary,
                outline = LightOutline
            )
            GlassThemePreset.AURO_GLASS -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF6D28D9),
                secondary = CyberCyan,
                onSecondary = Color.White,
                tertiary = NeonCoral,
                background = Color(0xFFFAF5FF),
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = Color(0xFFF3E8FF),
                onSurfaceVariant = LightTextSecondary,
                outline = Color(0xFFDDD6FE)
            )
            GlassThemePreset.SUNSET_GLOW -> lightColorScheme(
                primary = Color(0xFFE11D48),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE4E6),
                onPrimaryContainer = Color(0xFFBE123C),
                secondary = AmberGold,
                onSecondary = Color.White,
                tertiary = EmeraldGreen,
                background = Color(0xFFFFF1F2),
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = Color(0xFFFFE4E6),
                onSurfaceVariant = LightTextSecondary,
                outline = Color(0xFFFECDD3)
            )
            GlassThemePreset.EMERALD_CALM -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF047857),
                secondary = RoyalBlue,
                onSecondary = Color.White,
                tertiary = CrimsonRed,
                background = Color(0xFFF0FDF4),
                onBackground = LightTextPrimary,
                surface = LightSurface,
                onSurface = LightTextPrimary,
                surfaceVariant = Color(0xFFDCFCE7),
                onSurfaceVariant = LightTextSecondary,
                outline = Color(0xFFA7F3D0)
            )
        }
    }
}

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassPreset: GlassThemePreset = GlassThemePreset.MIDNIGHT_GLASS,
    currencySymbol: String = "₹",
    content: @Composable () -> Unit,
) {
    val colorScheme = getPresetColorScheme(glassPreset, darkTheme)

    CompositionLocalProvider(
        LocalGlassThemePreset provides glassPreset,
        LocalCurrencySymbol provides currencySymbol
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
