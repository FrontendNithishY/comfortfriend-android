package com.comfortfriend.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comfortfriend.app.data.prefs.ThemeChoice

val Navy = Color(0xFF0B1026)
val NavySurface = Color(0xFF151B3B)
val DeepPurple = Color(0xFF3B2A7A)
val Violet = Color(0xFF6C4FD6)
val Lavender = Color(0xFFC9B8FF)
val Mist = Color(0xFFF4F0FF)
val InkOnLight = Color(0xFF1B1536)

private val Dark = darkColorScheme(
    primary = Lavender, onPrimary = Navy,
    primaryContainer = DeepPurple, onPrimaryContainer = Mist,
    secondary = Color(0xFF9FB4FF), onSecondary = Navy,
    secondaryContainer = Color(0xFF232B5C), onSecondaryContainer = Mist,
    background = Navy, onBackground = Mist,
    surface = NavySurface, onSurface = Mist,
    surfaceVariant = Color(0xFF1E2550), onSurfaceVariant = Color(0xFFC3BEDD),
    outline = Color(0xFF6F6A99), error = Color(0xFFFFB4AB),
)

private val Light = lightColorScheme(
    primary = Violet, onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF), onPrimaryContainer = InkOnLight,
    secondary = Color(0xFF4A5BB8), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE3FF), onSecondaryContainer = InkOnLight,
    background = Mist, onBackground = InkOnLight,
    surface = Color.White, onSurface = InkOnLight,
    surfaceVariant = Color(0xFFEDE8FB), onSurfaceVariant = Color(0xFF4B4666),
    outline = Color(0xFF857FA6), error = Color(0xFFBA1A1A),
)

private val Serif = FontFamily.Serif
private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun ComfortFriendTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Soft gradient backdrop used behind every screen. */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = LocalDarkTheme.current
    val brush = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF1A1244), Navy, Color(0xFF080C1C)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE9E1FF), Mist, Color(0xFFFFFFFF)))
    }
    Box(modifier.fillMaxSize().background(brush)) { content() }
}
