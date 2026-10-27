package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DayformColorScheme = darkColorScheme(
    primary = NeonLime,
    onPrimary = PrimaryDarkText,
    primaryContainer = CharcoalElevated,
    onPrimaryContainer = NeonLime,
    secondary = LavenderPurple,
    onSecondary = PrimaryDarkText,
    secondaryContainer = Color(0xFF332042),
    onSecondaryContainer = LavenderPurple,
    tertiary = DeadlineOrange,
    onTertiary = PrimaryDarkText,
    background = CharcoalBg,
    onBackground = LightText,
    surface = CharcoalSurface,
    onSurface = LightText,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = SecondaryText,
    outline = CharcoalBorder,
    error = OverdueRed,
    onError = Color.White
)

val DayformShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun DayformTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DayformColorScheme,
        typography = Typography,
        shapes = DayformShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    DayformTheme(content = content)
}
