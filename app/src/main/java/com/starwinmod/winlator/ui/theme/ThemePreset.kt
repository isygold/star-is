package com.starwinmod.winlator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val name: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val tertiary: Color = Color(0xFFFBBF24),
    val onSurface: Color = Color(0xFFE0E0E0),
    val onSurfaceVariant: Color = Color(0xFFAAAAAA),
    val onBackground: Color = Color(0xFFFFFFFF),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val divider: Color = Color(0xFF404040),
    val outline: Color = Color(0xFF525252),
    val error: Color = Color(0xFFCF6679),
) {
    fun toColorScheme(accentOverride: Color? = null): androidx.compose.material3.ColorScheme {
        val accent = accentOverride ?: primary
        return darkColorScheme(
            primary              = accent,
            onPrimary            = onPrimary,
            primaryContainer     = accent.copy(alpha = 0.25f),
            onPrimaryContainer   = onSurface,
            secondary            = accent,
            onSecondary          = onPrimary,
            secondaryContainer   = accent.copy(alpha = 0.30f),
            onSecondaryContainer = onSurface,
            tertiary             = tertiary,
            onTertiary           = onPrimary,
            tertiaryContainer    = tertiary.copy(alpha = 0.25f),
            onTertiaryContainer  = onSurface,
            background           = background,
            onBackground         = onBackground,
            surface              = surface,
            onSurface            = onSurface,
            surfaceVariant       = surfaceVariant,
            onSurfaceVariant     = onSurfaceVariant,
            surfaceTint          = accent,
            outline              = outline,
            inverseSurface       = Color(0xFF2E2E2E),
            inversePrimary       = accent.copy(alpha = 0.80f),
            error                = error,
        )
    }

    fun toLightColorScheme(accentOverride: Color? = null): androidx.compose.material3.ColorScheme {
        val accent = accentOverride ?: primary
        return lightColorScheme(
            primary              = accent,
            onPrimary            = Color(0xFFFFFFFF),
            primaryContainer     = accent.copy(alpha = 0.15f),
            onPrimaryContainer   = Color(0xFF1A1A1A),
            secondary            = accent,
            onSecondary          = Color(0xFFFFFFFF),
            secondaryContainer   = accent.copy(alpha = 0.20f),
            onSecondaryContainer = Color(0xFF1A1A1A),
            tertiary             = tertiary,
            onTertiary           = Color(0xFFFFFFFF),
            tertiaryContainer    = tertiary.copy(alpha = 0.20f),
            onTertiaryContainer  = Color(0xFF1A1A1A),
            background           = Color(0xFFF5F5F5),
            onBackground         = Color(0xFF1A1A1A),
            surface              = Color(0xFFFFFFFF),
            onSurface            = Color(0xFF1A1A1A),
            surfaceVariant       = Color(0xFFEAEAEA),
            onSurfaceVariant     = Color(0xFF555555),
            surfaceTint          = accent,
            outline              = Color(0xFFBDBDBD),
            inverseSurface       = Color(0xFF1A1A1A),
            inversePrimary       = accent.copy(alpha = 0.80f),
            error                = Color(0xFFB00020),
        )
    }
}

val themePresets: List<ThemePreset> = listOf(
    ThemePreset(
        name          = "Classic Dark",
        background    = Color(0xFF1A1A1A),
        surface       = Color(0xFF2A2A2A),
        surfaceVariant= Color(0xFF333333),
        primary       = Color(0xFF8B6BE0),
        tertiary      = Color(0xFFFBBF24),
        outline       = Color(0xFF555555),
    ),
    ThemePreset(
        name          = "AMOLED",
        background    = Color(0xFF000000),
        surface       = Color(0xFF0D0D0D),
        surfaceVariant= Color(0xFF181818),
        primary       = Color(0xFFBB86FC),
        tertiary      = Color(0xFF22D3EE),
        outline       = Color(0xFF404040),
    ),
    ThemePreset(
        name          = "Ocean",
        background    = Color(0xFF0D1B2A),
        surface       = Color(0xFF162435),
        surfaceVariant= Color(0xFF1E3045),
        primary       = Color(0xFF0EA5E9),
        tertiary      = Color(0xFF34D399),
        outline       = Color(0xFF3B5E7A),
    ),
    ThemePreset(
        name          = "Forest",
        background    = Color(0xFF0D1A12),
        surface       = Color(0xFF142010),
        surfaceVariant= Color(0xFF1C2E1A),
        primary       = Color(0xFF22C55E),
        tertiary      = Color(0xFFEAB308),
        outline       = Color(0xFF3A5A3A),
    ),
    ThemePreset(
        name          = "Sunset",
        background    = Color(0xFF1A0D0D),
        surface       = Color(0xFF251515),
        surfaceVariant= Color(0xFF301C1C),
        primary       = Color(0xFFF97316),
        tertiary      = Color(0xFFFBBF24),
        outline       = Color(0xFF5A3A3A),
    ),
    ThemePreset(
        name          = "Rose",
        background    = Color(0xFF1A0D14),
        surface       = Color(0xFF25151E),
        surfaceVariant= Color(0xFF301C28),
        primary       = Color(0xFFEC4899),
        tertiary      = Color(0xFFF43F5E),
        outline       = Color(0xFF5A3A4A),
    ),
    ThemePreset(
        name          = "Steel",
        background    = Color(0xFF131419),
        surface       = Color(0xFF1C1D25),
        surfaceVariant= Color(0xFF252630),
        primary       = Color(0xFF64748B),
        tertiary      = Color(0xFF94A3B8),
        outline       = Color(0xFF3F4659),
    ),
    ThemePreset(
        name          = "VEGAS",
        background    = Color(0xFF0D0015),
        surface       = Color(0xFF1A0A2E),
        surfaceVariant= Color(0xFF2A1540),
        primary       = Color(0xFFD946EF),
        tertiary      = Color(0xFFFBBF24),
        onSurface     = Color(0xFFE8E8E8),
        onSurfaceVariant = Color(0xFFAAAAAA),
        divider       = Color(0xFF3A1A5E),
        outline       = Color(0xFF5A2A7E),
    ),
    ThemePreset(
        name          = "Custom",
        background    = Color(0xFF121212),
        surface       = Color(0xFF1E1E1E),
        surfaceVariant= Color(0xFF2A2A2A),
        primary       = Color(0xFF8B6BE0),
        tertiary      = Color(0xFFFBBF24),
        outline       = Color(0xFF525252),
    ),
)

val VEGAS_PRESET_INDEX = 7
val CUSTOM_PRESET_INDEX = themePresets.size - 1
