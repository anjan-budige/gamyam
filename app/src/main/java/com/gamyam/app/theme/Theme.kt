package com.gamyam.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Light scheme ──────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = ActionGreen,          // buttons, FAB
    secondary        = AppBarBlue,           // bottom-nav icons / tint
    tertiary         = Pink40,
    background       = PageBackground,       // scaffold background  #F0F2F5
    surface          = Color.White,          // card / sheet surface
    surfaceVariant   = BottomNavLight,       // bottom-nav bar surface  #D0E4F8
    onBackground     = Color(0xFF111111),    // text on background
    onSurface        = Color(0xFF111111),    // text on cards
    onSurfaceVariant = Color(0xFF555555),    // hint / secondary text
    outline          = Color(0xFFCCCCCC),    // borders
    outlineVariant   = Color(0xFFEEEEEE),    // dividers
    surfaceTint      = Color.Transparent
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = ActionGreen,
    secondary        = DarkSecondary,        // light-blue for bottom nav in dark
    tertiary         = Pink80,
    background       = DarkBackground,       // #1A1E24
    surface          = DarkSurface,          // #252A35 – card background
    surfaceVariant   = DarkSurfaceVar,       // #1E2530 – bottom nav surface
    onBackground     = DarkOnSurface,        // #E8EEF8 – primary text
    onSurface        = DarkOnSurface,        // #E8EEF8 – text on cards
    onSurfaceVariant = DarkOnSurfaceVar,     // #9BACC4 – hint / label text
    outline          = DarkOutline,          // #2E3545 – borders
    outlineVariant   = DarkOutlineVar,       // #252D3C – dividers
    surfaceTint      = Color.Transparent
)

@Composable
fun GamyamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
