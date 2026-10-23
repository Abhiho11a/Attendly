package com.example.attendly.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Indigo & Slate Palette (Optimized for WCAG 2.2 AA Contrast)
val PrimaryIndigo = Color(0xFF4F46E5) // Indigo 600 (WCAG AA compliant contrast > 5:1)
val SecondaryIndigo = Color(0xFF6366F1)
val AccentTeal = Color(0xFF0D9488) // Teal 700 for accessible contrast

// Light Theme
val SoftBackground = Color(0xFFF8FAFC)
val SoftSurface = Color(0xFFFFFFFF)
val SoftOutline = Color(0xFF64748B) // Slate 500 for non-text contrast >= 3:1

// Dark Theme
val DeepBackground = Color(0xFF0F172A)
val DeepSurface = Color(0xFF1E293B)
val DeepOutline = Color(0xFF475569)

// Sophisticated Status Colors (WCAG AA Compliant)
val StatusAttended = Color(0xFF059669) // Emerald 600
val StatusAbsent = Color(0xFFDC2626) // Red 600
val StatusCancelled = Color(0xFF475569) // Slate 600
val StatusPending = Color(0xFFD97706) // Amber 600

// M3 Palette Mapping - Light
val md_theme_light_primary = PrimaryIndigo
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFE0E7FF)
val md_theme_light_onPrimaryContainer = Color(0xFF312E81)
val md_theme_light_secondary = AccentTeal
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCCFBF1)
val md_theme_light_onSecondaryContainer = Color(0xFF134E4A)
val md_theme_light_tertiary = Color(0xFF6D28D9) // Purple 700 for contrast
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFEDE9FE)
val md_theme_light_onTertiaryContainer = Color(0xFF4338CA)
val md_theme_light_error = Color(0xFFB91C1C) // Red 700
val md_theme_light_errorContainer = Color(0xFFFEE2E2)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_outline = SoftOutline
val md_theme_light_background = SoftBackground
val md_theme_light_onBackground = Color(0xFF0F172A) // Slate 900 for high contrast
val md_theme_light_surface = SoftSurface
val md_theme_light_onSurface = Color(0xFF0F172A) // Slate 900 for high contrast
val md_theme_light_surfaceVariant = Color(0xFFF1F5F9)
val md_theme_light_onSurfaceVariant = Color(0xFF334155) // Slate 700 (contrast > 4.5:1)
val md_theme_light_inverseOnSurface = Color(0xFFF8FAFC)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inversePrimary = Color(0xFF818CF8)
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = PrimaryIndigo
val md_theme_light_outlineVariant = Color(0xFF64748B) // Slate 500 for non-text contrast >= 3:1
val md_theme_light_scrim = Color(0xFF000000)

// M3 Palette Mapping - Dark
val md_theme_dark_primary = Color(0xFF818CF8) // Indigo 400
val md_theme_dark_onPrimary = Color(0xFF0F172A)
val md_theme_dark_primaryContainer = Color(0xFF312E81)
val md_theme_dark_onPrimaryContainer = Color(0xFFE0E7FF)
val md_theme_dark_secondary = Color(0xFF5EEAD4) // Teal 300
val md_theme_dark_onSecondary = Color(0xFF042F2E)
val md_theme_dark_secondaryContainer = Color(0xFF134E4A)
val md_theme_dark_onSecondaryContainer = Color(0xFFCCFBF1)
val md_theme_dark_tertiary = Color(0xFFC4B5FD)
val md_theme_dark_onTertiary = Color(0xFF2E1065)
val md_theme_dark_tertiaryContainer = Color(0xFF4C1D95)
val md_theme_dark_onTertiaryContainer = Color(0xFFEDE9FE)
val md_theme_dark_error = Color(0xFFF87171)
val md_theme_dark_errorContainer = Color(0xFF7F1D1D)
val md_theme_dark_onError = Color(0xFFFFFFFF)
val md_theme_dark_onErrorContainer = Color(0xFFFEE2E2)
val md_theme_dark_outline = DeepOutline
val md_theme_dark_background = DeepBackground
val md_theme_dark_onBackground = Color(0xFFF8FAFC)
val md_theme_dark_surface = DeepSurface
val md_theme_dark_onSurface = Color(0xFFF8FAFC)
val md_theme_dark_surfaceVariant = Color(0xFF334155)
val md_theme_dark_onSurfaceVariant = Color(0xFFCBD5E1)
val md_theme_dark_inverseOnSurface = Color(0xFF0F172A)
val md_theme_dark_inverseSurface = Color(0xFFF1F5F9)
val md_theme_dark_inversePrimary = PrimaryIndigo
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = SecondaryIndigo
val md_theme_dark_outlineVariant = Color(0xFF475569)
val md_theme_dark_scrim = Color(0xFF000000)
