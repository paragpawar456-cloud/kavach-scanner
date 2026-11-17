package com.kavach.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kavach.core.model.Severity
import com.kavach.core.model.Verdict

object KColors {
    val Bg = Color(0xFF070B14)
    val Surface = Color(0xFF0E1626)
    val SurfaceHi = Color(0xFF15213A)
    val Outline = Color(0xFF1F3354)
    val Cyan = Color(0xFF00E5FF)
    val Green = Color(0xFF00FF9C)
    val Amber = Color(0xFFFFB020)
    val Red = Color(0xFFFF3B5C)
    val Text = Color(0xFFE6F1FF)
    val TextDim = Color(0xFF8BA3C7)

    fun verdict(v: Verdict?): Color = when (v) {
        Verdict.SAFE -> Green
        Verdict.SUSPICIOUS -> Amber
        Verdict.DANGEROUS -> Red
        null -> Cyan
    }

    fun severity(s: Severity): Color = when (s) {
        Severity.CRITICAL -> Red
        Severity.HIGH -> Color(0xFFFF6B3D)
        Severity.MEDIUM -> Amber
        Severity.LOW -> Color(0xFF6FA8FF)
        Severity.INFO -> TextDim
    }
}

private val scheme = darkColorScheme(
    primary = KColors.Cyan,
    onPrimary = Color(0xFF00222A),
    secondary = KColors.Green,
    onSecondary = Color(0xFF00210F),
    tertiary = KColors.Amber,
    error = KColors.Red,
    background = KColors.Bg,
    onBackground = KColors.Text,
    surface = KColors.Surface,
    onSurface = KColors.Text,
    surfaceVariant = KColors.SurfaceHi,
    onSurfaceVariant = KColors.TextDim,
    outline = KColors.Outline,
    surfaceContainer = KColors.Surface,
    surfaceContainerHigh = KColors.SurfaceHi,
)

private val typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun KavachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
