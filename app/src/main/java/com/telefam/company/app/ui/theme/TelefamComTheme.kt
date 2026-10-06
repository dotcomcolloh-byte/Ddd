package com.telefam.company.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TelefamCrimson = Color(0xFFC80815)
val TelefamCrimsonDark = Color(0xFF9E040F)
val TelefamCrimsonVibrant = Color(0xFFDC1422)
val TelefamCrimsonSoft = Color(0xFFFFEAEC)
val TelefamCrimsonSoftDark = Color(0xFF3A1215)

val LightSurfaceBackground = Color(0xFFFBF7F7)
val LightCardSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1A1C1E)
val LightTextSecondary = Color(0xFF6B7280)
val LightBorderStroke = Color(0xFFE5E7EB)

val DarkSurfaceBackground = Color(0xFF0F1115)
val DarkCardSurface = Color(0xFF181B20)
val DarkTextPrimary = Color(0xFFF3F4F6)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkBorderStroke = Color(0xFF2D323B)

enum class TelefamThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("White Mode"),
    DARK("Dark Mode")
}

data class TelefamBrandColors(
    val brandRed: Color,
    val brandRedDark: Color,
    val brandRedTint: Color,
    val cardBackground: Color,
    val pageBackground: Color,
    val inputBorder: Color,
    val inputBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalTelefamColors = staticCompositionLocalOf {
    TelefamBrandColors(
        brandRed = TelefamCrimson,
        brandRedDark = TelefamCrimsonDark,
        brandRedTint = TelefamCrimsonSoft,
        cardBackground = LightCardSurface,
        pageBackground = LightSurfaceBackground,
        inputBorder = LightBorderStroke,
        inputBackground = Color(0xFFFFFFFF),
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        isDark = false
    )
}

private val TelefamLightColorScheme = lightColorScheme(
    primary = TelefamCrimson,
    onPrimary = Color.White,
    primaryContainer = TelefamCrimsonSoft,
    onPrimaryContainer = TelefamCrimsonDark,
    secondary = TelefamCrimsonDark,
    onSecondary = Color.White,
    background = LightSurfaceBackground,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderStroke,
    error = Color(0xFFBA1A1A)
)

private val TelefamDarkColorScheme = darkColorScheme(
    primary = TelefamCrimsonVibrant,
    onPrimary = Color.White,
    primaryContainer = TelefamCrimsonSoftDark,
    onPrimaryContainer = Color(0xFFFFB3B4),
    secondary = Color(0xFFFF898D),
    onSecondary = Color(0xFF5F0007),
    background = DarkSurfaceBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderStroke,
    error = Color(0xFFFFB4AB)
)

val TelefamTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun TelefamComTheme(
    themeMode: TelefamThemeMode = TelefamThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        TelefamThemeMode.SYSTEM -> systemDark
        TelefamThemeMode.LIGHT -> false
        TelefamThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val baseScheme: ColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamic.copy(
                primary = if (darkTheme) TelefamCrimsonVibrant else TelefamCrimson,
                onPrimary = Color.White,
                primaryContainer = if (darkTheme) TelefamCrimsonSoftDark else TelefamCrimsonSoft
            )
        }
        darkTheme -> TelefamDarkColorScheme
        else -> TelefamLightColorScheme
    }

    val brandColors = if (darkTheme) {
        TelefamBrandColors(
            brandRed = TelefamCrimsonVibrant,
            brandRedDark = TelefamCrimson,
            brandRedTint = TelefamCrimsonSoftDark,
            cardBackground = DarkCardSurface,
            pageBackground = DarkSurfaceBackground,
            inputBorder = DarkBorderStroke,
            inputBackground = Color(0xFF1F232A),
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            isDark = true
        )
    } else {
        TelefamBrandColors(
            brandRed = TelefamCrimson,
            brandRedDark = TelefamCrimsonDark,
            brandRedTint = TelefamCrimsonSoft,
            cardBackground = LightCardSurface,
            pageBackground = LightSurfaceBackground,
            inputBorder = LightBorderStroke,
            inputBackground = Color(0xFFFFFFFF),
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalTelefamColors provides brandColors) {
        MaterialTheme(
            colorScheme = baseScheme,
            typography = TelefamTypography,
            content = content
        )
    }
}
