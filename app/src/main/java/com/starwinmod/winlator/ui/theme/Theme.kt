package com.starwinmod.winlator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DefaultColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    background       = Background,
    onBackground     = OnBackground,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error            = Error,
)

// ── Typography ──
// Slightly bumped label sizes for readability; titleSmall bold for section headers.
val AppTypography = Typography(
    labelSmall  = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    titleSmall  = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodySmall   = TextStyle(fontSize = 12.sp),
    bodyMedium  = TextStyle(fontSize = 14.sp),
)

// ── Shapes ──
// Slightly more rounded than Material3 defaults for a softer gaming UI.
val AppShapes = Shapes(
    small  = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large  = RoundedCornerShape(20.dp),
)

@Composable
fun WinlatorTheme(content: @Composable () -> Unit) {
    val colorScheme by AppThemeState.colorScheme.collectAsState(initial = AppThemeState.currentColorSchemeSnapshot())
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}
