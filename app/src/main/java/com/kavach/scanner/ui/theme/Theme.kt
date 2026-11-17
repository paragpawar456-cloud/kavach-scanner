package com.kavach.scanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kavach.core.model.Severity
import com.kavach.core.model.Verdict

/** Kavach colour palette: saffron/orange on clean white, with a matching dark mode. */
@Immutable
data class KPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val outline: Color,
    val text: Color,
    val textDim: Color,
    val orange: Color,
    val orangeDeep: Color,
    val orangeSoft: Color,
    val navy: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
    val blue: Color,
    val purple: Color,
) {
    fun verdict(v: Verdict?): Color = when (v) {
        Verdict.SAFE -> green
        Verdict.SUSPICIOUS -> amber
        Verdict.DANGEROUS -> red
        null -> orange
    }

    fun severity(s: Severity): Color = when (s) {
        Severity.CRITICAL -> red
        Severity.HIGH -> Color(0xFFEA580C)
        Severity.MEDIUM -> amber
        Severity.LOW -> blue
        Severity.INFO -> textDim
    }
}

val LightPalette = KPalette(
    isDark = false,
    bg = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFFFF4EE),
    outline = Color(0xFFE6E8EC),
    text = Color(0xFF1B1F24),
    textDim = Color(0xFF6B7280),
    orange = Color(0xFFFF5722),
    orangeDeep = Color(0xFFE64A19),
    orangeSoft = Color(0xFFFFE6DC),
    navy = Color(0xFF14213D),
    green = Color(0xFF16A34A),
    amber = Color(0xFFF59E0B),
    red = Color(0xFFDC2626),
    blue = Color(0xFF2563EB),
    purple = Color(0xFF7C3AED),
)

val DarkPalette = KPalette(
    isDark = true,
    bg = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceAlt = Color(0xFF231A16),
    outline = Color(0xFF2A313C),
    text = Color(0xFFF0F3F6),
    textDim = Color(0xFF9BA4B0),
    orange = Color(0xFFFF6E3A),
    orangeDeep = Color(0xFFFF5722),
    orangeSoft = Color(0xFF3A2219),
    navy = Color(0xFF0B1324),
    green = Color(0xFF22C55E),
    amber = Color(0xFFFBBF24),
    red = Color(0xFFF87171),
    blue = Color(0xFF60A5FA),
    purple = Color(0xFFA78BFA),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Shortcut: `K.orange`, `K.text` ... inside composables. */
val K: KPalette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun KavachTheme(mode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.orange, onPrimary = Color.White, secondary = p.navy, background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.textDim,
            outline = p.outline, error = p.red, surfaceContainer = p.surface, surfaceContainerHigh = p.surface,
        )
    } else {
        lightColorScheme(
            primary = p.orange, onPrimary = Color.White, secondary = p.navy, background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.textDim,
            outline = p.outline, error = p.red, surfaceContainer = p.surface, surfaceContainerHigh = p.surface,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
