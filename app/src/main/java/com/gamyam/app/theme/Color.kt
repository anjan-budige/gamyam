package com.gamyam.app.theme

import androidx.compose.ui.graphics.Color

// ── Legacy Material defaults ──────────────────────────────────────────────────
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ── Splash screen gradient ────────────────────────────────────────────────────
val GamyamGreenLight = Color(0xFF43A047)   // top of splash gradient
val GamyamGreenDark  = Color(0xFF1B5E20)   // bottom of splash gradient

// ── Brand / fixed colours (same in both themes) ───────────────────────────────
/** Deep blue — AppBar background, station-code pills, tab-selected text. */
val AppBarBlue      = Color(0xFF1448A6)
/** Dark green — primary action buttons ("Find buses", search icons). */
val ActionGreen     = Color(0xFF2E7D32)
/** Station-code pill background. */
val StationCodeBlue = Color(0xFF1448A6)

// ── Light theme surface colours ───────────────────────────────────────────────
val PageBackground  = Color(0xFFF0F2F5)   // scaffold background
val BottomNavLight  = Color(0xFFD0E4F8)   // bottom bar surface (light)

// ── Dark theme palette ────────────────────────────────────────────────────────
val DarkBackground  = Color(0xFF1A1E24)
val DarkSurface     = Color(0xFF252A35)
val DarkSurfaceVar  = Color(0xFF1E2530)   // bottom bar surface (dark) / surfaceVariant
val DarkOnSurface   = Color(0xFFE8EEF8)   // primary text in dark mode
val DarkOnSurfaceVar= Color(0xFF9BACC4)   // hint / secondary text in dark mode
val DarkOutline     = Color(0xFF2E3545)   // divider / border in dark mode
val DarkOutlineVar  = Color(0xFF252D3C)
val DarkSecondary   = Color(0xFF90B8F0)   // bottom-nav icon tint in dark (light blue)
